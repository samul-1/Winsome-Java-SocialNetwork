package protocol;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RestResponse {
    private final int code;
    private final String body;
    private final Map<String, String> headers = new HashMap<>();
    private static final Map<Integer, String> verboseCodes = initMap();

    private static Map<Integer, String> initMap() {
        Map<Integer, String> map = new HashMap<>();
        map.put(200, "200 OK");
        map.put(201, "201 CREATED");
        map.put(204, "204 NO CONTENT");
        map.put(400, "400 BAD REQUEST");
        map.put(401, "401 UNAUTHORIZED");
        map.put(403, "403 FORBIDDEN");
        map.put(404, "404 NOT FOUND");
        map.put(405, "405 METHOD NOT SUPPORTED");
        map.put(500, "500 INTERNAL SERVER ERROR");
        return Collections.unmodifiableMap(map);
    }

    public RestResponse(int code) {
        this(code, "");
    }

    public RestResponse(int code, String body) {
        this.code = code;
        this.body = body;
        this.headers.put("content-type", "application/json");
        this.headers.put("content-length", String.valueOf(this.body.length()));
    }

    private String getVerboseCode() {
        return RestResponse.verboseCodes.get(this.code);
    }

    public String toString() {
        String ret = "HTTP/1.1 " + this.getVerboseCode() + "\r\n";
        for (Map.Entry<String, String> entry : this.headers.entrySet()) {
            ret += entry.getKey() + ": " + entry.getValue() + "\r\n";
        }
        ret += "\r\n" + this.body + "\r\n\r\n";
        return ret;
    }
}
