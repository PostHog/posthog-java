package com.posthog.java.flags;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class FeatureFlagVariants {

    private final List<FeatureFlagVariant> variants;

    private FeatureFlagVariants(final Builder builder) {
        this.variants = builder.variants;
    }

    public static class Builder {
        private List<FeatureFlagVariant> variants = new ArrayList<>();

        public Builder variants(List<FeatureFlagVariant> variants) {
            this.variants = variants;
            return this;
        }

        public FeatureFlagVariants build() {
            return new FeatureFlagVariants(this);
        }
    }

    public List<FeatureFlagVariant> getVariants() {
        return variants;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeatureFlagVariants featureFlagVariants1 = (FeatureFlagVariants) o;
        return Objects.equals(getVariants(), featureFlagVariants1.getVariants());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getVariants());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FeatureFlagVariants.class.getSimpleName() + "[", "]")
                .add("variants=" + variants)
                .toString();
    }
}
