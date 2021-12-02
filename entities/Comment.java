package entities;

public class Comment {
    private final String authorUsername;
    private final String content;

    public Comment(String username, String content) {
        // TODO validate input
        this.authorUsername = username;
        this.content = content;
    }
}
