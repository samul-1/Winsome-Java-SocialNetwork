package exceptions;

import protocol.RestRequest;
import protocol.RestResponse;

public class ClientOperationFailedException extends Exception {
    private RestRequest request;
    private RestResponse response;

    public ClientOperationFailedException(RestRequest request, RestResponse response) {
        this(request, response, null);
    }

    public ClientOperationFailedException(RestRequest request, RestResponse response, String message) {
        super(message);
        this.request = request;
        this.response = response;
    }

    public ClientOperationFailedException(String message) {
        super(message);
    }

    public RestRequest getRequest() {
        return this.request;
    }

    public RestResponse getResponse() {
        return this.response;
    }
}
