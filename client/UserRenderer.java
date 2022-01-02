package client;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import entities.User;

public class UserRenderer implements IRenderer<User> {

    @Override
    public String render(User data) {
        String ret = data.getUsername() + " ";
        for (String tag : data.getTags()) {
            ret += tag + ", ";
        }
        return ret.substring(0, ret.length() - 2);
    }

    @Override
    public String render(User[] data) {
        if (data.length == 0) {
            return "No users to show.";
        }

        int longestUsernameLength = this.getLongestUsernameLength(data);
        int longestTagLength = this.getLongestTagLength(data);

        String ret = "Username";

        for (int i = 0; i < longestUsernameLength - "Username".length(); i++) {
            ret += " ";
        }

        ret += " | Tags\n";

        for (int i = 0; i < longestTagLength + longestUsernameLength + "Username".length() + "Tags".length() + 3; i++) {
            ret += "-";
        }

        ret += "\n";

        for (User user : data) {
            ret += user.getUsername();
            for (int i = 0; i < longestUsernameLength - user.getUsername().length() + 5; i++) {
                ret += " ";
            }
            for (String tag : user.getTags()) {
                ret += tag + ", ";
            }
            ret += "\n";
        }

        return ret.substring(0, ret.length() - 1);
    }

    private int getLongestTagLength(User[] data) {
        return Collections.max(
                Arrays.asList(data)
                        .stream()
                        .map(
                                user -> Collections.max(
                                        user.getTags()
                                                .stream()
                                                .map(tag -> tag.length())
                                                .collect(Collectors
                                                        .toList())))
                        .collect(Collectors.toList()));
    }

    private int getLongestUsernameLength(User[] data) {
        return Collections.max(
                Arrays.asList(data)
                        .stream()
                        .map(user -> user.getUsername().length())
                        .collect(Collectors.toList()));
    }

}
