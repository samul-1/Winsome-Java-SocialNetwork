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
import java.util.Iterator;

import auth.AuthenticationMiddleware;
import exceptions.BadRequestException;
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
    private final int BUF_CAPACITY = 4096 * 4096;

    private ApiRouter router;
    private final SocialNetworkService service;
    private final AuthenticationMiddleware authMiddleware;
    private ServerConfig config;
    private Selector selector;

    public Server(File config, File apiSchema) throws IOException {
        this.loadConfig(config);
        this.loadRouter(apiSchema);

        DataStoreService store = new DataStoreService(this.config.getStorageLocation());
        this.service = new SocialNetworkService(store);
        this.authMiddleware = new AuthenticationMiddleware(store);
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

        while (true) {
            try {
                System.out.println("ready to accept...");
                this.selector.select();
                System.out.println("accepted...");
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
                        System.out.println("A client connected");
                    } else if (currKey.isReadable()) {
                        // System.out.println("A client is readable");
                        this.readFromKey(currKey);
                    } else if (currKey.isWritable()) {
                        // System.out.println("A client is writable");
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
                    System.out.println("A client left");
                }
            }
        }
    }

    private void acceptKey(SelectionKey key) throws IOException {
        ServerSocketChannel channel = (ServerSocketChannel) key.channel();
        SocketChannel clientSkt = channel.accept();

        clientSkt.configureBlocking(false);
        SelectionKey clientKey = clientSkt.register(this.selector, SelectionKey.OP_READ);
        clientKey.attach(new ClientConnectionState());
    }

    private void writeToKey(SelectionKey key) throws IOException {
        SocketChannel clientSkt = (SocketChannel) key.channel();

        // get previously stored response to be written to client
        RestResponse response = ((ClientConnectionState) key.attachment()).pendingResponse;
        System.out.println("Response: " + response);
        ByteBuffer buf = ByteBuffer.wrap(response.toString().getBytes("UTF-8"));
        System.out.println("Written:\n" + response.toString());
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
            System.out.println("nothing to read");
            throw new IOException();
        }
        buf.flip();

        // System.out.println("Read:\n" +
        // StandardCharsets.UTF_8.decode(buf).toString());
        String reqString = StandardCharsets.UTF_8.decode(buf).toString();
        System.out.println("request: " + reqString);
        RestRequest request = RestRequest.parseRequestString(reqString);

        System.out.println("path: " + request.getPath() + "method: " + request.getMethod().name());

        RestResponse response;
        if (request.getMethod() == HttpMethod.OPTIONS) {
            response = new RestResponse(200);
        } else {
            response = handleRequest(request);
        }

        // remove OP_READ from the interest set and replace it with OP_WRITE
        // as there isn't anything to be read from the client, but we now
        // have a response to write to it
        key.interestOps(SelectionKey.OP_WRITE);
        // store the pending response so it can be written as soon
        // as the client becomes writable
        ((ClientConnectionState) key.attachment()).pendingResponse = response;
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

        // resolve route to get the handler method for this request
        try {
            handler = this.router.getRequestHandler(request);
            System.out.println("got handler" + handler.getName());

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
            System.out.println("invalid token");
            return new RestResponse(400);
        }
        // invoke handler to execute the request and get
        // response to write back to client
        try {
            System.out.println("About to invoke: " + handler.getName());
            response = (RestResponse) handler.invoke(this.service, authenticatedRequest);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            e.printStackTrace();
            return new RestResponse(500);
        } catch (InvocationTargetException e) {
            try {
                e.printStackTrace();
                // InvocationTargetException is thrown if the method called via
                // `invoke()` throws an exception; in order to get the original
                // exception, use exception chaining, so the correct HTTP error
                // can ultimately be written back to the client
                if (e.getCause() instanceof Exception) {
                    throw (Exception) e.getCause();
                }
            } catch (BadRequestException e1) {
                System.out.println("Raised bad request");
                return new RestResponse(400);
            } catch (PermissionDeniedException e1) {
                return new RestResponse(403);
            } catch (ResourceNotFoundException e1) {
                return new RestResponse(404);
            } catch (Exception e1) {
                assert false; // never reached
            }
        }
        return response;
    }
}
