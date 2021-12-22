package entities;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Comment {
    private String authorUsername;
    private final String content;

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
}
