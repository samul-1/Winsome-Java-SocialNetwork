package entities;

public class Reaction {
    private String voterUsername;
    private final short value;

    public Reaction(short value) {
        this(null, value);
    }

    public Reaction(String username, short value) {
        if (username == null || username.length() == 0) {
            throw new IllegalArgumentException();
        }
        if (value != 1 && value != -1) {
            throw new IllegalArgumentException("Vote value must either be 1 or -1");
        }

        this.voterUsername = username;
        this.value = value;
    }

    public void setUser(String username) {
        this.voterUsername = username;
    }
}
