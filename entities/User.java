package entities;

import java.util.Set;

import auth.Password;

public class User {
    private final String username;
    private final Set<String> tags;
    private final Password password;

    public User(String username, Set<String> tags, Password password) {
        // TODO validate input
        this.username = username;
        this.tags = tags;
        this.password = password;
    }

    public String getUsername() {
        return this.username;
    }

    @Override
    public int hashCode() {
        return this.username.hashCode();
    }
}
