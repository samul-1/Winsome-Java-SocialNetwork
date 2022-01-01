package services;

import java.rmi.Remote;
import java.rmi.RemoteException;

import auth.AuthenticationToken;
import client.IClientFollowerNotificationService;

public interface FollowerNotificationServiceInterface extends Remote {
    public void subscribe(IClientFollowerNotificationService client, AuthenticationToken token) throws RemoteException;
}
