package entities;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import auth.Password;

@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class User implements Serializable {
    private final String username;
    private final Set<String> tags;
    private final Password password;

    public User(@JsonProperty("username") String username,
            @JsonProperty("tags") Set<String> tags) {
        this(username, tags, null);
    }

    public User(String username, Set<String> tags, Password password) {
        if (username == null || username.trim().length() == 0
                || tags == null || tags.size() == 0 || tags.size() > 5) {
            throw new IllegalArgumentException();
        }
        this.username = username;
        Set<String> lowercaseTags = tags
                .stream()
                .map(tag -> tag.toLowerCase())
                .collect(Collectors.toSet());
        this.tags = lowercaseTags;
        this.password = password;
    }

    public String getUsername() {
        return this.username;
    }

    public Set<String> getTags() {
        return this.tags;
    }

    public Password getPassword() {
        return this.password;
    }

    public boolean isCompatibleWith(User anotherUser) {
        for (String tag : anotherUser.getTags()) {
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
