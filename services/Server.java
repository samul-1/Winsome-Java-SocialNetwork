package services;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import auth.AuthenticationMiddleware;
import exceptions.BadRequestException;
import exceptions.InternalServerErrorException;
import exceptions.InvalidTokenException;
import exceptions.MethodNotSupportedException;
import exceptions.NoAuthenticationProvidedException;
import exceptions.PermissionDeniedException;
import exceptions.ResourceNotFoundException;
import exceptions.RouteNotFoundException;
import protocol.AuthenticatedRestRequest;
import protocol.HttpMethod;
import protocol.RestRequest;
import protocol.RestResponse;
import routing.ApiRoute;
import routing.ApiRouter;

public class Server {
    private final int BUF_CAPACITY = 4096 * 2;

    private ApiRouter router;
    private final SocialNetworkService service;
    UserRegistrationService registrationService;
    FollowerNotificationService notificationService;
    private final AuthenticationMiddleware authMiddleware;
    private ServerConfig config;
    private Selector selector;

    public Server(File config, File apiSchema) throws IOException {
        this.loadConfig(config);
        this.loadRouter(apiSchema);

        // restore previously existing data if a valid storage file is supplied;
        // otherwise initialize a new empty data store
        DataStoreService store = DataStoreService.restoreOrCreate(this.config.getStorageLocation());

        this.notificationService = new FollowerNotificationService(store);
        this.service = new SocialNetworkService(
                store,
                this.notificationService,
                new WalletConversionService(),
                this.config);
        this.authMiddleware = new AuthenticationMiddleware(store);
        this.registrationService = new UserRegistrationService(store);

        // start rewards service in a separate thread
        new Thread(new RewardIssuer(store, this.config.getTimeInBetweenRewards(),
                this.config.getAuthorRewardPercentage(), this.config)).start();
    }

    private void loadConfig(File config) throws IOException {
        this.config = new Serializer<ServerConfig>().parse(config, ServerConfig.class);
    }

    private void loadRouter(File apiSchema) throws IOException {
        this.router = new ApiRouter(new Serializer<ApiRoute[]>().parse(apiSchema, ApiRoute[].class));
    }

    private class ClientConnectionState {
        RestResponse pendingResponse;
    }

    public void start() {
        try {
            ServerSocketChannel srvSktChan = ServerSocketChannel.open();
            ServerSocket skt = srvSktChan.socket();
            this.selector = Selector.open();

            skt.bind(new InetSocketAddress(this.config.getServerAddr(), this.config.getTcpPort()));
            srvSktChan.configureBlocking(false);
            srvSktChan.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Server address: " + this.config.getServerAddr());
            System.out.println("Listening on port " + this.config.getTcpPort() + "...");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // export RMI object to expose user registration service
        UserRegistrationInterface userRegistrationStub;
        FollowerNotificationServiceInterface followerNotificationStub;
        Registry registry;

        try {
            // expose RMI services
            userRegistrationStub = (UserRegistrationInterface) UnicastRemoteObject
                    .exportObject(this.registrationService, this.config.getRegistryPort());
            followerNotificationStub = (FollowerNotificationServiceInterface) UnicastRemoteObject
                    .exportObject(this.notificationService, this.config.getRegistryPort());

            LocateRegistry.createRegistry(this.config.getRegistryPort());
            registry = LocateRegistry.getRegistry(this.config.getRegistryPort());

            registry.rebind("USER-REGISTRATION-SERVICE", userRegistrationStub);
            registry.rebind("FOLLOWER-NOTIFICATION-SERVICE", followerNotificationStub);
        } catch (RemoteException e) {
            e.printStackTrace();
            System.exit(1);
        }

        while (true) {
            try {
                this.selector.select();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }

            Iterator<SelectionKey> rdyKeys = selector.selectedKeys().iterator();
            while (rdyKeys.hasNext()) {
                SelectionKey currKey = rdyKeys.next();
                rdyKeys.remove();
                try {
                    if (currKey.isAcceptable()) {
                        this.acceptKey(currKey);
                    } else if (currKey.isReadable()) {
                        this.readFromKey(currKey);
                    } else if (currKey.isWritable()) {
                        this.writeToKey(currKey);
                    }
                } catch (IOException e) {
                    currKey.cancel();
                    try {
                        // a client closed connection: close its channel
                        currKey.channel().close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        System.exit(1);
                    }
                }
            }
        }
    }

    private void acceptKey(SelectionKey key) throws IOException {
        ServerSocketChannel channel = (ServerSocketChannel) key.channel();
        SocketChannel clientSkt = channel.accept();

        clientSkt.configureBlocking(false);
        SelectionKey clientKey = clientSkt.register(this.selector, SelectionKey.OP_READ);

        // will be used to keep track of not-yet-written responses for this client
        clientKey.attach(new ClientConnectionState());
    }

    private void writeToKey(SelectionKey key) throws IOException {
        SocketChannel clientSkt = (SocketChannel) key.channel();

        // get previously stored response to be written to client
        RestResponse response = ((ClientConnectionState) key.attachment()).pendingResponse;
        ByteBuffer buf = ByteBuffer.wrap(response.toString().getBytes("UTF-8"));
        clientSkt.write(buf);

        // remove OP_WRITE from the interest set and replace it with OP_READ
        // as there isn't anything to be written to the client, and we're now
        // ready to read again from it
        key.interestOps(SelectionKey.OP_READ);
    }

    private void readFromKey(SelectionKey key) throws IOException {
        SocketChannel clientSkt = (SocketChannel) key.channel();
        ByteBuffer buf = ByteBuffer.allocate(this.BUF_CAPACITY);
        int readCount = clientSkt.read(buf);
        if (readCount == -1) { // no data was read, client closed connection
            // the caller will catch this and remove the key from the readset
            throw new IOException();
        }
        buf.flip();

        String reqString = StandardCharsets.UTF_8.decode(buf).toString();
        RestRequest request;

        try {
            request = RestRequest.parseRequestString(reqString);

            CompletableFuture
                    // submit task to internally-managed thread pool
                    .supplyAsync(() -> handleRequest(request))
                    // run callback on task completion
                    .thenAccept((response) -> {
                        // store the pending response so it can be written as soon
                        // as the client becomes writable
                        ((ClientConnectionState) key.attachment()).pendingResponse = response;

                        // remove OP_READ from the interest set and replace it with OP_WRITE
                        // as there isn't anything to be read from the client, but we now
                        // have a response to write to it
                        key.interestOps(SelectionKey.OP_WRITE);

                        // wake up selector from async callback
                        this.selector.wakeup();
                    });
        } catch (IllegalArgumentException e) {
            // a malformed HTTP request was sent
            ((ClientConnectionState) key.attachment()).pendingResponse = new RestResponse(400);
            key.interestOps(SelectionKey.OP_WRITE);
        }
    }

    private RestResponse handleRequest(RestRequest request) {
        /**
         * Main method to handle an incoming API request.
         * 
         * The flow is as follows:
         * 1. the request path and method are used by the ApiRouter to get
         * the handler method for the request
         * 2. the request is authenticated by the AuthenticationMiddleware
         * 3. the handler is invoked on the resulting AuthenticatedRestRequest
         * and a RestResponse is returned
         * 
         */
        Method handler;
        RestResponse response = null;
        AuthenticatedRestRequest authenticatedRequest;
        boolean requireAuth = !request.isLoginRequest();

        if (request.getMethod() == HttpMethod.OPTIONS) {
            // ignore OPTION requests sent from browsers
            return new RestResponse(200);
        }

        // resolve route to get the handler method for this request
        try {
            handler = this.router.getRequestHandler(request);
        } catch (RouteNotFoundException e) {
            return new RestResponse(404);
        } catch (MethodNotSupportedException e) {
            return new RestResponse(405);
        }

        // check request headers to authenticate the request
        try {
            authenticatedRequest = requireAuth ? this.authMiddleware.authenticateRequest(request)
                    : this.authMiddleware.getAnonymousRestRequest(request);
        } catch (NoAuthenticationProvidedException e) {
            return new RestResponse(401);
        } catch (InvalidTokenException e) {
            return new RestResponse(400);
        }

        // invoke handler for this request and get response to write back to client
        try {
            response = (RestResponse) handler.invoke(this.service, authenticatedRequest);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            // something went wrong on the server side
            e.printStackTrace();
            return new RestResponse(500);
        } catch (InvocationTargetException e) {
            // there's an error with the client request
            try {
                // InvocationTargetException is thrown if the method called via
                // `invoke()` throws an exception; in order to get the original
                // exception, use exception chaining, so the correct HTTP error
                // can ultimately be written back to the client
                if (e.getCause() instanceof Exception) {
                    throw (Exception) e.getCause();
                }
            } catch (BadRequestException e1) {
                return new RestResponse(400);
            } catch (PermissionDeniedException e1) {
                return new RestResponse(403);
            } catch (ResourceNotFoundException e1) {
                return new RestResponse(404);
            } catch (InternalServerErrorException e1) {
                return new RestResponse(500);
            } catch (Exception e1) {
                assert false; // never reached
            }
        }
        return response;
    }
}
