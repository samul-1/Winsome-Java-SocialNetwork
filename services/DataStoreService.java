package services;

import java.util.Set;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import auth.AuthenticationToken;
import auth.Password;
import client.IClientFollowerNotificationService;
import entities.Comment;
import entities.Post;
import entities.Reaction;
import entities.User;

@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)

public class DataStoreService {
    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<Post>> userPosts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<AuthenticationToken, User> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Post> posts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> followers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Double> wallets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, IClientFollowerNotificationService> notificationCallbacks = new ConcurrentHashMap<>();
    private String storageFileName = "";

    public static DataStoreService restoreOrCreate(String source) {
        /**
         * Attempts to restore the state saved to a json file. If no
         * valid file is provided, initializes an empty store and returns
         * it.
         * 
         */
        try {
            File sourceFile = new File(source);
            return new Serializer<DataStoreService>().parse(sourceFile, DataStoreService.class);
        } catch (IOException e) {
            e.printStackTrace(); // TODO remove
            // file not found or invalid file content
            return new DataStoreService(source);
        }
    }

    @JsonCreator
    public DataStoreService() {
    }

    public DataStoreService(String storageFilename) {
        // dummy data for testing the web interface
        // loadFakeData();
        this.storageFileName = storageFilename;

        // start anonymous thread that periodically persists store state
        new Thread(() -> {
            while (true) {
                try {
                    // in a real-world app this delay would be much longer
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    ;
                }
                this.persistStoreState();
            }
        }).start();
    }

    private void persistStoreState() {
        /**
         * Serializes the current state of the data store and writes
         * it to file specified in the storageFileName field.
         * 
         */
        String serializedState = new Serializer<DataStoreService>().serialize(this);

        try (PrintWriter writer = new PrintWriter(this.storageFileName, "UTF-8")) {
            writer.println(serializedState);
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    private void loadFakeData() {
        /**
         * Utility function to load some fake data into the store,
         * used for testing the browser GUI more easily with some
         * data already present
         * 
         */
        String[] tags1 = { "Crypto", "CS", "JavaScript", "Python", "C++" };
        String[] tags2 = { "CS", "OCaml", "Python", "C" };
        String[] tags3 = { "Unix", "CS", "JavaScript", "Java", "C++" };
        this.registerUser("admin", new HashSet<String>(Arrays.asList(tags1)), new Password("p"));
        this.registerUser("admin2", new HashSet<String>(Arrays.asList(tags2)), new Password("password"));
        this.registerUser("admin3", new HashSet<String>(Arrays.asList(tags3)), new Password("password"));

        Post post1 = new Post("admin", "Test post 1", "Test post1 content abc abc abc");

        this.addPost("admin", post1);
        this.addPost("admin", new Post("admin", "Test post 2", "Test post2 content abc abc abc"));
        this.addPost("admin2", new Post("admin2", "Test post 3", "Test post3 content abc abc abc"));
        this.addFollower("admin2", "admin");
        this.addPostComment(post1.getId(), new Comment("admin2", "comment1"));
        this.addPostComment(post1.getId(), new Comment("admin", "comment2"));
        this.addPostComment(post1.getId(), new Comment("admin3", "comment3"));
        this.setUserToken(this.getUser("admin"), new AuthenticationToken("aaa"));
    }

    public User registerUser(String username, Set<String> tags, Password password) {
        /**
         * Attempts to associate a new user to the given username.
         * 
         * Returns true if the user is successfully registered;
         * false if the username is already taken
         * 
         */
        User newUser = new User(username, tags, password);
        if (this.users.putIfAbsent(username, newUser) != null) {
            return null;
        }
        // The above operation is atomic - it's now safe to do the three following
        // operations because even if more threads attempted to register a user
        // with the same username at the same time, at this point all but one
        // of them have returned false
        this.userPosts.put(username, new HashSet<Post>());
        this.followers.put(username, new HashSet<String>());
        this.wallets.put(username, 0.0);
        return newUser;
    }

    public void setUserToken(User user, AuthenticationToken token) {
        /**
         * Binds a user (via their username) to the given authentication
         * token.
         * 
         */
        this.sessions.put(token, user);
    }

    public boolean deleteUserToken(AuthenticationToken token) {
        return this.sessions.remove(token) != null;
    }

    public User getUserFromToken(AuthenticationToken token) {
        return this.sessions.get(token);
    }

    public User getUser(AuthenticationToken token) {
        return this.sessions.get(token);
    }

    public User getUser(String username) {
        return this.users.get(username);
    }

    public Set<User> getCompatibleUsers(String requestingUsername) {
        Set<User> ret = new HashSet<>();
        User requestingUser = this.getUser(requestingUsername);
        System.out.println("REQUESTING USER " + requestingUsername + ", user " + requestingUser);

        this.users.forEach((_username, user) -> {
            if (user.isCompatibleWith(requestingUser)) {
                // if the requested username (A) is in the user's (B) follower
                // set, then this user is followed by the requested username
                // (A follows B)
                ret.add(user);
            }
        });
        return ret;
    }

    public Set<User> getUserFollowers(String username) {
        Set<User> ret = new HashSet<>();
        this.followers.get(username).forEach(user -> ret.add(this.getUser(user)));
        return ret;
    }

    public Set<User> getUserFollowing(String username) {
        Set<User> following = new HashSet<>();

        this.followers.forEach((user, followerSet) -> {
            if (followerSet.contains(username)) {
                // if the requested username (A) is in the user's (B) follower
                // set, then this user is followed by the requested username
                // (A follows B)
                following.add(this.getUser(user));
            }
        });

        return following;
    }

    @JsonIgnore
    public Set<String> getUsernames() {
        return this.users.keySet();
    }

    public Set<Post> getUserPosts(String username) {
        return this.userPosts.get(username);
    }

    // TODO remove as you won't need this when you correct the formula
    public int getUserCommentCount(String username) {
        Set<Post> commentedPosts = new HashSet<>();
        this.posts.forEach((__, post) -> {
            Set<Comment> postComments = post.getComments();
            if (postComments
                    .stream()
                    .filter(comment -> comment.getUser().equals(username))
                    .findAny()
                    .isPresent()) {
                commentedPosts.add(post);
            }
        });
        return commentedPosts.size();
    }

    public boolean addFollower(String username, String newFollower) {
        return this.followers.computeIfPresent(username, (__, followerSet) -> {
            followerSet.add(newFollower);
            return followerSet;
        }) != null;
    }

    public boolean removeFollower(String username, String removedFollower) {
        return this.followers.computeIfPresent(username, (__, followerSet) -> {
            followerSet.remove(removedFollower);
            return followerSet;
        }) != null;
    }

    public Set<Post> getUserFeed(String username) {
        Set<Post> feed = new HashSet<>();

        this.followers.forEach((user, followerSet) -> {
            if (followerSet.contains(username)) {
                // current user is in the follow set of requested username;
                // therefore their posts are in the user's feed
                feed.addAll(this.getUserPosts(user));
            }
        });

        return feed;
    }

    public void addPost(String username, Post newPost) {
        this.userPosts.computeIfPresent(username, (__, postSet) -> {
            postSet.add(newPost);
            this.posts.put(newPost.getId(), newPost);
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
                // don't delete the post since the removal from
                // the user's post set failed
                return post;
            }
            return null;
        }) == null;
    }

    public boolean addPostReaction(UUID postId, Reaction reaction) {
        return this.posts.computeIfPresent(postId, (__, post) -> {
            post.addReaction(reaction);
            return post;
        }) != null;
    }

    public boolean addPostComment(UUID postId, Comment comment) {
        return this.posts.computeIfPresent(postId, (__, post) -> {
            post.addComment(comment);
            return post;
        }) != null;
    }

    public double updateUserWallet(String username, double delta) {
        return this.wallets.computeIfPresent(username, (__, balance) -> balance + delta);
    }

    public IClientFollowerNotificationService getUserCallbackReference(String username) {
        return this.notificationCallbacks.get(username);
    }

    public void setUserCallbackReference(String username, IClientFollowerNotificationService ref) {
        this.notificationCallbacks.put(username, ref);
    }
}
