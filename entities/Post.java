package entities;

import java.util.UUID;

public class Post {
    private final UUID id;
    private final String authorUsername;
    private final String title;
    private final String content;

    Post(String author, String title, String content) {
        if (author == null || author.length() == 0 || title == null || title.length() == 0 || content == null
                || content.length() == 0) {
            throw new IllegalArgumentException();
        }
        this.authorUsername = author;
        this.title = title;
        this.content = content;
        this.id = UUID.randomUUID();
    }

    public String getId() {
        return this.id.toString();
    }
}
