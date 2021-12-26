package services;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

import exceptions.BadRequestException;
import exceptions.PermissionDeniedException;
import protocol.RestResponse;

public interface UserRegistrationInterface extends Remote {
    public RestResponse registerUserHandler(String username, String password, Set<String> tags)
            throws RemoteException;

}
