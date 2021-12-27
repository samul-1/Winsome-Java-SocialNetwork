package entities;

import java.util.Date;

public class Reaction {
    private String username;
    private final int value;
    private final Date timestamp = new Date();

    public Reaction(int value) {
        // this(null, value);
        if (value != 1 && value != -1) {
            throw new IllegalArgumentException("Vote value must either be 1 or -1");
        }

        this.value = value;
    }

    // public Reaction(String username, int value) {
    // if (username == null || username.length() == 0) {
    // throw new IllegalArgumentException();
    // }
    // if (value != 1 && value != -1) {
    // throw new IllegalArgumentException("Vote value must either be 1 or -1");
    // }

    // this.username = username;
    // this.value = value;
    // }

    public void setUser(String username) {
        this.username = username;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    public int getValue() {
        return this.value;
    }

    public String getUser() {
        return this.username;
    }

}
