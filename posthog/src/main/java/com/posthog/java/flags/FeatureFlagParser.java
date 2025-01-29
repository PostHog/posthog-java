package com.posthog.java.flags;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class FeatureFlagParser {

    public static FeatureFlags parse(JSONObject responseRaw) {
        final List<FeatureFlag> flags = StreamSupport.stream(responseRaw.optJSONArray("flags").spliterator(), false)
                .map(JSONObject.class::cast)
                .map(FeatureFlagParser::parseFeatureFlag)
                .collect(Collectors.toList());

        return new FeatureFlags.Builder()
                .flags(flags)
                .groupTypeMapping(parseGroupTypeMapping(responseRaw.optJSONObject("group_type_mapping")))
                .cohorts(parseCohorts(responseRaw.optJSONObject("cohorts")))
                .build();
    }

    private static FeatureFlag parseFeatureFlag(JSONObject featureFlagRaw) throws JSONException {
        if (featureFlagRaw == null)
            return null;

        final String key = featureFlagRaw.getString("key");
        final int id = featureFlagRaw.getInt("id");
        final int teamId = featureFlagRaw.getInt("team_id");

        return new FeatureFlag.Builder(key, id, teamId)
                .isSimpleFlag(featureFlagRaw.optBoolean("is_simple_flag"))
                .rolloutPercentage(featureFlagRaw.optInt("rollout_percentage"))
                .active(featureFlagRaw.optBoolean("active"))
                .filter(parseFilter(featureFlagRaw.optJSONObject("filters")))
                .ensureExperienceContinuity(featureFlagRaw.optBoolean("ensure_experience_continuity"))
                .build();
    }

    private static FeatureFlagFilter parseFilter(JSONObject filterRaw) throws JSONException {
        if (filterRaw == null)
            return null;

        return new FeatureFlagFilter.Builder()
                .aggregationGroupTypeIndex(filterRaw.optInt("aggregation_group_type_index"))
                .groups(parseFeatureFlagConditions(filterRaw.optJSONArray("groups")))
                .multivariate(parseVariants(filterRaw.optJSONObject("multivariate")))
                .payloads(filterRaw.optJSONObject("payloads").toMap())
                .build();
    }

    private static List<FeatureFlagCondition> parseFeatureFlagConditions(JSONArray conditionsRaw) throws JSONException {
        if (conditionsRaw == null)
            return new ArrayList<>();

        return StreamSupport.stream(conditionsRaw.spliterator(), false)
                .map(JSONObject.class::cast)
                .map(conditionRaw -> {
                    final List<FeatureFlagProperty> properties = StreamSupport.stream(conditionRaw.getJSONArray("properties").spliterator(), false)
                            .map(JSONObject.class::cast)
                            .map(FeatureFlagParser::parseFlagProperty)
                            .collect(Collectors.toList());
                    return parseFeatureFlagCondition(properties, conditionRaw);
                })
                .collect(Collectors.toList());
    }

    private static FeatureFlagProperty parseFlagProperty(JSONObject flagPropertyRaw) throws JSONException {
        if (flagPropertyRaw == null)
            return null;

        final List<String> value = flagPropertyRaw.optJSONArray("value") != null
                ? StreamSupport.stream(flagPropertyRaw.getJSONArray("value").spliterator(), false)
                    .map(Object::toString)
                    .collect(Collectors.toList())
                : Collections.singletonList(flagPropertyRaw.optString("value"));

        return new FeatureFlagProperty.Builder(flagPropertyRaw.optString("key"))
                .negation(flagPropertyRaw.optBoolean("negation", false))
                .value(value)
                .operator(flagPropertyRaw.optString("operator"))
                .type(flagPropertyRaw.optString("type"))
                .build();
    }

    private static FeatureFlagCondition parseFeatureFlagCondition(final List<FeatureFlagProperty> properties, JSONObject conditionRaw) throws JSONException {
        if (conditionRaw == null)
            return null;

        return new FeatureFlagCondition.Builder()
                .properties(properties)
                .rolloutPercentage(conditionRaw.optInt("rollout_percentage"))
                .variant(conditionRaw.optString("variant"))
                .build();
    }

    private static FeatureFlagVariants parseVariants(JSONObject variantsRaw) throws JSONException {
        if (variantsRaw == null)
            return null;

        return StreamSupport.stream(variantsRaw.getJSONArray("variants").spliterator(), false)
                .map(JSONObject.class::cast)
                .map(FeatureFlagParser::parseFlagVariant)
                .collect(Collectors.collectingAndThen(Collectors.toList(), variants -> new FeatureFlagVariants.Builder().variants(variants).build()));
    }

    private static FeatureFlagVariant parseFlagVariant(JSONObject flagVariantRaw) throws JSONException {
        if (flagVariantRaw == null)
            return null;

        return new FeatureFlagVariant.Builder(flagVariantRaw.getString("key"), flagVariantRaw.getString("name"))
                .rolloutPercentage(flagVariantRaw.optInt("rollout_percentage"))
                .build();
    }

    private static Map<String, FeatureFlagPropertyGroup> parseCohorts(JSONObject cohortsRaw) throws JSONException {
        if (cohortsRaw == null)
            return new HashMap<>();

        return cohortsRaw.keySet()
                .stream()
                .collect(Collectors.toMap(key -> key, key -> parsePropertyGroup(cohortsRaw.getJSONObject(key))));
    }

    private static FeatureFlagPropertyGroup parsePropertyGroup(JSONObject propertyGroupRaw) throws JSONException {
        final List<Object> values = new ArrayList<>();
        final JSONArray valuesJson = propertyGroupRaw.getJSONArray("values");

        for (int i = 0; i < valuesJson.length(); i++) {
            Object value = valuesJson.get(i);
            if (value instanceof JSONObject) {
                final JSONObject possibleChild = (JSONObject) value;
                if (possibleChild.has("type")) {
                    values.add(parsePropertyGroup(possibleChild));
                }
            } else {
                values.add(value);
            }
        }

        return new FeatureFlagPropertyGroup.Builder()
                .type(propertyGroupRaw.optString("type"))
                .values(values)
                .build();
    }

    private static Map<String, String> parseGroupTypeMapping(JSONObject groupTypeMappingRaw) throws JSONException {
        if (groupTypeMappingRaw == null)
            return new HashMap<>();

        return groupTypeMappingRaw.keySet()
                .stream()
                .collect(Collectors.toMap(key -> key, groupTypeMappingRaw::getString));
    }
}
