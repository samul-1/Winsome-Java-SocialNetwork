package services;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

public class WalletConversionService {
    private double cachedRate = 1.0;
    private Date circuitBreakerTriggerTimestamp = null;
    private boolean circuitBreakerOn = false;

    private final int CIRCUIT_BREAKER_COOL_DOWN = 10 * 1000;
    private final int MAX_RETRIES = 5;
    private final int RETRY_BACK_OFF = 500;
    private final int DECIMAL_PLACES = 2;

    public double getConversionRate() {
        synchronized (this) {
            if (this.circuitBreakerOn) {
                if (this.hasCoolDownExpired()) {
                    this.circuitBreakerOn = false;
                } else {
                    // circuit breaker is on because the remote service has been down
                    // recently - return cached value instead of repeatedly contacting
                    // the remote service, which is likely still down
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
                "       \"decimalPlaces\": " + this.DECIMAL_PLACES + "\n" +
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

        StringBuilder sb = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
            for (int c; (c = reader.read()) >= 0;)
                sb.append((char) c);
        }

        if (sb.toString().indexOf("error") != -1 || sb.toString().indexOf("data") == -1) {
            // unsuccessful interaction with remote service
            throw new IOException();
        }

        String generatedNumberData = sb
                .toString()
                .split("\"data\":")[1] // generated number immediately follows this string
                        .trim()
                        .substring(
                                1, // discard leading '['
                                this.DECIMAL_PLACES + 3 // leading digit + '.'
                        );

        return Double.parseDouble(generatedNumberData);
    }

    private void triggerCircuitBreaker() {
        if (!this.circuitBreakerOn) {
            this.circuitBreakerOn = true;
            this.circuitBreakerTriggerTimestamp = new Date();
        }
    }
}
