package tests;

import protocol.HttpMethod;
import protocol.RestRequest;

public class ProtocolTestCase {
    public static void main(String[] args) {
        String req1 = "GET posts\r\n\r\n\r\n\r\n\r\n";
        String req2 = "POST posts\r\n\r\n\r\nabc\r\n\r\n";
        String req3 = "DELETE posts/22\r\n\r\n\r\n\r\n\r\n";

        RestRequest req1Parsed = RestRequest.parseRequestLine(req1);
        assert req1Parsed.getMethod() == HttpMethod.GET;
        assert req1Parsed.getPath() == "posts";
        assert req1Parsed.getBody() == "";

        RestRequest req2Parsed = RestRequest.parseRequestLine(req2);
        assert req2Parsed.getMethod() == HttpMethod.POST;
        assert req2Parsed.getPath() == "posts";
        assert req2Parsed.getBody() == "abc";

        RestRequest req3Parsed = RestRequest.parseRequestLine(req3);
        assert req3Parsed.getMethod() == HttpMethod.DELETE;
        assert req3Parsed.getPath() == "posts/22";
        assert req3Parsed.getBody() == "";

    }
}
