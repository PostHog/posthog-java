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

public class Sender {
    private String apiKey;
    private String host;
    private OkHttpClient client;

    public Sender(String apiKey, String host) {
        this.apiKey = apiKey;
        this.host = host;
        client = new OkHttpClient();
    }

    public void send(List<JSONObject> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        String json = getRequestBody(events);
        System.out.println(json);

        try {
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(json, JSON);
            Request request = new Request.Builder().url(host + "/batch").post(body).build();
            System.out.println(request);
            Call call = client.newCall(request);
            Response response = call.execute();
            System.out.println(response.body().string());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
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
