package entities;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Comment {
    private String authorUsername;
    private final String content;
    private final Date timestamp = new Date();

    @JsonCreator
    public Comment(@JsonProperty("authorUsername") String username, @JsonProperty("content") String content) {
        // TODO validate input
        this.authorUsername = username;
        this.content = content;
    }

    public Comment(@JsonProperty("content") String content) {
        this.content = content;
    }

    public void setUser(String username) {
        this.authorUsername = username;
    }

    public String getUser() {
        return this.authorUsername;
    }

    public String getContent() {
        return this.content;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }
}
