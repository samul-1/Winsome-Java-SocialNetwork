package entities;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

// TODO fix the issues with jsonignore, particularly the fact that you can't use "originalPost" and the upvote and downvote counts
public class Post {
    private final UUID id;
    private String author;
    private final String title;
    private final String content;
    private final Set<Comment> comments = new HashSet<Comment>();
    private final Date timestamp = new Date();

    @JsonIgnore
    private final Set<Reaction> reactions = new HashSet<Reaction>();
    @JsonIgnore
    private Post originalPost; // for retwin feature

    public Post(String author, String title, String content) {
        if (author == null || author.length() == 0 || title == null || title.length() == 0 || content == null
                || content.length() == 0) {
            throw new IllegalArgumentException();
        }
        this.author = author;
        this.title = title;
        this.content = content;
        this.id = UUID.randomUUID();
    }

    @JsonCreator
    public Post(
            @JsonProperty("title") String title,
            @JsonProperty("content") String content) {
        this.title = title;
        this.content = content;
        this.id = UUID.randomUUID();

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

    @JsonIgnore
    public int getUpVotesCount() {
        return this.getUpvotes().size();
    }

    @JsonIgnore
    public int getDownVotesCount() {
        return -1;
    }

    public void addComment(Comment comment) {
        this.comments.add(comment);
    }

    public void addReaction(Reaction reaction) {
        this.reactions.add(reaction);
    }

    public Set<Comment> getComments() {
        return this.comments;
    }

    public Set<Reaction> getReactions() {
        return this.reactions;
    }

    public Set<Reaction> getUpvotes() {
        return this.reactions.stream().filter(reaction -> reaction.getValue() == 1).collect(Collectors.toSet());
    }

    @JsonIgnore
    public Post getRewinedPost() {
        return this.originalPost;
    }

    @JsonIgnore
    public boolean isRewin() {
        return this.originalPost != null;
    }
}
