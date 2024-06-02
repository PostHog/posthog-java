package com.posthog.java.flags;

import java.util.*;

public class FeatureFlagCondition {
    private final List<FeatureFlagProperty> properties;
    private final int rolloutPercentage;
    private final String variant;

    private FeatureFlagCondition(final Builder builder) {
        this.properties = builder.properties;
        this.rolloutPercentage = builder.rolloutPercentage;
        this.variant = builder.variant;
    }

    public static class Builder {
        private List<FeatureFlagProperty> properties = new ArrayList<>();
        private int rolloutPercentage = 0;
        private String variant = "";

        public Builder properties(List<FeatureFlagProperty> properties) {
            this.properties = properties;
            return this;
        }

        public Builder rolloutPercentage(int rolloutPercentage) {
            this.rolloutPercentage = rolloutPercentage;
            return this;
        }

        public Builder variant(String variant) {
            this.variant = variant;
            return this;
        }

        public FeatureFlagCondition build() {
            return new FeatureFlagCondition(this);
        }
    }

    public List<FeatureFlagProperty> getProperties() {
        return properties;
    }

    public int getRolloutPercentage() {
        return rolloutPercentage;
    }

    public Optional<String> getVariant() {
        return Optional.ofNullable(variant);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeatureFlagCondition that = (FeatureFlagCondition) o;
        return getRolloutPercentage() == that.getRolloutPercentage() && Objects.equals(getProperties(), that.getProperties()) && Objects.equals(getVariant(), that.getVariant());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getProperties(), getRolloutPercentage(), getVariant());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FeatureFlagCondition.class.getSimpleName() + "[", "]")
                .add("properties=" + properties)
                .add("rolloutPercentage=" + rolloutPercentage)
                .add("variant='" + variant + "'")
                .toString();
    }
}