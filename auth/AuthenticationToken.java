package auth;

import java.io.Serializable;
import java.security.SecureRandom;

import com.fasterxml.jackson.annotation.JsonValue;

public class AuthenticationToken implements Serializable {
    private final String token;

    private final int TOKEN_LENGTH = 128;

    public AuthenticationToken(String token) {
        this.token = token;
    }

    public AuthenticationToken() {
        this.token = this.getRandomToken();
    }

    private String getRandomToken() {
        final String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        final SecureRandom rand = new SecureRandom();
        final StringBuilder stringBuilder = new StringBuilder(this.TOKEN_LENGTH);
        for (int i = 0; i < this.TOKEN_LENGTH; i++) {
            stringBuilder.append(alphabet.charAt(rand.nextInt(alphabet.length())));
        }
        return stringBuilder.toString();
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

    @Override
    @JsonValue
    public String toString() {
        return "Token: " + this.token;
    }
}
