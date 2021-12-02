package services;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import auth.AuthenticationToken;
import entities.Post;
import entities.User;

public class DataStoreService {
    private final Map<String, User> users;
    private final Map<User, Set<Post>> userPosts;
    private final Map<User, AuthenticationToken> sessions;

    public void registerUser(String username, Set<String> tags) {
    }

    public void setUserToken(String username, AuthenticationToken token) {

    }

    public User getUser(String username) {

    }

    public Set<User> getUserFollowers(String username) {

    }

    public Set<User> getUserFollowing(String username) {

    }

    public Set<Post> getUserPosts(String username) {

    }

    public Set<Post> getUserFeed(String username) {

    }

    public Post createPost(String username, String title, String content) {

    }

    public Post getPost(UUID id) {

    }

    public void deletePost(UUID id) {

    }

    public void addPostReaction(String username, UUID postId, short reactionValue) {

    }

    public void addPostComment(String username, UUID postId, String comment) {

    }

}
