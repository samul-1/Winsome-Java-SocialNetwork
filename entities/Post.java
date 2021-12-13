package entities;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Post {
    private final UUID id;
    private String authorUsername;
    private final String title;
    private final String content;
    private final Set<Comment> comments = new HashSet<Comment>();
    private final Set<Reaction> reactions = new HashSet<Reaction>();

    public Post(String author, String title, String content) {
        if (author == null || author.length() == 0 || title == null || title.length() == 0 || content == null
                || content.length() == 0) {
            throw new IllegalArgumentException();
        }
        this.authorUsername = author;
        this.title = title;
        this.content = content;
        this.id = UUID.randomUUID();
    }

    public Post(
            @JsonProperty("title") String title,
            @JsonProperty("content") String content) {
        this.title = title;
        this.content = content;
        this.id = UUID.randomUUID();

    }

    public UUID getId() {
        return this.id;
    }

    public void setAuthor(String username) {
        this.authorUsername = username;
    }

    public String getAuthor() {
        return this.authorUsername;
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
}
