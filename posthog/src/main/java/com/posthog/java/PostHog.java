package com.posthog.java;

import java.time.Instant;
import java.util.*;

import com.posthog.java.flags.FeatureFlag;
import com.posthog.java.flags.FeatureFlagConfig;
import org.json.JSONException;
import org.json.JSONObject;

public class PostHog {
    private QueueManager queueManager;
    private Thread queueManagerThread;
    private Sender sender;
    private FeatureFlagPoller featureFlagPoller;

    private Map<String, String> distinctIdsFeatureFlagsReported;

    private static abstract class BuilderBase {
        protected QueueManager queueManager;
        protected Sender sender;
        protected Getter getter;
        protected FeatureFlagPoller featureFlagPoller;
        protected int maxDistinctIdsFeatureFlagsReport = 50_000;
    }

    public static class Builder extends BuilderBase {
        // required
        private final String projectApiKey;
        // optional
        private String host = "https://app.posthog.com";
        private String personalApiKey;

        public Builder(String projectApiKey) {
            this.projectApiKey = projectApiKey;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder personalApiKey(String personalApiKey) {
            this.personalApiKey = personalApiKey;
            return this;
        }

        public Builder maxDistinctIdsFeatureFlagsReport(int maxDistinctIdsFeatureFlagsReport) {
            this.maxDistinctIdsFeatureFlagsReport = maxDistinctIdsFeatureFlagsReport;
            return this;
        }

        public PostHog build() {
            final HttpInteractor httpClient = new HttpInteractor.Builder(projectApiKey)
                    .host(host)
                    .build();

            this.sender = httpClient;

            this.queueManager = new QueueManager.Builder(this.sender).build();

            if (this.personalApiKey != null && !this.personalApiKey.isEmpty()) {
                this.getter = httpClient;
                this.featureFlagPoller = new FeatureFlagPoller.Builder(this.projectApiKey, this.personalApiKey, this.getter)
                        .build();
            }

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

    public static class BuilderWithCustomFeatureFlagPoller extends BuilderBase {

        public BuilderWithCustomFeatureFlagPoller(FeatureFlagPoller featureFlagPoller) {
            this.featureFlagPoller = featureFlagPoller;
        }

        public PostHog build() {
            return new PostHog(this);
        }
    }

    public static class BuilderWithCustomQueueManagerAndCustomFeatureFlagPoller extends BuilderBase {

            public BuilderWithCustomQueueManagerAndCustomFeatureFlagPoller(QueueManager queueManager, FeatureFlagPoller featureFlagPoller, Sender... sender) {
                this.queueManager = queueManager;
                this.featureFlagPoller = featureFlagPoller;
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
        this.featureFlagPoller = builder.featureFlagPoller;
        this.distinctIdsFeatureFlagsReported = Collections.synchronizedMap(new LimitedSizeMap<>(builder.maxDistinctIdsFeatureFlagsReport));

        startQueueManager();

        if (this.featureFlagPoller != null) {
            this.featureFlagPoller.poll();
        }
    }

    public void shutdown() {
        queueManager.stop();
        try {
            queueManagerThread.join(); // wait for the current items in queue to be sent
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (featureFlagPoller != null) {
            featureFlagPoller.shutdown();
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

    public void capture(String distinctId, String event, boolean sendFeatureFlagEvents) {

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
            e.printStackTrace();
        }
        return eventJson;
    }

    /**
     * @param config FeatureFlagConfig
     *               key: String
     *               distinctId: String
     *               groupProperties: Map<String, Map<String, String>>
     *               personProperties: Map<String, String>
     *               groupProperties and personProperties are optional
     *               groupProperties is used for cohort matching
     *               personProperties is used for property matching
     *               key and distinctId must not be null or empty
     *
     * @return whether the feature flag is enabled or not
     */
    public boolean isFeatureFlagEnabled(FeatureFlagConfig config) {
        if (this.featureFlagPoller == null || config == null) {
            return false;
        }

        final boolean isEnabled = this.featureFlagPoller.isFeatureFlagEnabled(config);
        if (config.isSendFeatureFlagEvents()) {
            enqueueFeatureFlagEvent(config.getKey(), config.getDistinctId(), String.valueOf(isEnabled));
        }
        return isEnabled;
    }

    /**
     * The isFeatureFlagEnabled method is used to determine whether a feature flag is enabled for a given user.
     * It will try to use local evaluation first if a personal API key is provided, otherwise it will fallback to the server.
     *
     * @param featureFlag which uniquely identifies your feature flag
     *
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     *
     * @return           whether the feature flag is enabled or not
     */
    public boolean isFeatureFlagEnabled(String featureFlag, String distinctId) {
        if (this.featureFlagPoller == null) {
            if (getFeatureFlags(distinctId).get(featureFlag) == null)
                return false;
            return Boolean.parseBoolean(getFeatureFlags(distinctId).get(featureFlag));
        }

        return this.featureFlagPoller.isFeatureFlagEnabled(featureFlag, distinctId);
    }

    /**
     * @deprecated Use {@link #getFeatureFlagVariant(FeatureFlagConfig)} instead
     *
     * @param featureFlag which uniquely identifies your feature flag
     *
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     *
     * @return           Variant of the feature flag
     */
    public String getFeatureFlag(String featureFlag, String distinctId) {
        if (this.featureFlagPoller == null) {
            return getFeatureFlags(distinctId).get(featureFlag);
        }

        final FeatureFlagConfig featureFlagConfig = new FeatureFlagConfig.Builder(featureFlag, distinctId).build();
        return this.featureFlagPoller.getFeatureFlagVariant(featureFlagConfig)
                .orElse(null);
    }

    /**
     * The getFeatureFlagVariant method is used to determine the variant of a feature flag for a given user.
     * It will try to use local evaluation first if a personal API key is provided, otherwise it will always return an empty Optional.
     *
     * @param config FeatureFlagConfig
     *               key: String
     *               distinctId: String
     *               groupProperties: Map<String, Map<String, String>>
     *               personProperties: Map<String, String>
     *               groupProperties and personProperties are optional
     *               groupProperties is used for cohort matching
     *               personProperties is used for property matching
     *               sendFeatureFlagEvents: boolean
     *               key and distinctId must not be null or empty
     *
     * @return Variant of the feature flag
     */
    public Optional<String> getFeatureFlagVariant(FeatureFlagConfig config) {
        if (this.featureFlagPoller == null || config == null) {
            return Optional.empty();
        }

        final Optional<String> variant = this.featureFlagPoller.getFeatureFlagVariant(config);
        if (config.isSendFeatureFlagEvents()) {
            enqueueFeatureFlagEvent(config.getKey(), config.getDistinctId(), variant.orElse(null));
        }

        return variant;
    }

    /**
     * The getFeatureFlag method is used to determine the payload of a feature flag for a given user.
     * It will try to use local evaluation first if a personal API key is provided, otherwise it will always return an empty Optional.
     *
     * @param config FeatureFlagConfig
     *               key: String
     *               distinctId: String
     *               groupProperties: Map<String, Map<String, String>>
     *               personProperties: Map<String, String>
     *               groupProperties and personProperties are optional
     *               groupProperties is used for cohort matching
     *               personProperties is used for property matching
     *               sendFeatureFlagEvents: boolean
     *               key and distinctId must not be null or empty
     *
     * @return FeatureFlag payload
     */
    public Optional<FeatureFlag> getFeatureFlag(FeatureFlagConfig config) {
        if (this.featureFlagPoller == null || config == null) {
            return Optional.empty();
        }

        return this.featureFlagPoller.getFeatureFlag(config);
    }

    private void enqueueFeatureFlagEvent(String featureFlagKey, String distinctId, String flagValue) {
        if (!this.distinctIdsFeatureFlagsReported.containsKey(distinctId)) {
            final Map<String, Object> properties = new HashMap<>();
            properties.put("$feature_flag", featureFlagKey);
            properties.put("$feature_flag_response", flagValue);
            properties.put("$feature_flag_errored", String.valueOf(flagValue == null));
            enqueue(distinctId, "$feature_flag_called", properties);

            this.distinctIdsFeatureFlagsReported.put(distinctId, featureFlagKey);
        }
    }

    /**
     * @deprecated Use {@link #getFeatureFlag(FeatureFlagConfig)} instead
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
