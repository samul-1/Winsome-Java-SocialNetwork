package client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
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

import auth.Password;
import entities.Post;
import entities.User;
import exceptions.OperationFailedException;
import protocol.HttpMethod;
import protocol.RestRequest;
import protocol.RestResponse;
import services.Serializer;
import services.ServerConfig;

public class Client implements IClient {
    private final String UNKNOWN_OPERATION_MSG = "Unknown operation: ";
    private final int BUF_CAPACITY = 4096 * 4096;
    private final ServerConfig config;

    private SocketChannel sktChan;

    private final Map<String, String> clientMessages = initMap();

    private Map<String, String> initMap() {
        Map<String, String> map = new HashMap<>();
        map.put("client_ok", "Operation completed successfully.");
        map.put("wrong_username_password", "Wrong username or password.");
        map.put("server_error", "The server is temporarily unreachable.");
        map.put("cannot_log_out", "You can't log out using this username.");
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
                        switch (parameter) {
                            case "users":
                                User[] data = this.listUsers();
                                renderedResponseData = new UserRenderer().render(data);
                                break;
                            case "followers":
                                this.listFollowers();
                                break;
                            case "following":
                                this.listFollowing();
                                break;
                            default:
                                System.out.println(this.UNKNOWN_OPERATION_MSG + "list- " + parameter);
                        }
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
                        this.viewBlog();
                        break;
                    case "post":
                        title = this.getStringArgument(commandArguments, 0);
                        content = this.getStringArgument(commandArguments, 1);
                        this.createPost(title, content);
                        break;
                    case "show":
                        parameter = instructionLineTokens[1];
                        switch (parameter) {
                            case "feed":
                                this.showFeed();
                                break;
                            case "post":
                                postId = this.getUUIDArgument(commandArguments, 0);
                                this.showPost(postId);
                                break;
                            default:
                                System.out.println(this.UNKNOWN_OPERATION_MSG + "show " + parameter);
                        }
                        break;
                    case "delete":
                        postId = this.getUUIDArgument(commandArguments, 0);
                        this.deletePost(postId);
                        break;
                    case "rewin":
                        postId = this.getUUIDArgument(commandArguments, 0);
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
                        System.out.println(this.UNKNOWN_OPERATION_MSG + command);
                }
                System.out.println(renderedResponseData);
            }
            this.sktChan.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (OperationFailedException e) {
            System.out.println("OPERATION FAILED!");
            e.printStackTrace();
        }
    }

    private UUID getUUIDArgument(String[] arguments, int at) {
        return null;
    }

    private String getStringArgument(String[] arguments, int at) {
        return null;
    }

    private int getIntArgument(String[] arguments, int at) {
        return 1;
    }

    private void sendRequest(RestRequest request) throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(request.toString().getBytes());
        this.sktChan.write(buf);
    }

    private Map<String, String> getRequestHeaders() {
        Map<String, String> ret = new HashMap<>();
        ret.put("Authorization", "Bearer aaa");
        return ret;
    }

    private RestResponse receiveResponse() throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(this.BUF_CAPACITY);
        this.sktChan.read(buf);
        buf.flip();
        String responseString = StandardCharsets.UTF_8.decode(buf).toString();
        System.out.println("RESPONSE:\n" + responseString);
        RestResponse response = RestResponse.fromString(responseString);

        return response;
    }

    @Override
    public void login(String username, String password) throws IOException, OperationFailedException {
        RestRequest request = new RestRequest("/login", HttpMethod.POST, null, username + "\n" + password);
        this.sendRequest(request);
        RestResponse response = this.receiveResponse();

        if (response.isClientErrorResponse()) {
            throw new OperationFailedException(this.clientMessages.get("wrong_username_password"));
        } else if (response.isServerErrorResponse()) {
            throw new OperationFailedException(this.clientMessages.get("server_error"));
        }
    }

    @Override
    public void logout(String username) throws IOException, OperationFailedException {
        RestRequest request = new RestRequest("/logout", HttpMethod.POST, this.getRequestHeaders(), username);
        this.sendRequest(request);
        RestResponse response = this.receiveResponse();

        if (response.isClientErrorResponse()) {
            throw new OperationFailedException(this.clientMessages.get("cannot_log_out"));
        } else if (response.isServerErrorResponse()) {
            throw new OperationFailedException(this.clientMessages.get("server_error"));
        }
    }

    @Override
    public User[] listUsers() throws IOException, OperationFailedException {
        RestRequest request = new RestRequest("/users", HttpMethod.GET, this.getRequestHeaders());
        this.sendRequest(request);
        RestResponse response = this.receiveResponse();
        if (response.isClientErrorResponse()) {
            throw new OperationFailedException(this.clientMessages.get("cannot_log_out"));
        } else if (response.isServerErrorResponse()) {
            throw new OperationFailedException(this.clientMessages.get("server_error"));
        }

        User[] data = new Serializer<User[]>().parse(response.getBody(), User[].class);
        return data;
    }

    @Override
    public User[] listFollowers() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public User[] listFollowing() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void followUser(String username) {
        // TODO Auto-generated method stub

    }

    @Override
    public void unfollowUser(String username) {
        // TODO Auto-generated method stub

    }

    @Override
    public Post[] viewBlog() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Post createPost(String title, String content) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Post[] showFeed() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Post showPost(UUID postId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deletePost(UUID postId) {
        // TODO Auto-generated method stub

    }

    @Override
    public Post rewinPost(UUID postId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void ratePost(UUID postId, int value) {
        // TODO Auto-generated method stub

    }

    @Override
    public Post addComment(UUID postId, String comment) {
        // TODO Auto-generated method stub
        return null;
    }

}
