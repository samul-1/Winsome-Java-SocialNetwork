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
import java.util.Scanner;
import java.util.UUID;

import auth.Password;
import entities.Post;
import entities.User;
import services.Serializer;
import services.ServerConfig;

public class Client implements IClient {
    private final ServerConfig config;

    public Client(File config) throws IOException {
        this.config = new Serializer<ServerConfig>().parse(config, ServerConfig.class);
    }

    public void start() {
        System.out.println("Starting client...");
        SocketAddress sktAddr = new InetSocketAddress(this.config.getServerAddr(), this.config.getTcpPort());
        try (
                SocketChannel sktChan = SocketChannel.open(sktAddr);
                Scanner scanner = new Scanner(System.in)) {
            System.out.println("Connected!");
            while (true) {
                System.out.println("Waiting for message...");

                String msg = scanner.nextLine();
                System.out.println("Sent: " + msg);
                if (msg.equals("EXIT")) {
                    break;
                }
                ByteBuffer buf = ByteBuffer.wrap(msg.getBytes());
                sktChan.write(buf);

                buf.flip();
                sktChan.read(buf);
                buf.flip();
                System.out.println("Received: " + StandardCharsets.UTF_8.decode(buf).toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void login(String username, Password password) {
        // TODO Auto-generated method stub

    }

    @Override
    public void logout(String username) {
        // TODO Auto-generated method stub

    }

    @Override
    public User[] listUsers() {
        // TODO Auto-generated method stub
        return null;
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
