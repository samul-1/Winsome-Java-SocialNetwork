package protocol;

import java.util.Map;

public class RestRequest {
    private final String path;
    private final HttpMethod method;
    private final Map<String, String> headers;
    private final String body;

    RestRequest(String path, HttpMethod method) {
        this.path = path;
        this.method = method;
        this.params = null;
    }

    public static RestRequest parseRequestLine(String reqLine) {
        /**
         * Takes in a string containing an HTTP request line;
         * returns a RestRequest object containing the parsed
         * elements of the request string
         * 
         */
        if (reqLine == null) {
            throw new IllegalArgumentException("Request line cannot be null");
        }
        String[] reqTokens = reqLine.split(" ");
        if (reqTokens.length != 3) {
            throw new IllegalArgumentException("Invalid HTTP request line");
        }
        HttpMethod method = HttpMethod.valueOf(reqTokens[0]);
        String path = reqTokens[1];

        // TODO get params

        return new RestRequest(path, method);
    }

    public String getPath() {
        return this.path;
    }

    public HttpMethod getMethod() {
        return this.method;
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    public String getBody() {
        return this.body;
    }

}
