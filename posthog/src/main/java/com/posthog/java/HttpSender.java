package com.posthog.java;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpSender implements Sender {
    private String apiKey;
    private String host;
    private OkHttpClient client;
    private int maxRetries;
    private Duration initialRetryInterval;

    public static class Builder {
        // required
        private final String apiKey;

        // optional
        private String host = "https://app.posthog.com";

        // optional
        private int maxRetries = 3;

        // optional
        private Duration initialRetryInterval = Duration.ofMillis(500);

        public Builder(String apiKey) {
            this.apiKey = apiKey;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder initialRetryInterval(Duration initialRetryInterval) {
            this.initialRetryInterval = initialRetryInterval;
            return this;
        }

        public HttpSender build() {
            return new HttpSender(this);
        }
    }

    private HttpSender(Builder builder) {
        this.apiKey = builder.apiKey;
        this.host = builder.host;
        this.maxRetries = builder.maxRetries;
        this.initialRetryInterval = builder.initialRetryInterval;
        this.client = new OkHttpClient();
    }

    public Boolean send(List<JSONObject> events) {
        if (events == null || events.isEmpty()) {
            return null;
        }
        String json = getRequestBody(events);
        Response response = null;
        int retries = 0;

        while (true) {
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(json, JSON);
            Request request = new Request.Builder().url(host + "/batch").post(body).build();
            Call call = client.newCall(request);

            try {
                response = call.execute();

                if (response.isSuccessful()) {
                    // On 2xx status codes, the request was successful so we return and assume
                    // events have been successfully ingested by PostHog.
                    return true;
                }

                // On 4xx status codes, the request was unsuccessful so we
                // return and assume events have not been ingested by PostHog.
                if (response.code() >= 400 && response.code() < 500) {
                    return false;
                }
            } catch (IOException e) {
                // TODO: verify if we need to retry on IOException, this may
                // already be handled by OkHTTP. See
                // https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/-builder/retry-on-connection-failure/
                e.printStackTrace();
            } finally {
                // must always close an OkHTTP response
                // https://square.github.io/okhttp/4.x/okhttp/okhttp3/-call/execute/
                if (response != null) {
                    response.close();
                }
            }

            retries += 1;

            if (retries > maxRetries) {
                return false;
            }

            try {
                // TODO: use the Retry-After header if present to determine the
                // retry interval.
                // For now we use a fixed initial retry interval, falling back
                // exponentially.
                Thread.sleep(initialRetryInterval.toMillis() * (long) Math.pow(3, retries));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String getRequestBody(List<JSONObject> events) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("api_key", apiKey);
            jsonObject.put("batch", events);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }

}
