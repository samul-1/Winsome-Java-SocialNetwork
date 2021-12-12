package entities;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

    public Set<String> getTags() {
        return this.tags;
    }

    @JsonIgnore
    public Password getPassword() {
        return this.password;
    }

    public boolean isCompatibleWith(User user) {
        for (String tag : user.getTags()) {
            if (this.tags.contains(tag)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.username.hashCode();
    }
}
