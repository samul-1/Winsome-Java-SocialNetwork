package protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RestResponse implements Serializable {
    private final int code;
    private final String body;
    private final Map<String, String> headers;
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
        this(code, new HashMap<String, String>(), "");
    }

    public RestResponse(int code, String body) {
        this(code, new HashMap<String, String>(), body);
    }

    public RestResponse(int code, Map<String, String> headers, String body) {
        this.headers = headers;
        this.code = code;
        this.body = body;

        // add default headers
        this.headers.put("content-type", "application/json");
        this.headers.put("content-length", String.valueOf(this.body.length()));

        // for usage inside of browser
        this.headers.put("Access-Control-Allow-Origin", "*");
        this.headers.put("Access-Control-Allow-Methods", "*");
        this.headers.put("Access-Control-Allow-Headers", "*");
    }

    public int getCode() {
        return this.code;
    }

    public String getBody() {
        return this.body;
    }

    public static RestResponse fromString(String source) throws IOException {
        try (BufferedReader reader = new BufferedReader(new StringReader(source))) {
            String responseLine = reader.readLine();
            if (!responseLine.substring(0, 9).equals("HTTP/1.1 ")) {
                System.out.println(responseLine.substring(0, 8) + "!");
                throw new IllegalArgumentException("Invalid response line format " + responseLine);
            }

            // trim leading "HTTP/1.1 " and tailing "\r\n"
            String verboseResponseCode = responseLine.substring("HTTP/1.1 ".length(), responseLine.length())
                    .toUpperCase();
            Integer code = RestResponse.verboseCodes
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue()
                            .equals(verboseResponseCode))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
            if (code == null) {
                throw new IllegalArgumentException("Invalid response line " + responseLine);
            }

            Map<String, String> headers = new HashMap<String, String>();
            String body = "";

            String header = reader.readLine();
            while (header.length() > 0) { // read up to "\r\n"
                String[] tokens = header.split(": ");
                headers.put(tokens[0], tokens[1]); // set header
                header = reader.readLine();
            }

            String bodyLine = reader.readLine();
            while (bodyLine != null) { // read until the end of the response string
                body += "\n" + bodyLine;
                bodyLine = reader.readLine();
            }
            if (body.length() > 0) {
                body = body.substring(1); // remove preceding '\n'
            }
            return new RestResponse(code, headers, body);
        }
    }

    private String getVerboseCode() {
        return RestResponse.verboseCodes.get(this.code);
    }

    public boolean isSuccessResponse() {
        return (this.code / 100) == 2;
    }

    public boolean isClientErrorResponse() {
        return (this.code / 100) == 4;
    }

    public boolean isServerErrorResponse() {
        return (this.code / 100) == 5;
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
