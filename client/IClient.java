package client;

import java.io.IOException;
import java.util.UUID;

import auth.Password;
import entities.Post;
import entities.User;

public interface IClient {
    void login(String username, String password) throws IOException;

    void logout(String username);

    User[] listUsers();

    User[] listFollowers();

    User[] listFollowing();

    void followUser(String username);

    void unfollowUser(String username);

    Post[] viewBlog();

    Post createPost(String title, String content);

    Post[] showFeed();

    Post showPost(UUID postId);

    void deletePost(UUID postId);

    Post rewinPost(UUID postId);

    void ratePost(UUID postId, int value);

    Post addComment(UUID postId, String comment);

    // WalletData getWallet();

    // long getWalletInBitcoin();
}
