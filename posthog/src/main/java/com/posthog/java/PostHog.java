package com.posthog.java;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

public class PostHog {
    private QueueManager queueManager;
    private Thread queueManagerThread;
    private Sender sender;
    private HashMap<String, HashMap<String, String>> featureFlags;
    private Date lastFeatureFlagUpdate;
    private static int updateIntervalInMinutes = 5;

    private static abstract class BuilderBase {
        protected QueueManager queueManager;
        protected Sender sender;
    }

    public static class Builder extends BuilderBase {
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

        public PostHog build() {
            this.sender = new HttpSender.Builder(apiKey).host(host).build();
            this.queueManager = new QueueManager.Builder(this.sender).build();
            return new PostHog(this);
        }
    }

    public static class BuilderWithCustomQueueManager extends BuilderBase {

        public BuilderWithCustomQueueManager(QueueManager queueManager, Sender... sender) {
            this.queueManager = queueManager;
            if (sender.length > 0)
                this.sender = sender[0];
        }

        public PostHog build() {
            return new PostHog(this);
        }
    }

    private PostHog(BuilderBase builder) {
        this.queueManager = builder.queueManager;
        this.sender = builder.sender;
        this.featureFlags = new HashMap<>();
        startQueueManager();
    }

    public void shutdown() {
        queueManager.stop();
        try {
            queueManagerThread.join(); // wait for the current items in queue to be sent
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void startQueueManager() {
        queueManagerThread = new Thread(queueManager, "PostHog QueueManager thread");
        queueManagerThread.start();
        // TODO handle interrupts? (via addShutdownHook)
    }

    private void enqueue(String distinctId, String event, Map<String, Object> properties) {
        JSONObject eventJson = getEventJson(event, distinctId, properties);
        queueManager.add(eventJson);
    }

    /**
     * 
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     * @param event      name of the event. Must not be null or empty.
     * @param properties an array with any event properties you'd like to set.
     */
    public void capture(String distinctId, String event, Map<String, Object> properties) {
        enqueue(distinctId, event, properties);
    }

    /**
     * 
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     * @param event      name of the event. Must not be null or empty.
     */
    public void capture(String distinctId, String event) {
        enqueue(distinctId, event, null);
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
    public void identify(String distinctId, Map<String, Object> properties, Map<String, Object> propertiesSetOnce) {
        Map<String, Object> props = new HashMap<String, Object>();
        if (properties != null) {
            props.put("$set", properties);
        }
        if (propertiesSetOnce != null) {
            props.put("$set_once", propertiesSetOnce);
        }
        enqueue(distinctId, "$identify", props);
    }

    /**
     * 
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     * @param properties an array with any person properties you'd like to set.
     */
    public void identify(String distinctId, Map<String, Object> properties) {
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
        Map<String, Object> props = new HashMap<String, Object>() {
            {
                put("distinct_id", distinctId);
                put("alias", alias);
            }
        };
        enqueue(distinctId, "$create_alias", props);
    }

    /**
     * 
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     * @param properties an array with any person properties you'd like to set.
     */
    public void set(String distinctId, Map<String, Object> properties) {
        Map<String, Object> props = new HashMap<String, Object>() {
            {
                put("$set", properties);
            }
        };
        enqueue(distinctId, "$set", props);
    }

    /**
     * 
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     * @param properties an array with any person properties you'd like to set.
     *                   Previous values will not be overwritten.
     */
    public void setOnce(String distinctId, Map<String, Object> properties) {
        Map<String, Object> props = new HashMap<String, Object>() {
            {
                put("$set_once", properties);
            }
        };
        enqueue(distinctId, "$set_once", props);
    }

    private JSONObject getEventJson(String event, String distinctId, Map<String, Object> properties) {
        JSONObject eventJson = new JSONObject();
        try {
            // Ensure that we generate an identifier for this event such that we can e.g.
            // deduplicate server-side any duplicates we may receive.
            eventJson.put("uuid", UUID.randomUUID().toString());
            eventJson.put("timestamp", Instant.now().toString());
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

    /**
     * 
     * @param featureFlag which uniquely identifies your feature flag
     *
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     */
    public boolean isFeatureFlagEnabled(String featureFlag, String distinctId) {
        return getFeatureFlags(distinctId).get(featureFlag) != null;
    }

    /**
     * 
     * @param featureFlag which uniquely identifies your feature flag
     *
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     */
    public String getFeatureFlag(String featureFlag, String distinctId) {
        return getFeatureFlags(distinctId).get(featureFlag);
    }

    private HashMap<String, String> getFeatureFlags(String distinctId) {
        Date fiveMinAgo = new Date(
            Calendar.getInstance().getTimeInMillis() - (PostHog.updateIntervalInMinutes * 60 * 1000)
        );
        if (this.lastFeatureFlagUpdate.before(fiveMinAgo))
            return updateFeatureFlags(distinctId);
        HashMap<String, String> distinctFeatureFlags = featureFlags.get(distinctId);
        if (distinctFeatureFlags == null)
            return updateFeatureFlags(distinctId);

        return distinctFeatureFlags;
    }

    private HashMap<String, String> updateFeatureFlags(String distinctId) {
        JSONObject response = sender.post("/decide/?v=3", distinctId);

        HashMap<String, String> distinctFeatureFlags = new HashMap<>();

        JSONObject flags = response.getJSONObject("featureFlags");
        JSONObject payloads = response.getJSONObject("featureFlagPayloads");
        for (String flag : flags.keySet()) {
            String payload = flags.get(flag).toString();
            if (payloads.has(flag)) 
                payload = payloads.getString(flag);
            distinctFeatureFlags.put(flag, payload);
        }
        this.featureFlags.put(distinctId, distinctFeatureFlags);

        this.lastFeatureFlagUpdate = new Date();

        return distinctFeatureFlags;
    }
}
