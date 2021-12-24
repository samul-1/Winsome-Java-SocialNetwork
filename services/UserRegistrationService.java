package services;

import java.rmi.RemoteException;
import java.util.Set;

import auth.Password;
import exceptions.BadRequestException;
import exceptions.PermissionDeniedException;
import protocol.RestResponse;

public class UserRegistrationService implements UserRegistrationInterface {
    private DataStoreService store;

    public UserRegistrationService(DataStoreService store) {
        this.store = store;
    }

    @Override
    public RestResponse registerUserHandler(String username, String password, Set<String> tags)
            throws RemoteException, BadRequestException, PermissionDeniedException {
        if (username.trim().length() == 0 || password.length() == 0 || tags.size() > 5) {
            throw new BadRequestException();
        }
        if (!this.store.registerUser(username, tags, new Password(password))) {
            // username already taken
            throw new PermissionDeniedException();
        }
        return new RestResponse(201);
    }
}
