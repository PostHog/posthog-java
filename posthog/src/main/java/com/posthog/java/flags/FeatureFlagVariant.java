package com.posthog.java.flags;

import java.util.Objects;
import java.util.StringJoiner;

public class FeatureFlagVariant {
    private final String key;
    private final String name;
    private final int rolloutPercentage;

    private FeatureFlagVariant(Builder builder) {
        this.key = builder.key;
        this.name = builder.name;
        this.rolloutPercentage = builder.rolloutPercentage;
    }

    public static class Builder {
        private final String key;
        private final String name;

        private int rolloutPercentage = 0;

        public Builder(String key, String name) {
            this.key = key;
            this.name = name;
        }

        public Builder rolloutPercentage(int rolloutPercentage) {
            this.rolloutPercentage = rolloutPercentage;
            return this;
        }

        public FeatureFlagVariant build() {
            return new FeatureFlagVariant(this);
        }
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public int getRolloutPercentage() {
        return rolloutPercentage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeatureFlagVariant that = (FeatureFlagVariant) o;
        return Objects.equals(getKey(), that.getKey()) && Objects.equals(getName(), that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey(), getName());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FeatureFlagVariant.class.getSimpleName() + "[", "]")
                .add("key='" + key + "'")
                .add("name='" + name + "'")
                .add("rolloutPercentage=" + rolloutPercentage)
                .toString();
    }
}
