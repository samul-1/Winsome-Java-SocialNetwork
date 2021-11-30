package routing;

import protocol.HttpMethod;
import java.util.Map;

public class ApiRoute {
    private final String path;
    private Map<HttpMethod, String> actions;
}