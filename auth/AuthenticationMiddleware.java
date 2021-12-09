package auth;

import entities.User;
import exceptions.InvalidTokenException;
import exceptions.NoAuthenticationProvidedException;
import protocol.AuthenticatedRestRequest;
import protocol.RestRequest;
import services.DataStoreService;

public class AuthenticationMiddleware {
    /**
     * This class acts as a middleware between the API and the underlying
     * SocialNetworkService. It is instantiated and its method `authenticateRequest`
     * is ran before handling each API request (except those necessary to register,
     * log in, or log out a user).
     * 
     * 
     */
    private final DataStoreService store;

    public AuthenticationMiddleware(DataStoreService store) {
        this.store = store;
    }

    public AuthenticatedRestRequest getAnonymousRestRequest(RestRequest request) {
        /**
         * Returns an AuthenticatedRestRequest whose user is `null`
         */
        return new AuthenticatedRestRequest(request, null);
    }

    public AuthenticatedRestRequest authenticateRequest(RestRequest request)
            throws NoAuthenticationProvidedException, InvalidTokenException {
        /**
         * Searches the request headers for the `Authorization` header.
         * If found, looks up the store to see to which user it corresponds,
         * then returns an AuthenticatedRestRequest containing the original
         * request and a reference to the requesting user.
         * 
         * Throws NoAuthenticationProvidedException if the `Authorization` header
         * isn't present.
         * Throws InvalidTokenException if the value of the `Authorization` header
         * isn't a valid token or if it isn't present in the store.
         * 
         */

        String tokenString = request.getHeader("Authorization");
        if (tokenString == null) {
            throw new NoAuthenticationProvidedException();
        }
        AuthenticationToken token = new AuthenticationToken(tokenString.substring("Bearer ".length()));
        System.out.println("Token used is: " + token.getToken());
        User requestingUser = this.store.getUser(token);

        if (requestingUser == null) {
            throw new InvalidTokenException();
        }

        return new AuthenticatedRestRequest(request, requestingUser);
    }
}
