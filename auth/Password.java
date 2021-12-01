package auth;

public class Password {
    private final String encryptedPassword;

    Password(String password) {
        if (password == null || password.length() == 0) {
            throw new IllegalArgumentException();
        }
        this.encryptedPassword = this.encryptPassword(password);
    }

    public String getPassword() {
        return this.encryptedPassword;
    }

    private String encryptPassword(String password) {
        return password; // TODO implement
    }

    public boolean equals(Password obj) {
        return this.getPassword().equals(obj.getPassword());
    }
}
