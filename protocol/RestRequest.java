package protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RestRequest {
    private final String path;
    private final UUID pathParameter;
    private final HttpMethod method;
    private final Map<String, String> headers;
    private final String body;

    private final String URI_PARAMETER_TOKEN = "<id>";

    private RestRequest(String path, HttpMethod method, Map<String, String> headers, String body) {
        String[] pathAndParameter = this.getParsedRequestPath(path);

        this.path = pathAndParameter[0];
        this.pathParameter = pathAndParameter[1] == null ? null : UUID.fromString(pathAndParameter[1]);
        this.method = method;
        this.headers = headers;
        this.body = body;
    }

    private String[] getParsedRequestPath(String requestPath) {
        /**
         * Takes in a String representing a REST path and parses it looking for
         * a UUID parameter in the URI.
         * 
         * If the path doesn't contain UUID parameters,for example: `/posts`, the first
         * element of the returned pair is the passed String unchanged and the
         * second element is null.
         * 
         * If the path contains a UUID, for example:
         * `/posts/123e4567-e89b-12d3-a456-426614174000/comments`,
         * the first element of the returned pair is the URI with a symbolic token
         * in place of the parameter: `/posts/<id>/comments`, and the second element
         * is the parameter `123e4567-e89b-12d3-a456-426614174000`.
         * 
         * *NB* this assumes that the path will contain AT MOST ONE parameter
         * (it's a compromise but works fine here because no route in the project
         * ever uses more than one parameter, and this function could easily be
         * extended to accept an arbitrary number of parameters in the URI) and
         * 
         */

        String[] pair = new String[2];
        pair[1] = null;

        String[] tokens = requestPath.split("/");

        String constructedString = "";
        for (String token : tokens) {
            constructedString += "/";

            try {
                // test token to see if it's a valid UUID
                UUID.fromString(token);
                // found a UUID parameter (otherwise exception would've been raised)
                pair[1] = token;
                // insert token in place of UUID
                constructedString += this.URI_PARAMETER_TOKEN;
            } catch (IllegalArgumentException e) {
                // exception raised when trying to convert token to a UUID,
                // meaning this token is not a UUID parameter
                constructedString += token;
            }
        }

        pair[0] = constructedString.substring(1); // remove the extra `/` at the beginning
        return pair;
    }

    public static RestRequest parseRequestString(String request) throws IOException {
        /**
         * Takes in a string containing an HTTP request; returns a RestRequest
         * object containing the parsed elements of the request string
         * 
         */
        try (BufferedReader reader = new BufferedReader(new StringReader(request))) {
            System.out.println("REQ: " + request);

            String reqLine = reader.readLine(); // first line contains HTTP method and URI
            System.out.println("REQLINE: " + reqLine);
            String[] reqTokens = reqLine.split(" ");
            if (reqTokens.length != 3) {
                throw new IllegalArgumentException("Invalid HTTP request line");
            }
            HttpMethod method = HttpMethod.valueOf(reqTokens[0]);
            String path = reqTokens[1];

            Map<String, String> headers = new HashMap<String, String>();
            String body = "";

            String header = reader.readLine();
            while (header.length() > 0) { // read up to "\r\n"
                String[] tokens = header.split(": ");
                headers.put(tokens[0], tokens[1]); // set header
                header = reader.readLine();
            }

            String bodyLine = reader.readLine();
            while (bodyLine != null) { // read until the end of the request string
                body += bodyLine;
            }
            return new RestRequest(path, method, headers, body);
        }
    }

    public String getPath() {
        return this.path;
    }

    public UUID getPathParameter() {
        return this.pathParameter;
    }

    public HttpMethod getMethod() {
        return this.method;
    }

    public String getHeader(String header) {
        return this.headers.get(header);
    }

    public String getBody() {
        return this.body;
    }

    public boolean isLoginRequest() {
        /**
         * Convenience method to check if the requested path is "login"
         * 
         */
        return this.path.equals("login");
    }

    @Override
    public String toString() {
        String ret = this.method.name() + " " + this.path + "\r\n";
        for (Map.Entry<String, String> entry : this.headers.entrySet()) {
            ret += entry.getKey() + ": " + entry.getValue() + "\r\n";
        }
        ret += "\r\n" + this.body + "\r\n\r\n";
        return ret;
    }
}
