package auth;

public class AuthenticationToken {
    private final String token;

    AuthenticationToken() {
        this.token = this.getRandomToken();
    }

    private String getRandomToken() {
        return "aaa"; // TODO implement
    }

    public String getToken() {
        return this.token;
    }
}
