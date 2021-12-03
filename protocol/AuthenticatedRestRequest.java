package protocol;

import entities.User;

public class AuthenticatedRestRequest {
    private final RestRequest request;
    private final User user;

    public AuthenticatedRestRequest(RestRequest request, User user) {
        this.request = request;
        this.user = user;
    }

    public RestRequest getRequest() {
        return this.request;
    }

    public User getUser() {
        return this.user;
    }

}
