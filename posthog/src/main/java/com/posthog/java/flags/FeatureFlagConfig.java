package com.posthog.java.flags;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public class FeatureFlagConfig {
    private final String key;
    private final String distinctId;
    private final Map<String, Object> groups;
    private final Map<String, String> personProperties;
    private final Map<String, Map<String, String>> groupProperties;

    private FeatureFlagConfig(Builder builder) {
        this.key = builder.key;
        this.distinctId = builder.distinctId;
        this.groups = builder.groups;
        this.personProperties = builder.personProperties;
        this.groupProperties = builder.groupProperties;
    }

    public static class Builder {
        private final String key;
        private final String distinctId;

        private Map<String, Object> groups = new HashMap<>();
        private Map<String, String> personProperties = new HashMap<>();
        private Map<String, Map<String, String>> groupProperties = new HashMap<>();

        public Builder(String key, String distinctId) {
            this.key = key;
            this.distinctId = distinctId;
        }

        public Builder groups(Map<String, Object> groups) {
            this.groups = groups;
            return this;
        }

        public Builder personProperties(Map<String, String> personProperties) {
            this.personProperties = personProperties;
            return this;
        }

        public Builder groupProperties(Map<String, Map<String, String>> groupProperties) {
            this.groupProperties = groupProperties;
            return this;
        }

        public FeatureFlagConfig build() {
            return new FeatureFlagConfig(this);
        }
    }

    public String getKey() {
        return key;
    }

    public String getDistinctId() {
        return distinctId;
    }

    public Map<String, Object> getGroups() {
        return groups;
    }

    public Map<String, String> getPersonProperties() {
        return personProperties;
    }

    public Map<String, Map<String, String>> getGroupProperties() {
        return groupProperties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeatureFlagConfig that = (FeatureFlagConfig) o;
        return Objects.equals(key, that.key) && Objects.equals(distinctId, that.distinctId) && Objects.equals(groups, that.groups) && Objects.equals(personProperties, that.personProperties) && Objects.equals(groupProperties, that.groupProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, distinctId, groups, personProperties, groupProperties);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FeatureFlagConfig.class.getSimpleName() + "[", "]")
                .add("key='" + key + "'")
                .add("distinctId='" + distinctId + "'")
                .add("groups=" + groups)
                .add("personProperties=" + personProperties)
                .add("groupProperties=" + groupProperties)
                .toString();
    }
}
