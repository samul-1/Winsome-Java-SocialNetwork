package client;

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
        String ret = "USERS\n";

        for (User user : data) {
            ret += this.render(user) + "\n";
        }

        return ret.substring(0, ret.length() - 1);
    }

}
