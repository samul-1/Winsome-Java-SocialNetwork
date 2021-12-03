package routing;

import protocol.HttpMethod;
import java.util.Map;

public class ApiRoute {
    private final String path;
    private Map<HttpMethod, String> actions;

    ApiRoute(String path, Map<HttpMethod, String> actions) {
        this.path = path;
        this.actions = actions;
    }

    public String getPath() {
        return this.path;
    }

    public String getMethodAction(HttpMethod method) {
        return this.actions.get(method);
    }
}