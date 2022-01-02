package auth;

import java.io.Serializable;

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
        return password; // TODO implement
    }

    public boolean equals(Password obj) {
        return this.getPassword().equals(obj.getPassword());
    }
}
