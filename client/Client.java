package client;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
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

public class Client implements IClient {
    private final int BUF_CAPACITY = 4096 * 4096;
    private final ServerConfig config;
    private UserRegistrationInterface registrationService = null;
    private FollowerNotificationServiceInterface notificationService = null;

    private Map<String, String> requestHeaders = new HashMap<>();

    private SocketChannel sktChan;

    private final Map<String, String> clientMessages = initMap();

    private Map<String, String> initMap() {
        Map<String, String> map = new HashMap<>();
        map.put("client_ok", "Operation completed successfully.");
        map.put("wrong_username_password", "Wrong username or password.");
        map.put("server_error", "The server is temporarily unreachable.");
        map.put("cannot_log_out", "You can't log out using this username.");
        map.put("unknown_operation", "Unknown operation: ");
        map.put("invalid_operation_arguments", "Invalid arguments. ");
        map.put("welcome_message", "Connected to server!\nTo start, type\n" +
                "register <username> <password> <tags> (at least one tag is REQUIRED)" +
                " or, if you already have an account,\nlogin <username> <password>");
        // map.put(403, "403 FORBIDDEN");
        // map.put(404, "404 NOT FOUND");
        // map.put(405, "405 METHOD NOT SUPPORTED");
        // map.put(500, "500 INTERNAL SERVER ERROR");
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
            e.printStackTrace();
            System.exit(1);
        }

        SocketAddress sktAddr = new InetSocketAddress(this.config.getServerAddr(), this.config.getTcpPort());
        try (Scanner scanner = new Scanner(System.in)) {
            // start TCP connection
            this.sktChan = SocketChannel.open(sktAddr);

            // join multicast group
            MulticastSocket multicastSkt = new MulticastSocket(this.config.getMulticastPort());
            InetSocketAddress multicastGroup = new InetSocketAddress(this.config.getMulticastAddr(),
                    this.config.getMulticastPort());
            NetworkInterface netIf = NetworkInterface.getByName("wlan1");
            // TODO fix below
            // multicastSkt.joinGroup(multicastGroup, netIf);

            // TODO spawn thread for multicast stuff

            System.out.println(this.clientMessages.get("welcome_message"));

            while (true) {
                System.out.println("Waiting for message...");

                String instructionLine = scanner.nextLine();
                String[] instructionLineTokens = instructionLine.split(" ");
                String command = instructionLineTokens[0];
                String[] commandArguments = Arrays.copyOfRange(instructionLineTokens, 1, instructionLineTokens.length);
                if (command.equals("EXIT")) {
                    break;
                }

                // named arguments used in the switch branches
                String username, password, title, content, comment, parameter;
                UUID postId;
                int value;

                String renderedResponseData = "";
                System.out.println(instructionLine);
                try {
                    switch (command) {
                        case "register":
                            User newUser = this.register(commandArguments);
                            renderedResponseData = new UserRenderer().render(newUser);
                            break;
                        case "login":
                            username = this.getStringArgument(commandArguments, 0);
                            password = this.getStringArgument(commandArguments, 1);
                            this.login(username, password);
                            break;
                        case "logout":
                            username = this.getStringArgument(commandArguments, 0);
                            this.logout(username);
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
                            break;
                        case "unfollow":
                            username = this.getStringArgument(commandArguments, 0);
                            this.unfollowUser(username);
                            break;
                        case "blog":
                            Post[] blog = this.viewBlog();
                            renderedResponseData = new PostRenderer().render(blog);
                            break;
                        case "post":
                            title = this.getStringArgument(commandArguments, 0, true);
                            content = this.getStringArgument(commandArguments, title.split(" ").length, true);
                            Post newPost = this.createPost(title, content);
                            renderedResponseData = new PostRenderer().render(newPost);
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
                            break;
                        case "rewin":
                            postId = this.getUUIDArgument(commandArguments, 0);
                            // ? maybe no need to return a post
                            this.rewinPost(postId);
                            break;
                        case "rate":
                            postId = this.getUUIDArgument(commandArguments, 0);
                            value = this.getIntArgument(commandArguments, 1);
                            this.ratePost(postId, value);
                            break;
                        case "comment":
                            postId = this.getUUIDArgument(commandArguments, 0);
                            comment = this.getStringArgument(commandArguments, 1, true);
                            this.addComment(postId, comment);
                            break;
                        case "wallet":
                            // TODO handle "wallet" and "wallet btc"
                            break;
                        default:
                            System.out.println(this.clientMessages.get("unknown_operation") + command);
                    }
                    System.out.println(renderedResponseData);
                } catch (InvalidClientArgumentsException e) {
                    System.out.println(this.clientMessages.get("invalid_operation_arguments") + e.getMessage());
                } catch (ClientOperationFailedException e) {
                    System.out.println("OPERATION FAILED!");
                    e.printStackTrace();
                }
            }
            this.sktChan.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private Map<String, String> getRequestHeaders() {
        // Map<String, String> ret = new HashMap<>();
        // ret.put("Authorization", "Bearer aaa");
        // return ret;
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
        if (firstToken.charAt(0) != '"' || !allowMultipleWords || firstToken.charAt(firstToken.length() - 1) == '"') {
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
        System.out.println("ARG: " + ret);
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
        System.out.println("RESPONSE:\n" + responseString);
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
            throw new ClientOperationFailedException(null, response);
        }

        System.out.println(response.getBody());
        return new Serializer<User>().parse(response.getBody(), User.class);

    }

    @Override
    public void login(String username, String password) throws IOException, ClientOperationFailedException {
        RestResponse response = this
                .receiveResponse(new RestRequest("/login", HttpMethod.POST, null, username + "\n" + password));

        AuthenticationToken authToken = new AuthenticationToken(response.getBody());

        // include received token in future requests to server
        this.requestHeaders.put("Authorization", "Bearer " + authToken.getToken());

        // subscribe to follower updates service
        IClientFollowerNotificationService callbackStub = (IClientFollowerNotificationService) UnicastRemoteObject
                .exportObject(new ClientFollowerNotificationService(), 0);
        this.notificationService.subscribe(callbackStub, authToken);
    }

    @Override
    public void logout(String username) throws IOException, ClientOperationFailedException {
        this.receiveResponse(new RestRequest("/logout", HttpMethod.POST, this.getRequestHeaders(), username));
        this.requestHeaders.remove("Authorization");
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
        // TODO Auto-generated method stub
        return null;
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
        String requestData = new Serializer<Post>().serialize(new Post(title, content));
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

}
