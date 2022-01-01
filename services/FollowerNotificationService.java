package services;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.concurrent.ConcurrentLinkedQueue;

import auth.AuthenticationToken;
import client.IClientFollowerNotificationService;
import entities.User;

public class FollowerNotificationService extends RemoteServer implements FollowerNotificationServiceInterface {
    private final DataStoreService store;

    public FollowerNotificationService(DataStoreService store) {
        this.store = store;
    }

    @Override
    public void subscribe(IClientFollowerNotificationService client, AuthenticationToken token) throws RemoteException {
        if (token == null || client == null) {
            throw new IllegalArgumentException();
        }

        User requestingUser = this.store.getUser(token);
        System.out.println("!!! Token" + token);
        // TODO this is returning null
        if (requestingUser == null) {
            throw new IllegalArgumentException();
        }

        this.store.setUserCallbackReference(requestingUser.getUsername(), client);
    }

    public void notifyUser(String username) {
        IClientFollowerNotificationService callbackRef = this.store.getUserCallbackReference(username);
        try {
            callbackRef.updateFollowerList(this.store.getUserFollowers(username));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}
