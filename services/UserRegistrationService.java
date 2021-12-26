package services;

import java.rmi.RemoteException;
import java.util.Set;

import auth.Password;
import entities.User;
import protocol.RestResponse;

public class UserRegistrationService implements UserRegistrationInterface {
    private DataStoreService store;

    public UserRegistrationService(DataStoreService store) {
        this.store = store;
    }

    @Override
    public RestResponse registerUserHandler(String username, String password, Set<String> tags)
            throws RemoteException {
        if (username.trim().length() == 0 || password.length() == 0 || tags.size() > 5 || tags.size() == 0) {
            return new RestResponse(400);
        }
        User newUser = this.store.registerUser(username, tags, new Password(password));
        if (newUser == null) {
            // username already taken
            return new RestResponse(403);
        }
        return new RestResponse(201, new Serializer<User>().serialize(newUser));
    }
}
