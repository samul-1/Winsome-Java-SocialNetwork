package services;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;

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

        if (requestingUser == null) {
            throw new IllegalArgumentException();
        }

        this.store.setUserCallbackReference(requestingUser.getUsername(), client);
    }

    public void notifyUser(String username) {
        System.out.println("about to notify " + username);
        IClientFollowerNotificationService callbackRef = this.store.getUserCallbackReference(username);
        try {
            if (callbackRef != null) {
                callbackRef.updateFollowerList(this.store.getUserFollowers(username));
                System.out.println("notified " + username);
            } else {
                System.out.println("ref for " + username + "is null");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        System.out.println("exiting from notification method for " + username);
    }

}