package com.posthog.java;

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
    private final String apiKey;
    private final String host;
    private final OkHttpClient client;

    public static class Builder {
        // required
        private final String apiKey;

        // optional
        private String host = "https://app.posthog.com";

        public Builder(String apiKey) {
            this.apiKey = apiKey;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public HttpSender build() {
            return new HttpSender(this);
        }
    }

    private HttpSender(Builder builder) {
        this.apiKey = builder.apiKey;
        this.host = builder.host;
        this.client = new OkHttpClient();
    }

    public void send(List<JSONObject> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        String json = getRequestBody(events);

        Response response = null;
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
                response.close();
            }
        }
    }

    private String getRequestBody(List<JSONObject> events) {
        var jsonObject = new JSONObject();
        try {
            jsonObject.put("api_key", apiKey);
            jsonObject.put("batch", events);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }

}
