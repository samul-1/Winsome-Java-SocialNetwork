package client;

import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.Set;

import entities.User;

public class ClientFollowerNotificationService extends RemoteObject implements IClientFollowerNotificationService {

    @Override
    public void updateFollowerList(Set<User> followers) throws RemoteException {
        System.out.println("notification");
    }

}
