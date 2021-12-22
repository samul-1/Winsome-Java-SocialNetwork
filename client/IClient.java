package client;

import java.io.IOException;
import java.util.UUID;

import entities.Post;
import entities.User;
import exceptions.OperationFailedException;

public interface IClient {
    void login(String username, String password) throws IOException, OperationFailedException;

    void logout(String username) throws IOException, OperationFailedException;

    User[] listUsers() throws IOException, OperationFailedException;

    User[] listFollowers();

    User[] listFollowing() throws IOException, OperationFailedException;

    void followUser(String username) throws IOException, OperationFailedException;

    void unfollowUser(String username) throws IOException, OperationFailedException;

    Post[] viewBlog() throws IOException, OperationFailedException;

    Post createPost(String title, String content) throws IOException, OperationFailedException;

    Post[] showFeed() throws IOException, OperationFailedException;

    Post showPost(UUID postId) throws IOException, OperationFailedException;

    void deletePost(UUID postId) throws IOException, OperationFailedException;

    Post rewinPost(UUID postId) throws IOException, OperationFailedException;

    void ratePost(UUID postId, int value) throws IOException, OperationFailedException;

    void addComment(UUID postId, String comment);

    // WalletData getWallet();

    // long getWalletInBitcoin();
}
