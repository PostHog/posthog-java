package com.posthog.java.flags;

import java.util.*;

public class FeatureFlagFilter {
    private final int aggregationGroupTypeIndex;
    private final List<FeatureFlagCondition> groups;
    private final FeatureFlagVariants multivariate;
    private final Map<String, Object> payloads;

    private FeatureFlagFilter(Builder builder) {
        this.aggregationGroupTypeIndex = builder.aggregationGroupTypeIndex;
        this.groups = builder.groups;
        this.multivariate = builder.multivariate;
        this.payloads = builder.payloads;
    }

    public static class Builder {
        private int aggregationGroupTypeIndex = 0;
        private List<FeatureFlagCondition> groups = new ArrayList<>();
        private FeatureFlagVariants multivariate = null;
        private Map<String, Object> payloads = new HashMap<>();

        public Builder aggregationGroupTypeIndex(int aggregationGroupTypeIndex) {
            this.aggregationGroupTypeIndex = aggregationGroupTypeIndex;
            return this;
        }

        public Builder groups(List<FeatureFlagCondition> groups) {
            this.groups = groups;
            return this;
        }

        public Builder multivariate(FeatureFlagVariants multivariate) {
            this.multivariate = multivariate;
            return this;
        }

        public Builder payloads(Map<String, Object> payloads) {
            this.payloads = payloads;
            return this;
        }

        public FeatureFlagFilter build() {
            return new FeatureFlagFilter(this);
        }
    }

    public int getAggregationGroupTypeIndex() {
        return aggregationGroupTypeIndex;
    }

    public List<FeatureFlagCondition> getGroups() {
        return groups;
    }

    public Optional<FeatureFlagVariants> getMultivariate() {
        return Optional.ofNullable(multivariate);
    }

    public Map<String, Object> getPayloads() {
        return payloads;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeatureFlagFilter featureFlagFilter = (FeatureFlagFilter) o;
        return getAggregationGroupTypeIndex() == featureFlagFilter.getAggregationGroupTypeIndex() && Objects.equals(getGroups(), featureFlagFilter.getGroups()) && Objects.equals(getMultivariate(), featureFlagFilter.getMultivariate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAggregationGroupTypeIndex(), getGroups(), getMultivariate());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FeatureFlagFilter.class.getSimpleName() + "[", "]")
                .add("aggregationGroupTypeIndex=" + aggregationGroupTypeIndex)
                .add("groups=" + groups)
                .add("multivariate=" + multivariate)
                .add("payloads=" + payloads)
                .toString();
    }
}
