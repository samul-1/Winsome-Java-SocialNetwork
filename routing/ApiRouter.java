package routing;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import protocol.RestRequest;
import services.SocialNetworkService;
import exceptions.MethodNotSupportedException;
import exceptions.RouteNotFoundException;

public class ApiRouter {
    Set<ApiRoute> routes = new HashSet<ApiRoute>();

    public ApiRouter(ApiRoute[] routes) {
        this.routes = new HashSet<ApiRoute>(Arrays.asList(routes));
    }

    private ApiRoute resolveRoute(String requestPath) {
        return this.routes
                .stream()
                .filter(route -> requestPath.equals(route.getPath()))
                .findFirst()
                .orElse(null);
    }

    public Method getRequestHandler(RestRequest request) throws MethodNotSupportedException, RouteNotFoundException {
        /**
         * Takes in a RestRequest object and uses it to look up the name of
         * the handler method to execute based on the requested path and http
         * method.
         * 
         * Upon success, returns the Method that SocialNetworkService
         * will call to handle the client's request.
         * 
         * Throws RouteNotFoundException if no route in the router matches that of
         * the request; throws MethodNotSupportedException if the matched route
         * doesn't have a handler for the request http method.
         * 
         */
        Class<?> serviceClass = SocialNetworkService.class;

        ApiRoute matchingRoute = this.resolveRoute(request.getPath());

        if (matchingRoute == null) {
            throw new RouteNotFoundException();
        }

        Method handler = null;
        try {
            // find handler method name based on the HTTP method of the request
            handler = serviceClass.getMethod(matchingRoute.getMethodAction(request.getMethod()));
        } catch (NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
            assert false; // this is never supposed to happen
        }

        if (handler == null) {
            // matched route doesn't support the requested HTTP method
            throw new MethodNotSupportedException();
        }
        return handler;
    }
}
