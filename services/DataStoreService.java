package services;

import java.util.Set;
import java.util.TreeSet;
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
import services.DataStoreService.OperationStatus.Status;

@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)

public class DataStoreService {
    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<Post>> userPosts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<AuthenticationToken, User> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Post> posts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> followers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Wallet> wallets = new ConcurrentHashMap<>();
    @JsonIgnore
    private final ConcurrentHashMap<String, IClientFollowerNotificationService> notificationCallbacks = new ConcurrentHashMap<>();
    private String storageFileName = "";

    public static class OperationStatus {
        /**
         * Helper class used as a wrapper around the Status enum.
         * 
         * The Status enum is used to enrich the information returned by an
         * operation whose success value is boolean (success/failure) but
         * the failure can happen for more than one reason, and the information
         * about what caused the failure is to be retained for informational
         * purposes
         * 
         */
        public enum Status {
            OK,
            NOT_FOUND,
            ILLEGAL_OPERATION
        }

        public Status status = Status.OK;
    }

    public static DataStoreService restoreOrCreate(String source) {
        /**
         * Attempts to restore the state saved to a json file. If no
         * valid file is provided, initializes an empty store and returns
         * it.
         * 
         */
        try {
            File sourceFile = new File(source);
            DataStoreService ret = new Serializer<DataStoreService>().parse(sourceFile, DataStoreService.class);
            ret.startStatePersistenceThread();
            return ret;
        } catch (IOException | NullPointerException e) {
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
        System.out.println("starting thread");
        this.startStatePersistenceThread();
    }

    public void startStatePersistenceThread() {
        // start anonymous thread that periodically persists store state
        new Thread(() -> {
            System.out.println("thread started");
            while (true) {
                try {
                    // in a real-world app this delay would be much longer
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
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
        System.out.println("About to write state");
        try (PrintWriter writer = new PrintWriter(this.storageFileName, "UTF-8")) {
            writer.println(serializedState);
            writer.close();
            System.out.println("Written state");
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
        this.userPosts.put(username, new TreeSet<Post>());
        this.followers.put(username, new TreeSet<String>());
        this.wallets.put(username, new Wallet());
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
        Set<Post> feed = new TreeSet<>();

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

    public OperationStatus deletePost(UUID id, String fromUser) {
        OperationStatus status = new OperationStatus();
        Set<Post> chainedDeletionSet = new HashSet<>();

        this.posts.computeIfPresent(id, (__, toDelete) -> {
            try {
                // first try to remove post from the user's post set:
                // this will fail if the post doesn't belong to the user
                this.userPosts.computeIfPresent(fromUser, (user, postSet) -> {
                    if (!postSet.remove(toDelete)) {
                        // the user who requested the deletion isn't
                        // the author of the specified toDelete
                        throw new RuntimeException();
                    }
                    return postSet;
                });
            } catch (RuntimeException e) {
                // don't delete the post since the removal from
                // the user's post set failed

                // report appropriate error depending on whether the post attempted to be
                // deleted doesn't exist or the user who tried to delete it isn't its author
                status.status = this.posts.get(id) == null ? Status.NOT_FOUND : Status.ILLEGAL_OPERATION;
                return toDelete;
            }

            // existence and ownership of the post were verified - now
            // mark all rewins of that post for deletion
            this.posts.forEach((postId, p) -> {
                if (p.isRewin() && p.getRewinedPost().equals(toDelete)) {
                    chainedDeletionSet.add(p);
                }
            });
            // returning null removes the mapping for this post from posts map
            return null;
        });

        // finally delete the rewins
        for (Post rewin : chainedDeletionSet) {
            this.deletePost(rewin.getId(), rewin.getAuthor());
        }

        return status;
    }

    public OperationStatus addPostReaction(UUID postId, Reaction reaction) {
        OperationStatus status = new OperationStatus();
        if (this.posts.computeIfPresent(postId, (__, post) -> {
            if (!post.addReaction(reaction)) {
                status.status = Status.ILLEGAL_OPERATION;
            }
            return post;
        }) == null) {
            status.status = Status.NOT_FOUND;
        }
        return status;
    }

    public OperationStatus addPostComment(UUID postId, Comment comment) {
        OperationStatus status = new OperationStatus();
        if (this.posts.computeIfPresent(postId, (__, post) -> {
            if (!post.addComment(comment)) {
                status.status = Status.ILLEGAL_OPERATION;
            }
            return post;
        }) != null) {
            status.status = Status.NOT_FOUND;
        }
        return status;
    }

    public Wallet getUserWallet(String username) {
        return this.wallets.get(username);
    }

    public void updateUserWallet(String username, double delta) {
        this.wallets.computeIfPresent(username, (__, wallet) -> {
            wallet.addTransaction(delta);
            return wallet;
        });
    }

    public IClientFollowerNotificationService getUserCallbackReference(String username) {
        return this.notificationCallbacks.get(username);
    }

    public void setUserCallbackReference(String username, IClientFollowerNotificationService ref) {
        this.notificationCallbacks.put(username, ref);
    }
}
