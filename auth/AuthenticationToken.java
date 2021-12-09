package auth;

public class AuthenticationToken {
    private final String token;

    public AuthenticationToken(String token) {
        // TODO implement
        this.token = token;
    }

    public AuthenticationToken() {
        this.token = this.getRandomToken();
    }

    private String getRandomToken() {
        return "aaa"; // TODO implement
    }

    public String getToken() {
        return this.token;
    }

    @Override
    public boolean equals(Object obj) {
        return this.token.equals(((AuthenticationToken) obj).getToken());
    }

    @Override
    public int hashCode() {
        return this.token.hashCode();
    }
}
