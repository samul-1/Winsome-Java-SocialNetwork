package client;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

import entities.User;

public interface IClientFollowerNotificationService extends Remote {
    public void updateFollowerList(Set<User> followers) throws RemoteException;
}
