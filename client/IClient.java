package client;

import java.io.IOException;
import java.util.UUID;

import entities.Post;
import entities.User;
import exceptions.ClientOperationFailedException;

public interface IClient {
    void login(String username, String password) throws IOException, ClientOperationFailedException;

    void logout(String username) throws IOException, ClientOperationFailedException;

    User[] listUsers() throws IOException, ClientOperationFailedException;

    User[] listFollowers();

    User[] listFollowing() throws IOException, ClientOperationFailedException;

    void followUser(String username) throws IOException, ClientOperationFailedException;

    void unfollowUser(String username) throws IOException, ClientOperationFailedException;

    Post[] viewBlog() throws IOException, ClientOperationFailedException;

    Post createPost(String title, String content) throws IOException, ClientOperationFailedException;

    Post[] showFeed() throws IOException, ClientOperationFailedException;

    Post showPost(UUID postId) throws IOException, ClientOperationFailedException;

    void deletePost(UUID postId) throws IOException, ClientOperationFailedException;

    Post rewinPost(UUID postId) throws IOException, ClientOperationFailedException;

    void ratePost(UUID postId, int value) throws IOException, ClientOperationFailedException;

    void addComment(UUID postId, String comment) throws IOException, ClientOperationFailedException;

    // WalletData getWallet();

    // long getWalletInBitcoin();
}
