package routing;

import java.io.File;
import java.util.Set;

import protocol.RestRequest;
import exceptions.MethodNotSupportedException;
import exceptions.RouteNotFoundException;

public class ApiRouter {
    Set<ApiRoute> routes;

    ApiRouter(File routes) {
        // TODO de-serialize routes file
    }

    public String resolveRoute(RestRequest request) throws MethodNotSupportedException, RouteNotFoundException {
        /**
         * Takes in a RestRequest object and uses it to look up the name of
         * the handler method to execute based on the requested path and http
         * method.
         * 
         * Upon success, returns the *name of a method* that the SocialNetworkService
         * can call to handle the client's request.
         * 
         * Throws RouteNotFoundException if no route in the router matches that of
         * the request; throws MethodNotSupportedException if the matched route
         * doesn't have a handler for the request http method.
         * 
         */
        return null;
    }
}
