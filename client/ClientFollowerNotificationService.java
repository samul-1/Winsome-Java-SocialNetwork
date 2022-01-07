package client;

import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.HashSet;
import java.util.Set;

import entities.User;

public class ClientFollowerNotificationService extends RemoteObject implements IClientFollowerNotificationService {
    private final Set<User> localRef;

    public ClientFollowerNotificationService(Set<User> localRef) {
        this.localRef = localRef;
    }

    @Override
    public void updateFollowerList(Set<User> followers) throws RemoteException {
        this.localRef.clear();
        this.localRef.addAll(followers);
    }

}
