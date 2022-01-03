package services;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import protocol.HttpMethod;
import protocol.RestRequest;
import protocol.RestResponse;

public class WalletConversionService {
    private double cachedRate = 1.0;
    private Date circuitBreakerTriggerTimestamp = null;
    private boolean circuitBreakerOn = false;

    private final int CIRCUIT_BREAKER_COOL_DOWN = 10 * 1000;
    private final int MAX_RETRIES = 5;
    private final int RETRY_BACK_OFF = 500;
    private final int BUF_CAPACITY = 4096 * 4096;

    public double getConversionRate() {
        synchronized (this) {
            if (this.circuitBreakerOn) {
                if (this.hasCoolDownExpired()) {
                    this.circuitBreakerOn = false;
                } else {
                    // circuit breaker is on because the server has been down recently
                    // return cached value instead of repeatedly contacting the remote
                    // service, which is likely still down
                    return this.cachedRate;
                }
            }
        }

        int triesCount = 0;
        double value = this.cachedRate;

        while (triesCount < this.MAX_RETRIES) {
            try {
                value = this.requestRate();
                break;
            } catch (IOException e) {
                triesCount += 1;
                e.printStackTrace();
                System.out.println("TRYING AGAIN...");
            }

            // try again after a bit
            try {
                Thread.sleep(this.RETRY_BACK_OFF);
            } catch (InterruptedException e) {
                ;
            }
        }

        synchronized (this) {
            if (triesCount == this.MAX_RETRIES) {
                this.triggerCircuitBreaker();
            } else {
                this.cachedRate = value;
            }
        }

        return value;
    }

    private boolean hasCoolDownExpired() {
        return (new Date().getTime()
                - this.circuitBreakerTriggerTimestamp
                        .getTime()) >= this.CIRCUIT_BREAKER_COOL_DOWN;
    }

    private double requestRate() throws IOException {
        URL url = new URL("https://api.random.org/json-rpc/4/invoke");
        String postBody = "{\n" +
                "    \"jsonrpc\": \"2.0\",\n" +
                "    \"method\": \"generateDecimalFractions\",\n" +
                "    \"params\": {\n" +
                "        \"apiKey\": \"e536fefe-1137-4c02-88ac-49c10a8f1064\",\n" +
                "       \"n\": 1,\n" +
                "       \"decimalPlaces\": 2\n" +
                "   },\n" +
                " \"id\": 123\n" +
                "}";
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("content-type", "application/json");
        connection.setRequestProperty("content-length", Integer.toString(postBody.length()));
        connection.setDoOutput(true);
        try (DataOutputStream writer = new DataOutputStream(connection.getOutputStream())) {
            writer.write(postBody.getBytes());
        }
        // byte[] buf = new byte[this.BUF_CAPACITY];
        // try (DataInputStream reader = new
        // DataInputStream(connection.getInputStream())) {
        // reader.readFully(buf);
        // }

        Reader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
        System.out.println("RESPONSE FROM RANDOM\n\n");
        StringBuilder sb = new StringBuilder();
        for (int c; (c = in.read()) >= 0;)
            sb.append((char) c);

        String generatedNumberData = sb.toString().split("\"data\":")[1].substring(1, 5);

        // RestResponse response = RestResponse.fromString(sb.toString());
        // if (response.isClientErrorResponse() || response.isServerErrorResponse()) {
        // throw new IOException();
        // }
        // String generatedNumberData =
        // response.getBody().split("\"data\":")[1].substring(1, 5);
        // RestRequest request = new RestRequest("/json-rpc/4/invoke",
        // HttpMethod.POST,
        // null,
        // "{\n" +
        // " \"jsonrpc\": \"2.0\",\n" +
        // " \"method\": \"generateDecimalFractions\",\n" +
        // " \"params\": {\n" +
        // " \"apiKey\": \"e536fefe-1137-4c02-88ac-49c10a8f1064\",\n" +
        // " \"n\": 1,\n" +
        // " \"decimalPlaces\": 2\n" +
        // " },\n" +
        // " \"id\": 123\n" +
        // "}");
        // InetAddress addr = InetAddress.getByName("api.random.org");
        // SocketAddress sktAddr = new InetSocketAddress(addr, 443);
        // SocketChannel sktChan = SocketChannel.open(sktAddr);

        // System.out.println("REQUEST TO RANDOM: " + request.toString());
        // sktChan.write(ByteBuffer.wrap(request.toString().getBytes()));

        // ByteBuffer buf = ByteBuffer.allocate(this.BUF_CAPACITY);
        // sktChan.read(buf);
        // buf.flip();

        // String responseString = StandardCharsets.UTF_8.decode(buf).toString();
        // System.out.println("RESPONSE:\n" + responseString);
        // RestResponse response = RestResponse.fromString(responseString);

        // System.out.println("RESPONSE FROM RANDOM: " + response.toString());

        // sktChan.close();
        System.out.println("GENERATED " + generatedNumberData);
        return Double.parseDouble(generatedNumberData);
    }

    private void triggerCircuitBreaker() {
        if (!this.circuitBreakerOn) {
            this.circuitBreakerOn = true;
            this.circuitBreakerTriggerTimestamp = new Date();
        }
    }
}
