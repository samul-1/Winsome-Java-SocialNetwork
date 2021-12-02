package entities;

import java.util.Set;

public class User {
    private final String username;
    private final Set<String> tags;

    @Override
    public int hashCode() {
        return this.username.hashCode();
    }
}
