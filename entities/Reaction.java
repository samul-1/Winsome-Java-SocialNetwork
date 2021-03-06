package entities;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)

public class Reaction implements Comparable<Reaction> {
    private String username;
    private int value;
    private final Date timestamp = new Date();

    @JsonCreator
    public Reaction() {
    }

    public Reaction(int value) {
        // this(null, value);
        if (value != 1 && value != -1) {
            throw new IllegalArgumentException("Vote value must either be 1 or -1");
        }

        this.value = value;
    }

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

    @Override
    public int compareTo(Reaction o) {
        return this.timestamp.compareTo(o.getTimestamp());
    }
}
