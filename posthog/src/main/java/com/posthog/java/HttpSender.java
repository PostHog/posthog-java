package com.posthog.java;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class HttpSender implements Sender {
    private String apiKey;
    private String host;
    private OkHttpClient client;

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

        try {
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(json, JSON);
            Request request = new Request.Builder().url(host + "/batch").post(body).build();
            Call call = client.newCall(request);
            call.execute();
        } catch (Exception e) {
            e.printStackTrace();
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
