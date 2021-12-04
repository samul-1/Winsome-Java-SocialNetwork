package services;

import java.util.Map;
import java.util.Set;
import java.io.File;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import auth.AuthenticationToken;
import auth.Password;
import entities.Comment;
import entities.Post;
import entities.Reaction;
import entities.User;

public class DataStoreService {
    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<Post>> userPosts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AuthenticationToken> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Post> posts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> followers = new ConcurrentHashMap<>();

    public DataStoreService(File persistedState) {
        // TODO implement loading the state from a file
    }

    public boolean registerUser(String username, Set<String> tags, Password password) {
        /**
         * Attempts to associate a new user to the given username.
         * 
         * Returns true if the user is successfully registered;
         * false if the username is already taken
         * 
         */
        if (this.users.putIfAbsent(username, new User(username, tags, password)) != null) {
            return false;
        }
        // The above operation is atomic - it's now safe to do the two following
        // operations because even if more threads attempted to register a user
        // with the same username at the same time, at this point all but one
        // of them have returned false
        this.userPosts.put(username, new HashSet<Post>());
        this.followers.put(username, new HashSet<String>());

        return true;
    }

    public void setUserToken(String username, AuthenticationToken token) {
        /**
         * Binds a user (via their username) to the given authentication
         * token.
         * 
         * A new token is associated [resp. disassociated] to a user when
         * they log in [resp. out], and the token for a user is checked
         * when they make an authenticated request to validate their
         * identity
         * 
         */
        this.sessions.put(username, token);
    }

    public AuthenticationToken getUserToken(String username) {
        return this.sessions.get(username);
    }

    public User getUser(String username) {
        return this.users.get(username);
    }

    public Set<String> getUserFollowers(String username) {
        return this.followers.get(username);
    }

    public Set<String> getUserFollowing(String username) {
        Set<String> following = new HashSet<String>();

        this.followers.forEach((user, followerSet) -> {
            if (followerSet.contains(username)) {
                // if the requested username (A) is in the user's (B) follower
                // set, then this user is followed by the requested username
                // (A follows B)
                following.add(user);
            }
        });

        return following;
    }

    public Set<Post> getUserPosts(String username) {
        return this.userPosts.get(username);
    }

    public void addFollower(String username, String newFollower) {
        this.followers.computeIfPresent(username, (__, followerSet) -> {
            followerSet.add(newFollower);
            return followerSet;
        });
    }

    public void removeFollower(String username, String removedFollower) {
        this.followers.computeIfPresent(username, (__, followerSet) -> {
            followerSet.remove(removedFollower);
            return followerSet;
        });
    }

    public Set<Post> getUserFeed(String username) {
        Set<Post> feed = new HashSet<>();

        this.followers.forEach((user, followerSet) -> {
            if (followerSet.contains(username)) {
                // current user is in the follow set of requested username;
                // therefore their posts are in the user's feed
                feed.addAll(this.getUserPosts(user))
            }
        });

        return feed;
    }

    public void addPost(String username, Post newPost) {
        this.userPosts.computeIfPresent(username, (__, postSet) -> {
            postSet.add(newPost);
            return postSet;
        });
    }

    public Post getPost(UUID id) {
        return this.posts.get(id);
    }

    public boolean deletePost(UUID id, String fromUser) {
        return this.posts.computeIfPresent(id, (__, post) -> {
            try {
                // first try to remove post from the user's post set:
                // this will fail if the post doesn't belong to the user
                this.userPosts.computeIfPresent(fromUser, (user, postSet) -> {
                    if (!postSet.remove(post)) {
                        // the user who requested the deletion isn't
                        // the author of the specified post
                        throw new RuntimeException();
                    }
                    return postSet;
                });
            } catch (RuntimeException e) {
                // don't delete the post since the removal from the user's
                // post set failed
                return post;
            }
            return null;
        }) == null;
    }

    public boolean addPostReaction(String username, UUID postId, short reactionValue) {
        Reaction newReaction = new Reaction(username, reactionValue);

        return this.posts.computeIfPresent(postId, (__, post) -> {
            post.addReaction(newReaction);
            return post;
        }) != null;
    }

    public boolean addPostComment(String username, UUID postId, String comment) {
        Comment newComment = new Comment(username, comment);

        return this.posts.computeIfPresent(postId, (__, post) -> {
            post.addComment(newComment);
            return post;
        }) != null;
    }

}
