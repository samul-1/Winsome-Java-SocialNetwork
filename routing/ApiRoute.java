package routing;

import protocol.HttpMethod;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiRoute {
    private final String path;
    private final Map<HttpMethod, String> actions;

    ApiRoute(
            @JsonProperty("path") String path,
            @JsonProperty("actions") Map<HttpMethod, String> actions) {
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