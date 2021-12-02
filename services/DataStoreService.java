package services;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import auth.AuthenticationToken;
import auth.Password;
import entities.Comment;
import entities.Post;
import entities.Reaction;
import entities.User;

public class DataStoreService {
    /**
     * nestedAccessLock is used when having to modify a complex object associated
     * to a key in one of the ConcurrentHashMap (for example a Set or a Post
     * instance),
     * to prevent another thread from concurrently accessing the object (which
     * isn't thread-safe)
     * 
     * The common pattern used in the methods that employ this lock is:
     * - lock nestedAccessLock
     * - get the object from the ConcurrentHashMap
     * - enter a synchronized block for that object
     * - release nestedAccessLock (while *still* in the synchronized block)
     * - modify the object and leave the synchronized block
     * 
     * This prevents the object from being modified in between the `get` call
     * to the concurrent map and when we actually edit it, making the whole
     * modification to it atomic to the eyes of other threads
     */
    private final ReentrantLock nestedAccessLock = new ReentrantLock();

    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<String, User>();
    private final ConcurrentHashMap<String, Set<Post>> userPosts = new ConcurrentHashMap<String, Set<Post>>();
    private final ConcurrentHashMap<String, AuthenticationToken> sessions = new ConcurrentHashMap<String, AuthenticationToken>();
    private final ConcurrentHashMap<UUID, Post> posts = new ConcurrentHashMap<UUID, Post>();
    private final ConcurrentHashMap<String, Set<String>> followers = new ConcurrentHashMap<String, Set<String>>();

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
        this.sessions.compute(username, (k, v) -> v = token);
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
        this.nestedAccessLock.lock();
        for (Map.Entry<String, Set<String>> entry : this.followers.entrySet()) {
            if (entry.getValue().contains(username)) {
                following.add(entry.getKey());
            }
        }
        this.nestedAccessLock.unlock();
        return following;
    }

    public Set<Post> getUserPosts(String username) {
        return this.userPosts.get(username);
    }

    public void addFollower(String username, String newFollower) {
        this.nestedAccessLock.lock();
        Set<String> followers = this.followers.get(username);
        synchronized (followers) {
            this.nestedAccessLock.unlock();
            followers.add(newFollower);
        }
    }

    public void removeFollower(String username, String removedFollower) {
        this.nestedAccessLock.lock();
        Set<String> followers = this.followers.get(username);
        synchronized (followers) {
            this.nestedAccessLock.unlock();
            followers.remove(removedFollower);
        }
    }

    public Set<Post> getUserFeed(String username) {
        // TODO probably won't work
        Set<Post> feed = new HashSet<Post>();
        this.nestedAccessLock.lock();

        for (String followed : this.getUserFollowing(username)) {
            feed.addAll(this.getUserPosts(followed));
        }

        this.nestedAccessLock.unlock();
        return feed;
    }

    public Post createPost(String username, String title, String content) {
        Post newPost = new Post(username, title, content);
        this.nestedAccessLock.lock();
        Set<Post> userPosts = this.userPosts.get(username);
        synchronized (userPosts) {
            this.nestedAccessLock.unlock();
            userPosts.add(newPost);
        }
        this.posts.put(newPost.getId(), newPost);
        return newPost;
    }

    public Post getPost(UUID id) {
        return this.posts.get(id);
    }

    public void deletePost(UUID id) {
        Post deletingPost = this.posts.get(id);
        this.nestedAccessLock.lock();
        Set<Post> userPosts = this.userPosts.get(deletingPost.getAuthor());
        synchronized (userPosts) {
            this.posts.remove(id);
            this.nestedAccessLock.unlock();
            userPosts.remove(deletingPost);
        }
    }

    public boolean addPostReaction(String username, UUID postId, short reactionValue) {
        Reaction newReaction = new Reaction(username, reactionValue);
        boolean ret;
        this.nestedAccessLock.lock();
        Post post = this.posts.get(postId);
        if (post != null) {
            post.addReaction(newReaction);
            ret = true;
        } else {
            ret = false;
        }
        this.nestedAccessLock.unlock();
        return ret;
    }

    public boolean addPostComment(String username, UUID postId, String comment) {
        boolean ret;
        Comment newComment = new Comment(username, comment);
        this.nestedAccessLock.lock();
        Post post = this.posts.get(postId);
        if (post != null) {
            post.addComment(newComment);
            ret = true;
        } else {
            ret = false;
        }
        this.nestedAccessLock.unlock();
        return ret;
    }

}
