package com.posthog.java;

import com.posthog.java.flags.*;
import com.posthog.java.flags.hash.Hasher;
import org.json.JSONObject;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.*;

public class FeatureFlagPoller {
    private final String projectApiKey;
    private final String personalApiKey;
    private final CountDownLatch initialLoadLatch;
    private final Duration featureFlagPollingInterval;
    private final ScheduledExecutorService executor;
    private final Getter getter;
    private volatile List<FeatureFlag> featureFlags = new ArrayList<>();
    private volatile Map<String, FeatureFlagPropertyGroup> cohorts = new HashMap<>();
    private volatile Map<String, String> groups = new HashMap<>();

    private FeatureFlagPoller(Builder builder) {
        this.executor = builder.executor;
        this.projectApiKey = builder.projectApiKey;
        this.personalApiKey = builder.personalApiKey;
        this.initialLoadLatch = new CountDownLatch(1);
        this.featureFlagPollingInterval = builder.featureFlagPollingInterval;
        this.getter = builder.getter;
    }

    public static class Builder {
        private final String projectApiKey;
        private final String personalApiKey;
        private final Getter getter;

        private Duration featureFlagPollingInterval = Duration.ofSeconds(300);
        private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

        public Builder(String projectApiKey, String personalApiKey, Getter getter) {
            this.projectApiKey = projectApiKey;
            this.personalApiKey = personalApiKey;
            this.getter = getter;
        }

        public Builder featureFlagPollingInterval(Duration featureFlagPollingInterval) {
            this.featureFlagPollingInterval = featureFlagPollingInterval;
            return this;
        }

        public Builder executor(ScheduledExecutorService executor) {
            this.executor = executor;
            return this;
        }

        public FeatureFlagPoller build() {
            return new FeatureFlagPoller(this);
        }
    }

    /**
     * Polls the PostHog API for feature flags at the specified interval.
     * The feature flags are stored in memory and can be accessed using the other methods in this class.
     * This method will block until the initial load of feature flags is complete.
     */
    public void poll() {
        this.executor.scheduleAtFixedRate(() -> {
            this.fetchFeatureFlags();
            this.initialLoadLatch.countDown();
        }, 0, this.featureFlagPollingInterval.getSeconds(), SECONDS);
    }

    private void fetchFeatureFlags() {
        final String url = String.format("/v1/api/feature_flag/local_evaluation?token=%s&send_cohorts=true", this.projectApiKey);

        final Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + this.personalApiKey);
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        headers.put("User-Agent", "PostHog-Java/1.0.0");

        final JSONObject jsonResponse = getter.get(url, headers);
        if (jsonResponse == null) {
            System.err.println("Failed to fetch feature flags: response is null");
            return;
        }

        final FeatureFlags featureFlags = FeatureFlagParser.parse(jsonResponse);
        this.featureFlags = featureFlags.getFlags();
        this.cohorts = featureFlags.getCohorts();
        this.groups = featureFlags.getGroupTypeMapping();
    }

    /**
     * Shuts down the executor service.
     */
    public void shutdown() {
        executor.shutdownNow();
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
     *
     * @return boolean indicating whether the feature flag is enabled
     */
    public boolean isFeatureFlagEnabled(FeatureFlagConfig config) {
        final Optional<FeatureFlag> featureFlag = getFeatureFlag(config);
        return featureFlag.map(flag -> {
            try {
                return computeFlagLocally(flag, config, this.cohorts).isPresent();
            } catch (InconclusiveMatchException e) {
                System.err.println("Error computing flag locally: " + e.getMessage());
                return false;
            }
        }).orElse(false);
    }

    /**
     * @param key String
     *            key of the feature flag
     * @param distinctId String
     *                   distinctId of the user
     * @return boolean indicating whether the feature flag is enabled
     */
    public boolean isFeatureFlagEnabled(String key, String distinctId) {
        final FeatureFlagConfig config = new FeatureFlagConfig.Builder(key, distinctId).build();
        final Optional<FeatureFlag> featureFlag = getFeatureFlag(config);
        return featureFlag.map(flag -> {
            try {
                return computeFlagLocally(flag, config, this.cohorts).isPresent();
            } catch (InconclusiveMatchException e) {
                System.err.println("Error computing flag locally: " + e.getMessage());
                return false;
            }
        }).orElse(false);
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
     *
     * @return Optional<String> variant key of the feature flag
     */
    public Optional<String> getFeatureFlagVariant(FeatureFlagConfig config) {
        final Optional<FeatureFlag> featureFlag = getFeatureFlag(config);
        return featureFlag.flatMap(flag -> getMatchingVariant(flag, config.getDistinctId()).map(FeatureFlagVariantMeta::getKey));
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
     * @return Optional<FeatureFlag> feature flag
     */
    public Optional<FeatureFlag> getFeatureFlag(FeatureFlagConfig config) {
        final Optional<FeatureFlag> featureFlag = getFeatureFlags().stream()
                .filter(flag -> flag.getKey().equals(config.getKey()))
                .findFirst();

        if (!featureFlag.isPresent()) {
            return Optional.empty();
        }

        try {
            final Optional<String> computedFlag = computeFlagLocally(featureFlag.get(), config, this.cohorts);
            if (computedFlag.isPresent()) {
                return featureFlag;
            }
        } catch (InconclusiveMatchException e) {
            System.err.println("Error computing flag locally: " + e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * If the feature flags have not been loaded, this method will block until they are loaded.
     *
     * @return List<FeatureFlag> feature flags
     */
    public List<FeatureFlag> getFeatureFlags() {
        try {
            this.initialLoadLatch.await();
            if (this.featureFlags.isEmpty()) {
                System.err.println("No feature flags loaded");
                return new ArrayList<>();
            }
        } catch (InterruptedException e) {
            System.err.println("Error waiting for initial load: " + e.getMessage());
        }
        return featureFlags;
    }

    private Optional<String> computeFlagLocally(
            FeatureFlag flag,
            FeatureFlagConfig config,
            Map<String, FeatureFlagPropertyGroup> cohorts
    ) throws InconclusiveMatchException {
        if (flag.isEnsureExperienceContinuity()) {
            throw new InconclusiveMatchException("Flag has experience continuity enabled");
        }

        if (!flag.isActive()) {
            return Optional.empty();
        }

        final int aggregationIndex = flag.getFilter()
                .map(FeatureFlagFilter::getAggregationGroupTypeIndex)
                .orElse(0);

        if (aggregationIndex > 0) {
            final String groupName = groups.get(String.valueOf(aggregationIndex));
            if (groupName == null) {
                throw new InconclusiveMatchException("Flag has unknown group type index");
            }

            final Map<String, Map<String, String>> groupProperties = addLocalGroupProperties(config.getGroupProperties(), groups);
            return matchFeatureFlagProperties(flag, config.getDistinctId(), groupProperties.get(groupName), cohorts);
        }

        final Map<String, String> personProperties = addLocalPersonProperties(config.getPersonProperties(), config.getDistinctId());
        return matchFeatureFlagProperties(flag, config.getDistinctId(), personProperties, cohorts);
    }

    private Map<String, Map<String, String>> addLocalGroupProperties(final Map<String, Map<String, String>> properties, final Map<String, String> groups) {
        return groups.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            String groupName = entry.getKey();
                            Map<String, String> groupProps = new HashMap<>();
                            groupProps.put("$group_key", entry.getValue());
                            if (properties.containsKey(groupName)) {
                                groupProps.putAll(properties.get(groupName));
                            }
                            return groupProps;
                        }
                ));
    }

    private Map<String, String> addLocalPersonProperties(Map<String, String> properties, String distinctId) {
        final Map<String, String> localProperties = properties.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
        localProperties.put("distinct_id", distinctId);
        return localProperties;
    }

    private Optional<String> matchFeatureFlagProperties(
            FeatureFlag featureFlag,
            String distinctId,
            Map<String, String> properties,
            Map<String, FeatureFlagPropertyGroup> cohorts
    ) throws InconclusiveMatchException {

        final List<FeatureFlagCondition> conditions = featureFlag.getFilter()
                .map(FeatureFlagFilter::getGroups)
                .orElse(new ArrayList<>());

        final List<FeatureFlagCondition> sortedConditions = conditions.stream()
                .sorted(Comparator.comparingInt(a -> a.getVariant().isPresent() ? -1 : 1))
                .collect(Collectors.toList());

        boolean isInconclusive = false;
        for (FeatureFlagCondition condition : sortedConditions) {
            boolean isMatch;

            try {
                isMatch = isConditionMatch(featureFlag, distinctId, condition, properties, cohorts);
            } catch (InconclusiveMatchException e) {
                isInconclusive = true;
                continue;
            }

            if (isMatch) {
                if (condition.getVariant().isPresent()) {
                    return condition.getVariant();
                }

                return getMatchingVariant(featureFlag, distinctId)
                        .map(FeatureFlagVariantMeta::getKey);
            }
        }

        if (isInconclusive) {
            throw new InconclusiveMatchException("Error matching conditions");
        }

        return Optional.empty();
    }

    private boolean isConditionMatch(
            FeatureFlag featureFlag,
            String distinctId,
            FeatureFlagCondition condition,
            Map<String, String> properties,
            Map<String, FeatureFlagPropertyGroup> cohorts
    ) throws InconclusiveMatchException {

        for (FeatureFlagProperty property : condition.getProperties()) {
            boolean matches;
            if (property.isCohort()) {
                matches = matchCohort(property, properties, cohorts);
            } else {
                matches = matchProperty(property, properties);
            }

            if (!matches) {
                return false;
            }
        }

        return condition.getRolloutPercentage() == 0 ||
                isSimpleFlagEnabled(featureFlag.getKey(), distinctId, condition.getRolloutPercentage());
    }

    private boolean matchCohort(
            FeatureFlagProperty property,
            Map<String, String> properties,
            Map<String, FeatureFlagPropertyGroup> cohorts
    ) throws InconclusiveMatchException {

        final FeatureFlagPropertyGroup cohort = cohorts.get(property.getKey());

        if (cohort == null) {
            throw new InconclusiveMatchException("Cohort not found");
        }

        return matchPropertyGroup(cohort, properties);
    }

    private boolean matchPropertyGroup(
            FeatureFlagPropertyGroup featureFlagPropertyGroup,
            Map<String, String> properties
    ) throws InconclusiveMatchException {

        if (featureFlagPropertyGroup.getValues().isEmpty()) {
            return true;
        }

        boolean errorMatchingLocally = false;

        for (Object value : featureFlagPropertyGroup.getValues()) {
            boolean matches;

            if (value instanceof FeatureFlagPropertyGroup) {
                try {
                    matches = matchPropertyGroup((FeatureFlagPropertyGroup) value, properties);
                } catch (InconclusiveMatchException e) {
                    errorMatchingLocally = true;
                    continue;
                }
            } else {
                final FeatureFlagProperty flagProperty = (FeatureFlagProperty) value;
                try {
                    matches = matchProperty(flagProperty, properties);
                } catch (InconclusiveMatchException e) {
                    errorMatchingLocally = true;
                    continue;
                }
            }

            final String propertyGroupType = featureFlagPropertyGroup.getType();
            if (propertyGroupType.equals("AND") && !matches) {
                return false;
            } else if (propertyGroupType.equals("OR") && matches) {
                return true;
            }
        }

        if (errorMatchingLocally) {
            throw new InconclusiveMatchException("Error matching property group");
        }

        return featureFlagPropertyGroup.getType().equals("AND");
    }

    private boolean matchProperty(
            FeatureFlagProperty property,
            Map<String, String> properties
    ) throws InconclusiveMatchException {
        final Optional<String> overrideValue = Optional.ofNullable(properties.get(property.getKey()));
        final List<String> propertyValue = property.getValue();

        return propertyValue.stream()
                .anyMatch(eachPropertyValue ->
                        property.getOperator()
                                .map(operator -> {
                                    switch (operator) {
                                        case EXACT:
                                            final boolean result = overrideValue
                                                    .map(value -> value.equals(eachPropertyValue))
                                                    .orElse(false);
                                            return result;
                                        case IS_NOT:
                                            return overrideValue
                                                    .map(value -> !value.equals(eachPropertyValue))
                                                    .orElse(false);
                                        case IS_SET:
                                            return overrideValue.isPresent();
                                        case CONTAINS_INSENSITIVE:
                                            return overrideValue.map(value -> value.toString().toLowerCase())
                                                    .map(value -> value.contains(eachPropertyValue.toLowerCase()))
                                                    .orElse(false);

                                        case NOT_CONTAINS_INSENSITIVE:
                                            return overrideValue.map(value -> value.toString().toLowerCase())
                                                    .map(value -> !value.contains(eachPropertyValue.toLowerCase()))
                                                    .orElse(false);
                                        case REGEX:
                                            return overrideValue.map(value -> overrideValue.toString())
                                                    .map(value -> Pattern.compile(eachPropertyValue).matcher(value).find())
                                                    .orElse(false);
                                        case NOT_REGEX:
                                            return overrideValue.map(value -> overrideValue.toString())
                                                    .map(value -> !Pattern.compile(eachPropertyValue).matcher(value).find())
                                                    .orElse(false);
                                        case GREATER_THAN:
                                            return overrideValue.map(value -> Double.parseDouble(value.toString()))
                                                    .orElse(0.0) > Double.parseDouble(eachPropertyValue);
                                        case LESS_THAN:
                                            return overrideValue
                                                    .map(value -> Double.parseDouble(value.toString()))
                                                    .orElse(0.0) < Double.parseDouble(eachPropertyValue);
                                        case GREATER_THAN_OR_EQUAL:
                                            return overrideValue
                                                    .map(value -> Double.parseDouble(value.toString()))
                                                    .orElse(0.0) >= Double.parseDouble(eachPropertyValue);
                                        case LESS_THAN_OR_EQUAL:
                                            return overrideValue
                                                    .map(value -> Double.parseDouble(value.toString()))
                                                    .orElse(0.0) <= Double.parseDouble(eachPropertyValue);
                                    }
                                    return false;
                                })
                                .orElse(false));
    }

    private Optional<FeatureFlagVariantMeta> getMatchingVariant(FeatureFlag featureFlag, String distinctId) {
        final List<FeatureFlagVariantMeta> lookupTable = getVariantLookupTable(featureFlag);

        double flagHash = Hasher.hash(featureFlag.getKey(), distinctId, "variant");

        return lookupTable.stream()
                .filter(variantMeta -> flagHash >= variantMeta.getValueMin() && flagHash < variantMeta.getValueMax())
                .findFirst();
    }

    private List<FeatureFlagVariantMeta> getVariantLookupTable(FeatureFlag flag) {
        final List<FeatureFlagVariant> variants = flag.getFilter()
                .flatMap(FeatureFlagFilter::getMultivariate)
                .map(FeatureFlagVariants::getVariants)
                .orElse(new ArrayList<>());

        double valueMin = 0.0;

        List<FeatureFlagVariantMeta> lookupTable = new ArrayList<>();

        for (FeatureFlagVariant variant : variants) {
            final double valueMax = valueMin + (double) variant.getRolloutPercentage() / 100;
            final FeatureFlagVariantMeta variantMeta = new FeatureFlagVariantMeta.Builder(variant.getKey())
                    .valueMin(valueMin)
                    .valueMax(valueMax)
                    .build();
            lookupTable.add(variantMeta);
            valueMin = valueMax;
        }

        return lookupTable;
    }

    private boolean isSimpleFlagEnabled(String key, String distinctId, int rolloutPercentage) {
        return Hasher.hash(key, distinctId, "") < (double) rolloutPercentage / 100;
    }
}
