package com.posthog.java.flags;

import java.util.Objects;
import java.util.StringJoiner;

public class FeatureFlagVariantMeta {
    public final String key;
    public final double valueMin;
    public final double valueMax;

    private FeatureFlagVariantMeta(Builder builder) {
        this.key = builder.key;
        this.valueMin = builder.valueMin;
        this.valueMax = builder.valueMax;
    }

    public static class Builder {
        private final String key;

        private double valueMin = 0;
        private double valueMax = 0;

        public Builder(String key) {
            this.key = key;
        }

        public Builder valueMin(double valueMin) {
            this.valueMin = valueMin;
            return this;
        }

        public Builder valueMax(double valueMax) {
            this.valueMax = valueMax;
            return this;
        }

        public FeatureFlagVariantMeta build() {
            return new FeatureFlagVariantMeta(this);
        }
    }

    public String getKey() {
        return key;
    }

    public double getValueMin() {
        return valueMin;
    }

    public double getValueMax() {
        return valueMax;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeatureFlagVariantMeta that = (FeatureFlagVariantMeta) o;
        return Objects.equals(getKey(), that.getKey());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getKey());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FeatureFlagVariantMeta.class.getSimpleName() + "[", "]")
                .add("key='" + key + "'")
                .add("valueMin=" + valueMin)
                .add("valueMax=" + valueMax)
                .toString();
    }
}
