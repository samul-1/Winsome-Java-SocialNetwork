package protocol;

public class RestResponse {
    private final int code;
    private final String body;

    public RestResponse(int code) {
        this.code = code;
        this.body = "";
    }

    public RestResponse(int code, String body) {
        this.code = code;
        this.body = body;
    }

    public String toString() {
        // TODO turn the response into what could be sent to a browser
        return "";
    }

}
