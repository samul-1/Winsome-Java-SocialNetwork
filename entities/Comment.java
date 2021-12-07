package entities;

public class Comment {
    private String authorUsername;
    private final String content;

    public Comment(String username, String content) {
        // TODO validate input
        this.authorUsername = username;
        this.content = content;
    }

    public Comment(String content) {
        this.content = content;
    }

    public void setUser(String username) {
        this.authorUsername = username;
    }
}
