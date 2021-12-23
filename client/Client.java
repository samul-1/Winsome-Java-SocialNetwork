package client;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
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
import services.Serializer;
import services.ServerConfig;

public class Client implements IClient {
    private final int BUF_CAPACITY = 4096 * 4096;
    private final ServerConfig config;

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
        // map.put(401, "401 UNAUTHORIZED");
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
        System.out.println("Starting client...");
        SocketAddress sktAddr = new InetSocketAddress(this.config.getServerAddr(), this.config.getTcpPort());
        try (
                Scanner scanner = new Scanner(System.in)) {
            this.sktChan = SocketChannel.open(sktAddr);
            System.out.println("Connected!");

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
                            title = this.getStringArgument(commandArguments, 0);
                            content = this.getStringArgument(commandArguments, 1);
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
                                    postId = this.getUUIDArgument(commandArguments, 0);
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
                            comment = this.getStringArgument(commandArguments, 1);
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
                }
            }
            this.sktChan.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (ClientOperationFailedException e) {
            System.out.println("OPERATION FAILED!");
            e.printStackTrace();
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

    private String getStringArgument(String[] arguments, int at) throws InvalidClientArgumentsException {
        try {
            return arguments[at];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new InvalidClientArgumentsException("string");
        }
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

    @Override
    public void login(String username, String password) throws IOException, ClientOperationFailedException {
        RestResponse response = this
                .receiveResponse(new RestRequest("/login", HttpMethod.POST, null, username + "\n" + password));

        this.requestHeaders.put("Authorization", new AuthenticationToken(response.getBody()).toString());
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
        this.receiveResponse(new RestRequest("/users/following", HttpMethod.PUT, this.getRequestHeaders(), username));
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
