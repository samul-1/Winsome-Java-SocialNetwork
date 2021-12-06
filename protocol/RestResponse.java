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

    private String getVerboseCode() {
        return null;
    }

    public String toString() {
        return "HTTP/1.1 " + this.getVerboseCode() + "\r\n\r\n" + this.body + "\r\n\r\n";
    }

}
