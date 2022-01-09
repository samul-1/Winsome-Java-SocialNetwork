package client;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

import auth.AuthenticationToken;
import entities.Comment;
import entities.Post;
import entities.User;
import exceptions.ClientOperationFailedException;
import exceptions.InvalidClientArgumentsException;
import protocol.HttpMethod;
import protocol.RestRequest;
import protocol.RestResponse;
import services.FollowerNotificationServiceInterface;
import services.Serializer;
import services.ServerConfig;
import services.UserRegistrationInterface;
import services.Wallet;

public class Client implements IClient {
    private final int BUF_CAPACITY = 4096 * 4096;
    private final ServerConfig config;
    private UserRegistrationInterface registrationService = null;
    private FollowerNotificationServiceInterface notificationService = null;
    private final Set<User> localFollowers = new HashSet<>();
    private MulticastSocket multicastSkt = null;
    private Thread multicastThread = null;
    private ClientFollowerNotificationService clientNotificationService = null;

    private Map<String, String> requestHeaders = new HashMap<>();

    private SocketChannel sktChan;

    private final Map<String, String> clientMessages = initMsgsMap();
    private final Map<String, Map<Integer, String>> outcomeMessages = initOutcomesMap();

    private Map<String, String> initMsgsMap() {
        Map<String, String> map = new HashMap<>();
        map.put("client_ok", "Operation completed successfully.");
        map.put("login_ok", "Successfully logged in.");
        map.put("logout_ok", "Successfully logged out.");
        map.put("registration_ok", "Successfully registered. Here's your new account:");
        map.put("invalid_registration_data", "There was an error creating your account. You sent invalid data.");
        map.put("username_already_taken", "The username you chose is already taken.");
        map.put("unknown_registration_error", "An unknown error occurred during registration.");
        map.put("follow_ok", "Successfully followed user.");
        map.put("unfollow_ok", "Stopped following user.");
        map.put("delete_ok", "Successfully deleted post.");
        map.put("rewin_ok", "Successfully rewinned post.");
        map.put("rate_ok", "Vote added.");
        map.put("comment_ok", "Comment added.");
        map.put("new_post_ok", "Successfully created a new post. Here's your post:");
        map.put("wrong_username_password", "Wrong username or password.");
        map.put("server_error", "The server is temporarily unreachable.");
        map.put("cannot_log_out", "You can't log out using this username.");
        map.put("unknown_operation", "Unknown operation: ");
        map.put("invalid_operation_arguments", "Invalid arguments. ");
        map.put("welcome_message", "Connected to server!\nTo start, type" +
                "register <username> <password> <tags> (at least one tag is REQUIRED)" +
                " or, if you already have an account, login <username> <password>\n" +
                "You can type 'exit' at any time to exit the program.\n");
        map.put("btc_wallet", "Your wallet balance converted to BitCoins is: btc ");
        map.put("waiting_for_command", "Your command: ");
        map.put("post_constraint_failed",
                "Titles can only be 20 characters long and the content of the" +
                        "post can be up to 500 characters long.");
        map.put("already_logged_in", "You're already logged in.");
        map.put("not_logged_in", "You aren't logged in.");
        map.put("discarding_services", "Leaving multicast group and discarding RMI service...");
        map.put("unable_connect",
                "Unable to connect to server. Make sure the server is running before launching the client.");

        return Collections.unmodifiableMap(map);
    }

    private Map<String, Map<Integer, String>> initOutcomesMap() {
        Map<String, Map<Integer, String>> map = new HashMap<>();
        map.put("/login", new HashMap<>());
        map.get("/login").put(403, "Wrong username or password.");

        map.put("/logout", new HashMap<>());
        map.get("/logout").put(403, "You are not logged in under this username.");

        map.put("/users/following", new HashMap<>());
        map.get("/users/following").put(404, "Requested user doesn't exist.");

        map.put("/posts", new HashMap<>());
        map.get("/posts").put(400, "You sent invalid data for this post.");

        map.put("/posts/<id>", new HashMap<>());
        map.get("/posts/<id>").put(404, "Requested post doesn't exist.");
        map.get("/posts/<id>").put(403, "You are not the author of this post.");

        map.put("/posts/<id>/rewin", new HashMap<>());
        map.get("/posts/<id>/rewin").put(404, "Requested post doesn't exist.");
        map.get("/posts/<id>/rewin").put(403, "You are the author of this post");

        map.put("/posts/<id>/rate", new HashMap<>());
        map.get("/posts/<id>/rate").put(400, "You sent an invalid value.");
        map.get("/posts/<id>/rate").put(404, "Requested post doesn't exist.");
        map.get("/posts/<id>/rate").put(403, "You are the author of this post.");

        map.put("/posts/<id>/comments", new HashMap<>());
        map.get("/posts/<id>/comments").put(400, "You sent invalid data for the comment.");
        map.get("/posts/<id>/comments").put(404, "Requested post doesn't exist.");
        map.get("/posts/<id>/comments").put(403, "You are the author of this post.");

        return Collections.unmodifiableMap(map);
    }

    public Client(File config) throws IOException {
        this.config = new Serializer<ServerConfig>().parse(config, ServerConfig.class);
    }

    public void start() {
        try {
            Registry registry = LocateRegistry.getRegistry(this.config.getRegistryPort());
            // get RMI user registration service
            this.registrationService = (UserRegistrationInterface) registry.lookup("USER-REGISTRATION-SERVICE");
            // get RMI notification callback service
            this.notificationService = (FollowerNotificationServiceInterface) registry
                    .lookup("FOLLOWER-NOTIFICATION-SERVICE");
        } catch (RemoteException | NotBoundException e) {
            System.out.println(this.clientMessages.get("unable_connect"));
            System.exit(1);
        }

        SocketAddress sktAddr = new InetSocketAddress(this.config.getServerAddr(), this.config.getTcpPort());
        try (Scanner scanner = new Scanner(System.in)) {
            // start TCP connection
            this.sktChan = SocketChannel.open(sktAddr);

            System.out.println(this.clientMessages.get("welcome_message"));

            while (true) {
                System.out.print(this.clientMessages.get("waiting_for_command"));

                String instructionLine = scanner.nextLine();
                String[] instructionLineTokens = instructionLine.split(" ");
                String command = instructionLineTokens[0];
                String[] commandArguments = Arrays.copyOfRange(instructionLineTokens, 1, instructionLineTokens.length);
                if (command.equals("exit")) {
                    break;
                }

                // named arguments used in the switch branches
                String username, password, title, content, comment, parameter;
                UUID postId;
                int value;

                String renderedResponseData = "";
                try {
                    switch (command) {
                        case "register":
                            User newUser = this.register(commandArguments);
                            renderedResponseData = this.clientMessages.get("registration_ok") + "\n"
                                    + new UserRenderer().render(newUser);
                            break;
                        case "login":
                            username = this.getStringArgument(commandArguments, 0);
                            password = this.getStringArgument(commandArguments, 1);
                            this.login(username, password);
                            renderedResponseData = this.clientMessages.get("login_ok");
                            break;
                        case "logout":
                            username = this.getStringArgument(commandArguments, 0);
                            this.logout(username);
                            renderedResponseData = this.clientMessages.get("logout_ok");
                            break;
                        case "list":
                            parameter = instructionLineTokens[1];
                            User[] users;
                            switch (parameter) {
                                case "users":
                                    users = this.listUsers();
                                    break;
                                case "followers":
                                    users = this.listFollowers();
                                    break;
                                case "following":
                                    users = this.listFollowing();
                                    break;
                                default:
                                    System.out.println(
                                            this.clientMessages.get("unknown_operation") + "list " + parameter);
                                    continue;
                            }
                            renderedResponseData = new UserRenderer().render(users);
                            break;
                        case "follow":
                            username = this.getStringArgument(commandArguments, 0);
                            this.followUser(username);
                            renderedResponseData = this.clientMessages.get("follow_ok") + username;
                            break;
                        case "unfollow":
                            username = this.getStringArgument(commandArguments, 0);
                            this.unfollowUser(username);
                            renderedResponseData = this.clientMessages.get("unfollow_ok") + username;
                            break;
                        case "blog":
                            Post[] blog = this.viewBlog();
                            renderedResponseData = new PostRenderer().render(blog);
                            break;
                        case "post":
                            title = this.getStringArgument(commandArguments, 0, true);
                            content = this.getStringArgument(commandArguments, title.split(" ").length, true);
                            Post newPost = this.createPost(title, content);
                            renderedResponseData = this.clientMessages.get("new_post_ok") + "\n"
                                    + new PostRenderer().render(newPost);
                            break;
                        case "show":
                            parameter = instructionLineTokens[1];
                            switch (parameter) {
                                case "feed":
                                    Post[] feed = this.showFeed();
                                    renderedResponseData = new PostRenderer().render(feed);
                                    break;
                                case "post":
                                    postId = this.getUUIDArgument(commandArguments, 1);
                                    Post post = this.showPost(postId);
                                    renderedResponseData = new PostRenderer().render(post);
                                    break;
                                default:
                                    System.out.println(
                                            this.clientMessages.get("unknown_operation") + "show " + parameter);
                            }
                            break;
                        case "delete":
                            postId = this.getUUIDArgument(commandArguments, 0);
                            this.deletePost(postId);
                            renderedResponseData = this.clientMessages.get("delete_ok");
                            break;
                        case "rewin":
                            postId = this.getUUIDArgument(commandArguments, 0);
                            this.rewinPost(postId);
                            renderedResponseData = this.clientMessages.get("rewin_ok");
                            break;
                        case "rate":
                            postId = this.getUUIDArgument(commandArguments, 0);
                            value = this.getIntArgument(commandArguments, 1);
                            this.ratePost(postId, value);
                            renderedResponseData = this.clientMessages.get("rate_ok");
                            break;
                        case "comment":
                            postId = this.getUUIDArgument(commandArguments, 0);
                            comment = this.getStringArgument(commandArguments, 1, true);
                            this.addComment(postId, comment);
                            renderedResponseData = this.clientMessages.get("comment_ok");
                            break;
                        case "wallet":
                            try {
                                parameter = instructionLineTokens[1];
                            } catch (ArrayIndexOutOfBoundsException e) {
                                Wallet wallet = this.getWallet();
                                renderedResponseData = new WalletRenderer().render(wallet);
                                break;
                            }
                            switch (parameter) {
                                case "btc":
                                    double btcValue = this.getWalletInBitcoin();
                                    renderedResponseData = this.clientMessages.get("btc_wallet") + btcValue;
                                    break;
                                default:
                                    System.out.println(
                                            this.clientMessages.get("unknown_operation") + "wallet " + parameter);
                            }
                            break;
                        default:
                            System.out.println(this.clientMessages.get("unknown_operation") + command);
                    }
                    System.out.println(renderedResponseData);
                } catch (InvalidClientArgumentsException e) {
                    System.out.println(this.clientMessages.get("invalid_operation_arguments") + e.getMessage());
                } catch (ClientOperationFailedException e) {
                    System.out.println(this.getOperationFailedMessage(e));
                }
            }
            this.sktChan.close();
            this.discardLoggedInServices();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void discardLoggedInServices() {
        System.out.println(this.clientMessages.get("discarding_services"));
        if (this.multicastThread != null) {
            this.multicastThread.interrupt();
            try {
                this.multicastThread.join();
            } catch (InterruptedException e) {
                ;
            }
            this.multicastThread = null;
        }
        if (this.multicastSkt != null) {
            this.multicastSkt.close();
        }
        if (this.clientNotificationService != null) {
            try {
                UnicastRemoteObject.unexportObject(this.clientNotificationService, true);
            } catch (NoSuchObjectException e) {
                System.exit(1);
            }
            this.clientNotificationService = null;
        }
    }

    private String getOperationFailedMessage(ClientOperationFailedException exc) {
        if (exc.getMessage() != null && exc.getMessage().length() > 0) {
            return exc.getMessage();
        }

        assert exc.getRequest() != null && exc.getResponse() != null;

        if (exc.getResponse().getCode() == 401) {
            return this.clientMessages.get("not_logged_in");
        }

        return this.outcomeMessages
                .get(exc.getRequest().getPath())
                .get(exc.getResponse().getCode());
    }

    private Map<String, String> getRequestHeaders() {
        return this.requestHeaders;
    }

    private UUID getUUIDArgument(String[] arguments, int at) throws InvalidClientArgumentsException {
        try {
            return UUID.fromString(arguments[at]);
        } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
            throw new InvalidClientArgumentsException("UUID");
        }
    }

    private String getStringArgument(String[] arguments, int at)
            throws InvalidClientArgumentsException {
        return this.getStringArgument(arguments, at, false);
    }

    private String getStringArgument(String[] arguments, int at, boolean allowMultipleWords)
            throws InvalidClientArgumentsException {
        String firstToken;
        try {
            firstToken = arguments[at];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new InvalidClientArgumentsException("string");
        }
        if (!allowMultipleWords || firstToken.charAt(0) != '"' || firstToken.charAt(firstToken.length() - 1) == '"') {
            return firstToken; // single-word, non-quoted string
        }
        String ret = firstToken;
        for (int i = at + 1;; i++) {
            // continue until encounter another " or the line ends
            try {
                ret += " " + arguments[i];
                if (arguments[i].contains("\"")) {
                    break;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                break;
            }
        }
        // System.out.println("ARG: " + ret);
        return ret;

    }

    private int getIntArgument(String[] arguments, int at) throws InvalidClientArgumentsException {
        try {
            return Integer.parseInt(arguments[at]);
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            throw new InvalidClientArgumentsException("integer");
        }
    }

    private RestResponse receiveResponse(RestRequest request) throws IOException, ClientOperationFailedException {
        this.sktChan.write(ByteBuffer.wrap(request.toString().getBytes()));

        ByteBuffer buf = ByteBuffer.allocate(this.BUF_CAPACITY);
        this.sktChan.read(buf);
        buf.flip();

        String responseString = StandardCharsets.UTF_8.decode(buf).toString();
        // System.out.println("RESPONSE:\n" + responseString);
        RestResponse response = RestResponse.fromString(responseString);

        if (response.isClientErrorResponse() || response.isServerErrorResponse()) {
            throw new ClientOperationFailedException(request, response);
        }

        return response;
    }

    public User register(String[] args)
            throws InvalidClientArgumentsException, ClientOperationFailedException, IOException {
        String username = this.getStringArgument(args, 0);
        String password = this.getStringArgument(args, 1);
        Set<String> tags = new HashSet<>();

        // at least one tag is mandatory
        String tag1 = this.getStringArgument(args, 2);
        tags.add(tag1);

        // get the remaining optional tags (up to 4 more)
        for (int i = 0; i < 4; i++) {
            try {
                String nextTag = this.getStringArgument(args, i + 3);
                tags.add(nextTag);
            } catch (InvalidClientArgumentsException e) {
                // no more tags
                break;
            }
        }

        RestResponse response = null;

        try {
            response = this.registrationService.registerUserHandler(username, password, tags);
        } catch (RemoteException e) {
            e.printStackTrace();
            System.exit(1);
        }

        if (response.isClientErrorResponse() || response.isServerErrorResponse()) {
            throw new ClientOperationFailedException(
                    response.getCode() == 400 ? this.clientMessages.get("invalid_register_data")
                            : (response.getCode() == 403 ? this.clientMessages.get("username_already_taken")
                                    : this.clientMessages.get("unknown_registration_error")));
        }

        // System.out.println(response.getBody());
        return new Serializer<User>().parse(response.getBody(), User.class);

    }

    @Override
    public void login(String username, String password) throws IOException, ClientOperationFailedException {
        if (this.getRequestHeaders().get("Authorization") != null) {
            throw new ClientOperationFailedException(this.clientMessages.get("already_logged_in"));
        }

        RestResponse response = this
                .receiveResponse(new RestRequest("/login", HttpMethod.POST, null, username + "\n" + password));

        AuthenticationToken authToken = new AuthenticationToken(response.getBody().split("\n")[0].trim());

        // include received token in future requests to server
        this.requestHeaders.put("Authorization", "Bearer " + authToken.getToken());

        // subscribe to follower updates service
        this.clientNotificationService = new ClientFollowerNotificationService(this.localFollowers);
        IClientFollowerNotificationService callbackStub = (IClientFollowerNotificationService) UnicastRemoteObject
                .exportObject(this.clientNotificationService, 0);
        this.notificationService.subscribe(callbackStub, authToken);

        // join multicast group
        InetAddress multicastAddr = InetAddress.getByName(response.getBody().split("\n")[1].substring(1));
        int multicastPort = Integer.parseInt(response.getBody().split("\n")[2]);

        this.multicastSkt = new MulticastSocket(multicastPort);
        InetSocketAddress multicastGroup = new InetSocketAddress(multicastAddr, multicastPort);
        NetworkInterface netIf = NetworkInterface.getByName("wlan1");
        multicastSkt.joinGroup(multicastGroup, netIf);

        this.multicastThread = new Thread(() -> {
            byte[] buf = new byte[this.BUF_CAPACITY];
            while (true) {
                if (Thread.interrupted()) {
                    break;
                }
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                try {
                    this.multicastSkt.receive(pkt);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // assert new String(pkt.getData()).equals("WALLETS_UPDATED");
                // System.out.println(new String(pkt.getData()));
            }
        });
        this.multicastThread.start();
    }

    @Override
    public void logout(String username) throws IOException, ClientOperationFailedException {
        this.receiveResponse(new RestRequest("/logout", HttpMethod.POST, this.getRequestHeaders(), username));
        this.requestHeaders.remove("Authorization");
        this.discardLoggedInServices();
    }

    @Override
    public User[] listUsers() throws IOException, ClientOperationFailedException {
        RestResponse response = this
                .receiveResponse(new RestRequest("/users", HttpMethod.GET, this.getRequestHeaders()));

        User[] data = new Serializer<User[]>().parse(response.getBody(), User[].class);
        return data;
    }

    @Override
    public User[] listFollowers() {
        return (User[]) this.localFollowers.toArray(new User[0]);
    }

    @Override
    public User[] listFollowing() throws IOException, ClientOperationFailedException {
        RestResponse response = this
                .receiveResponse(new RestRequest("/users/following", HttpMethod.GET, this.getRequestHeaders()));

        User[] data = new Serializer<User[]>().parse(response.getBody(), User[].class);
        return data;
    }

    @Override
    public void followUser(String username) throws IOException, ClientOperationFailedException {
        this.receiveResponse(new RestRequest("/users/following", HttpMethod.PUT, this.getRequestHeaders(), username));
    }

    @Override
    public void unfollowUser(String username) throws IOException, ClientOperationFailedException {
        this.receiveResponse(
                new RestRequest("/users/following", HttpMethod.DELETE, this.getRequestHeaders(), username));
    }

    @Override
    public Post[] viewBlog() throws IOException, ClientOperationFailedException {
        RestResponse response = this
                .receiveResponse(new RestRequest("/posts/my-posts", HttpMethod.GET, this.getRequestHeaders()));

        Post[] data = new Serializer<Post[]>().parse(response.getBody(), Post[].class);
        return data;
    }

    @Override
    public Post createPost(String title, String content) throws IOException, ClientOperationFailedException {
        String requestData;
        try {
            requestData = new Serializer<Post>().serialize(new Post(title, content));
        } catch (IllegalArgumentException e) {
            throw new ClientOperationFailedException(this.clientMessages.get("post_constraint_failed"));
        }
        RestResponse response = this
                .receiveResponse(new RestRequest("/posts", HttpMethod.POST, this.getRequestHeaders(), requestData));

        Post responseData = new Serializer<Post>().parse(response.getBody(), Post.class);
        return responseData;
    }

    @Override
    public Post[] showFeed() throws IOException, ClientOperationFailedException {
        RestResponse response = this
                .receiveResponse(new RestRequest("/posts", HttpMethod.GET, this.getRequestHeaders()));

        Post[] data = new Serializer<Post[]>().parse(response.getBody(), Post[].class);
        return data;
    }

    @Override
    public Post showPost(UUID postId) throws IOException, ClientOperationFailedException {
        RestResponse response = this
                .receiveResponse(
                        new RestRequest("/posts/" + postId.toString(), HttpMethod.GET, this.getRequestHeaders()));

        Post data = new Serializer<Post>().parse(response.getBody(), Post.class);
        return data;
    }

    @Override
    public void deletePost(UUID postId) throws IOException, ClientOperationFailedException {
        this.receiveResponse(
                new RestRequest("/posts/" + postId.toString(), HttpMethod.DELETE, this.getRequestHeaders()));
    }

    @Override
    public Post rewinPost(UUID postId) throws IOException, ClientOperationFailedException {
        RestResponse response = this
                .receiveResponse(
                        new RestRequest("/posts/" + postId.toString() + "/rewin", HttpMethod.POST,
                                this.getRequestHeaders()));

        Post data = new Serializer<Post>().parse(response.getBody(), Post.class);
        return data;
    }

    @Override
    public void ratePost(UUID postId, int value) throws IOException, ClientOperationFailedException {
        this.receiveResponse(
                new RestRequest("/posts/" + postId.toString() + "/rate", HttpMethod.POST, this.getRequestHeaders(),
                        Integer.toString(value)));
    }

    @Override
    public void addComment(UUID postId, String comment) throws IOException, ClientOperationFailedException {
        String requestData = new Serializer<Comment>().serialize(new Comment(comment));
        this.receiveResponse(new RestRequest("/posts/" + postId.toString() + "/comments", HttpMethod.POST,
                this.getRequestHeaders(), requestData));
    }

    @Override
    public Wallet getWallet() throws IOException, ClientOperationFailedException {
        RestResponse response = this
                .receiveResponse(new RestRequest("/wallet", HttpMethod.GET, this.getRequestHeaders()));

        return new Serializer<Wallet>().parse(response.getBody(), Wallet.class);
    }

    @Override
    public double getWalletInBitcoin() throws IOException, ClientOperationFailedException {
        RestResponse response = this
                .receiveResponse(new RestRequest("/wallet/btc", HttpMethod.GET, this.getRequestHeaders()));

        return Double.parseDouble(response.getBody().trim());
    }

}
