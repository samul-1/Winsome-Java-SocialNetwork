package auth;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)

public class Password implements Serializable {
    private String encryptedPassword;

    @JsonCreator
    public Password() {
    }

    public Password(String password) {
        if (password == null || password.length() == 0) {
            throw new IllegalArgumentException();
        }
        this.encryptedPassword = this.encryptPassword(password);
    }

    public String getPassword() {
        return this.encryptedPassword;
    }

    private String encryptPassword(String password) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            assert false; // never reached
        }

        // use MD5 algorithm to digest passed string
        md.update(password.getBytes());
        byte[] bytes = md.digest();

        StringBuilder sb = new StringBuilder();
        // convert bytes to hex chars
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    public boolean equals(Password obj) {
        return this.getPassword().equals(obj.getPassword());
    }
}
