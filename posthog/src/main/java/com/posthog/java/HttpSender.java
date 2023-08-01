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

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(json, JSON);

        while (true) {
            Request request = new Request.Builder().url(host + "/batch").post(body).build();
            Call call = client.newCall(request);

            try {
                response = call.execute();

                if (response.isSuccessful()) {
                    // On 2xx status codes, the request was successful so we return and assume
                    // events have been successfully ingested by PostHog.
                    return true;
                }

                // On 4xx status codes, the request was unsuccessful, so we
                // return and assume events have not been ingested by PostHog.
                if (response.code() >= 400 && response.code() < 500) {
                    // Make sure we log that we are giving up specifically
                    // because of a Http Client error.
                    System.out.println("Giving up on sending events to PostHog because of a HTTP Client error.");
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
                // Make sure to shout very loudly if we have reached the end of
                // our retries and haven't managed to send events.
                System.out.println("Giving up on sending events to PostHog after " + retries + " retries.");
                return false;
            }

            long retryInterval = initialRetryInterval.toMillis() * (long) Math.pow(3, retries);

            // On retries, make sure we log the response code or exception such
            // that people will know if something is up, ensuring we include the
            // retry count and how long we will wait before retrying.
            System.out.println("Retrying sending events to PostHog after " + retries + " retries. Waiting for "
                    + retryInterval + "ms before retrying.");

            try {
                // TODO: use the Retry-After header if present to determine the retry interval.
                // For now we use a fixed initial retry interval, falling back exponentially.
                // TODO this is a blocking sleep, we should use `Future`s here instead
                Thread.sleep(retryInterval);
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

    public JSONObject post(String route, String distinctId) {
        JSONObject bodyJSON = new JSONObject();
        try {
            bodyJSON.put("api_key", apiKey);
            bodyJSON.put("distinct_id", distinctId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Response response = null;

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(bodyJSON.toString(), JSON);
        Request request = new Request.Builder().url(host + route).post(body).build();
        Call call = client.newCall(request);

        try {
            response = call.execute();

            if (response.isSuccessful()) {
                JSONObject responseJSON = new JSONObject(response.body().string());

                return responseJSON;
            }

            if (response.code() >= 400 && response.code() < 500) {
                System.err.println("Error calling API: " + response.body().string());
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return null;
    }
}
