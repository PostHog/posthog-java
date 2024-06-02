package com.posthog.java.flags;

import java.util.*;

public class FeatureFlags {
    private final List<FeatureFlag> flags;
    private final Map<String, String> groupTypeMapping;
    private final Map<String, FeatureFlagPropertyGroup> cohorts;

    private FeatureFlags(final Builder builder) {
        this.flags = builder.flags;
        this.groupTypeMapping = builder.groupTypeMapping;
        this.cohorts = builder.cohorts;
    }

    public static class Builder {
        private List<FeatureFlag> flags = new ArrayList<>();
        private Map<String, String> groupTypeMapping = new HashMap<>();
        private Map<String, FeatureFlagPropertyGroup> cohorts = new HashMap<>();

        public Builder flags(List<FeatureFlag> flags) {
            this.flags = flags;
            return this;
        }

        public Builder groupTypeMapping(Map<String, String> groupTypeMapping) {
            this.groupTypeMapping = groupTypeMapping;
            return this;
        }

        public Builder cohorts(Map<String, FeatureFlagPropertyGroup> cohorts) {
            this.cohorts = cohorts;
            return this;
        }

        public FeatureFlags build() {
            return new FeatureFlags(this);
        }
    }

    public List<FeatureFlag> getFlags() {
        return flags;
    }

    public Map<String, String> getGroupTypeMapping() {
        return groupTypeMapping;
    }

    public Map<String, FeatureFlagPropertyGroup> getCohorts() {
        return cohorts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeatureFlags that = (FeatureFlags) o;
        return Objects.equals(getFlags(), that.getFlags()) && Objects.equals(getGroupTypeMapping(), that.getGroupTypeMapping()) && Objects.equals(getCohorts(), that.getCohorts());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFlags(), getGroupTypeMapping(), getCohorts());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FeatureFlags.class.getSimpleName() + "[", "]")
                .add("flags=" + flags)
                .add("groupTypeMapping=" + groupTypeMapping)
                .add("cohorts=" + cohorts)
                .toString();
    }
}
