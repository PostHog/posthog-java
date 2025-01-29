package com.posthog.java;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

public class PostHog {
    private QueueManager queueManager;
    private Thread queueManagerThread;
    private Sender sender;
    private PostHogLogger logger; 

    private static abstract class BuilderBase {
        protected QueueManager queueManager;
        protected Sender sender;
        protected PostHogLogger logger; 
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

        public Builder logger(PostHogLogger logger) {
            this.logger = logger;
            return this;
        }

        public PostHog build() {
            if (this.logger == null) {
                this.logger = new DefaultPostHogLogger();
            }
            
            this.sender = new HttpSender.Builder(apiKey).host(host).logger(logger).build();
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
            if (this.logger == null) {
                this.logger = new DefaultPostHogLogger();
            }
            return new PostHog(this);
        }
    }

    private PostHog(BuilderBase builder) {
        this.queueManager = builder.queueManager;
        this.sender = builder.sender;
        this.logger = builder.logger;
        startQueueManager();
    }

    public void shutdown() {
        queueManager.stop();
        try {
            queueManagerThread.join(); // wait for the current items in queue to be sent
        } catch (InterruptedException e) {
            logger.error("Error shutting down PostHog", e);
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
            eventJson.put("$lib", "posthog-java");
            if (properties != null) {
                eventJson.put("properties", properties);
            }
        } catch (JSONException e) {
            logger.error("Error creating event JSON", e);
        }
        return eventJson;
    }

    /**
     * 
     * @param featureFlag which uniquely identifies your feature flag
     *
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     * 
     * @return           whether the feature flag is enabled or not
     */
    public boolean isFeatureFlagEnabled(String featureFlag, String distinctId) {
        if (getFeatureFlags(distinctId).get(featureFlag) == null)
            return false;
        return Boolean.parseBoolean(getFeatureFlags(distinctId).get(featureFlag));
    }

    /**
     * 
     * @param featureFlag which uniquely identifies your feature flag
     *
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     * 
     * @return           Variant of the feature flag
     */
    public String getFeatureFlag(String featureFlag, String distinctId) {
        return getFeatureFlags(distinctId).get(featureFlag);
    }

    /**
     * 
     * @param featureFlag which uniquely identifies your feature flag
     *
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     * 
     * @return           The feature flag payload, if it exists
     */
    public String getFeatureFlagPayload(String featureFlag, String distinctId) {
        return getFeatureFlagPayloads(distinctId).get(featureFlag);
    }

    private HashMap<String, String> getFeatureFlags(String distinctId) {
        JSONObject response = sender.post("/decide/?v=3", distinctId);

        HashMap<String, String> featureFlags = new HashMap<>();

        JSONObject flags = response.getJSONObject("featureFlags");
        for (String flag : flags.keySet()) {
            featureFlags.put(flag, flags.get(flag).toString());
        }

        return featureFlags;
    }

    private HashMap<String, String> getFeatureFlagPayloads(String distinctId) {
        JSONObject response = sender.post("/decide/?v=3", distinctId);

        HashMap<String, String> flagPayloads = new HashMap<>();

        JSONObject payloads = response.getJSONObject("featureFlagPayloads");
        for (String flag : payloads.keySet()) {
            String payload = payloads.get(flag).toString();
            flagPayloads.put(flag, payload);
        }

        return flagPayloads;
    }
}
