package com.posthog.java;

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

    private Duration retryInterval;

    public static class Builder {
        // required
        private final String apiKey;

        // optional
        private String host = "https://app.posthog.com";

        // optional
        private int maxRetries = 5;

        // optional
        private Duration retryInterval = Duration.ofMillis(500);

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

        public Builder retryInterval(Duration retryInterval) {
            this.retryInterval = retryInterval;
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
        this.retryInterval = builder.retryInterval;
        this.client = new OkHttpClient();
    }

    public void send(List<JSONObject> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        String json = getRequestBody(events);
        Response response = null;
        boolean retry = true;
        int retries = 0;

        while(retry) {
            try {
                MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                RequestBody body = RequestBody.create(json, JSON);
                Request request = new Request.Builder().url(host + "/batch").post(body).build();
                Call call = client.newCall(request);

                // must always close an OkHTTP response
                // https://square.github.io/okhttp/4.x/okhttp/okhttp3/-call/execute/
                response = client.newCall(request).execute();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (response != null) {
                    if (response.isSuccessful() || retries >= maxRetries) {
                        retry = false;
                    } else {
                        retries += 1;

                        try {
                            Thread.sleep(retryInterval.toMillis());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    response.close();
                }
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
