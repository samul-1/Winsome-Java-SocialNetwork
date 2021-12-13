package entities;

public class Reaction {
    private String voterUsername;
    private final int value;

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

    // this.voterUsername = username;
    // this.value = value;
    // }

    public void setUser(String username) {
        this.voterUsername = username;
    }
}
