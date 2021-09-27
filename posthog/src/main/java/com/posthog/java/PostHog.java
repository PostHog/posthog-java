package com.posthog.java;

import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import org.json.JSONObject;
import org.json.JSONException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Call;

public class PostHog {
    private String apiKey;
    private String host;

    public static class Builder {
        // required
        private final String apiKey;

        // optional
        // private String host = "https://app.posthog.com";
        private String host = "localhost";

        public Builder(String apiKey) {
            this.apiKey = apiKey;
        }

        public Builder host(String value) {
            host = value;
            return this;
        }

        public PostHog build() {
            return new PostHog(this);
        }
    }

    private PostHog(Builder builder) {
        apiKey = builder.apiKey;
        host = builder.host;
    }

    private void send(String distinctId, String event, HashMap<String, Object> properties) {
        JSONObject eventJson = getEventJson(event, distinctId, properties);
        String json = getRequestBody(Collections.singletonList(eventJson));

        System.out.println(json);

        try {
            OkHttpClient client = new OkHttpClient();

            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(json, JSON);

            Request request = new Request.Builder().url(host + "/batch").post(body).build();
            System.out.println(request);
            Call call = client.newCall(request);
            Response response = call.execute();
            System.out.println(response.body().string());
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * 
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     * @param event      name of the event. Must not be null or empty.
     * @param properties an array with any event properties you'd like to set.
     */
    public void capture(String distinctId, String event, HashMap<String, Object> properties) {
        send(distinctId, event, properties);
    }

    /**
     * 
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     * @param event      name of the event. Must not be null or empty.
     */
    public void capture(String distinctId, String event) {
        send(distinctId, event, null);
    }

    /**
     * 
     * @param distinctId        which uniquely identifies your user in your
     *                          database. Must not be null or empty.
     * @param properties        an array with any person properties you'd like to
     *                          set.
     * @param propertiesSetOnce an array with any person properties you'd like to
     *                          set without overwriting previous values.
     */
    public void identify(String distinctId, HashMap<String, Object> properties,
            HashMap<String, Object> propertiesSetOnce) {
        HashMap<String, Object> props = new HashMap<String, Object>();
        if (properties != null) {
            props.put("$set", properties);
        }
        if (propertiesSetOnce != null) {
            props.put("$set_once", propertiesSetOnce);
        }
        send(distinctId, "$identify", props);
    }

    /**
     * 
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     * @param properties an array with any person properties you'd like to set.
     */
    public void identify(String distinctId, HashMap<String, Object> properties) {
        identify(distinctId, properties, null);
    }

    /**
     * 
     * @param distinctId distinct ID to merge. Must not be null or empty. Note: If
     *                   there is a conflict, the properties of this person will
     *                   take precedence.
     * @param alias      distinct ID to merge. Must not be null or empty. Note: If
     *                   there is a conflict, the properties of this person will be
     *                   overriden.
     */
    public void alias(String distinctId, String alias) {
        HashMap<String, Object> props = new HashMap<String, Object>() {
            {
                put("distinct_id", distinctId);
                put("alias", alias);
            }
        };
        send(distinctId, "$create_alias", props);
    }

    /**
     * 
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     * @param properties an array with any person properties you'd like to set.
     */
    public void set(String distinctId, HashMap<String, Object> properties) {
        HashMap<String, Object> props = new HashMap<String, Object>() {
            {
                put("$set", properties);
            }
        };
        send(distinctId, "$set", props);
    }

    /**
     * 
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     * @param properties an array with any person properties you'd like to set.
     *                   Previous values will not be overwritten.
     */
    public void setOnce(String distinctId, HashMap<String, Object> properties) {
        HashMap<String, Object> props = new HashMap<String, Object>() {
            {
                put("$set_once", properties);
            }
        };
        send(distinctId, "$set_once", props);
    }

    private JSONObject getEventJson(String event, String distinctId, HashMap<String, Object> properties) {
        JSONObject eventJson = new JSONObject();
        try {
            eventJson.put("distinct_id", distinctId);
            eventJson.put("event", event);
            if (properties != null) {
                eventJson.put("properties", properties);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return eventJson;
    }

    private String getRequestBody(List<JSONObject> events) {
        // Note: This is in prep for actually batching requests
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
