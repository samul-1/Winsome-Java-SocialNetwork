package entities;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Post implements Comparable<Post> {
    private final UUID id;
    private String author;
    private final String title;
    private final String content;
    private final Set<Comment> comments = new TreeSet<Comment>();
    private final Date timestamp = new Date();

    private Post originalPost; // for rewin feature
    private final Set<Reaction> reactions = new TreeSet<Reaction>();

    @JsonCreator
    public Post(
            @JsonProperty("title") String title,
            @JsonProperty("content") String content) {
        if (title == null || title.length() == 0 || content == null
                || content.length() == 0 || title.length() > 20 || content.length() > 500) {
            throw new IllegalArgumentException();
        }
        this.title = title;
        this.content = content;
        this.id = UUID.randomUUID();
    }

    public Post(String author, String title, String content) {
        this(title, content);
        this.author = author;
    }

    public Post(String rewiner, Post rewinedPost) {
        this.author = rewiner;
        this.title = "";
        this.content = "";
        this.id = UUID.randomUUID();

        this.originalPost = rewinedPost.isRewin() ? rewinedPost.getRewinedPost() : rewinedPost;
    }

    public UUID getId() {
        return this.id;
    }

    public void setAuthor(String username) {
        this.author = username;
    }

    public String getAuthor() {
        return this.author;
    }

    public String getTitle() {
        return this.title;
    }

    public String getContent() {
        return this.content;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    @JsonProperty("upvotes")
    public int getUpvotesCount() {
        return this.getUpvotes().size();
    }

    @JsonProperty("downvotes")
    public int getDownvotesCount() {
        return this.getDownvotes().size();
    }

    public boolean addComment(Comment comment) {
        if (comment.getUser().equals(this.author)) {
            return false;
        }
        this.comments.add(comment);
        return true;
    }

    public boolean addReaction(Reaction reaction) {
        // author of post adding reaction to their own post
        if (reaction.getUser().equals(this.author)) {
            return false;
        }
        // user adding more than one reaction to same post
        if (this.reactions.stream().anyMatch(r -> r.getUser().equals(reaction.getUser()))) {
            return false;
        }

        this.reactions.add(reaction);
        return true;
    }

    public Set<Comment> getComments() {
        return this.comments;
    }

    public Set<Reaction> getReactions() {
        return this.reactions;
    }

    @JsonIgnore
    public Set<Reaction> getUpvotes() {
        return this.reactions.stream().filter(reaction -> reaction.getValue() == 1).collect(Collectors.toSet());
    }

    @JsonIgnore
    public Set<Reaction> getDownvotes() {
        return this.reactions.stream().filter(reaction -> reaction.getValue() == -1).collect(Collectors.toSet());
    }

    @JsonIgnore
    @JsonProperty("originalPost")
    public Post getRewinedPost() {
        return this.originalPost;
    }

    public void setOriginalPost(Post post) {
        this.originalPost = post;
    }

    @JsonIgnore
    public boolean isRewin() {
        return this.originalPost != null;
    }

    public int compareTo(Post o) {
        return this.timestamp.compareTo(o.getTimestamp());
    }
}
