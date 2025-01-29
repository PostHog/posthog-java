package com.posthog.java.flags;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

public class FeatureFlag {
    private final String key;
    private final String name;
    private final int id;
    private final int teamId;
    private final int rolloutPercentage;
    private final boolean isSimpleFlag;
    private final boolean active;
    private final boolean ensureExperienceContinuity;
    private final boolean deleted;
    private final FeatureFlagFilter featureFlagFilter;

    public static class Builder {
        private final String key;
        private final int id;
        private final int teamId;

        private int rolloutPercentage;
        private String name;
        private boolean isSimpleFlag;
        private boolean active;
        private boolean ensureExperienceContinuity;
        private boolean deleted;
        private FeatureFlagFilter featureFlagFilter;

        public Builder(String key, int id, int teamId) {
            this.key = key;
            this.id = id;
            this.teamId = teamId;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder rolloutPercentage(int rolloutPercentage) {
            this.rolloutPercentage = rolloutPercentage;
            return this;
        }

        public Builder isSimpleFlag(boolean isSimpleFlag) {
            this.isSimpleFlag = isSimpleFlag;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Builder deleted(boolean deleted) {
            this.deleted = deleted;
            return this;
        }

        public Builder ensureExperienceContinuity(boolean ensureExperienceContinuity) {
            this.ensureExperienceContinuity = ensureExperienceContinuity;
            return this;
        }

        public Builder filter(FeatureFlagFilter featureFlagFilter) {
            this.featureFlagFilter = featureFlagFilter;
            return this;
        }

        public FeatureFlag build() {
            return new FeatureFlag(this);
        }
    }

    private FeatureFlag(Builder builder) {
        this.key = builder.key;
        this.name = builder.name;
        this.id = builder.id;
        this.teamId = builder.teamId;
        this.rolloutPercentage = builder.rolloutPercentage;
        this.isSimpleFlag = builder.isSimpleFlag;
        this.active = builder.active;
        this.ensureExperienceContinuity = builder.ensureExperienceContinuity;
        this.featureFlagFilter = builder.featureFlagFilter;
        this.deleted = builder.deleted;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public int getTeamId() {
        return teamId;
    }

    public int getRolloutPercentage() {
        return rolloutPercentage;
    }

    public boolean isSimpleFlag() {
        return isSimpleFlag;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isEnsureExperienceContinuity() {
        return ensureExperienceContinuity;
    }

    public Optional<FeatureFlagFilter> getFilter() {
        return Optional.ofNullable(featureFlagFilter);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeatureFlag that = (FeatureFlag) o;
        return isActive() == that.isActive() && Objects.equals(getKey(), that.getKey()) && Objects.equals(getId(), that.getId()) && Objects.equals(getTeamId(), that.getTeamId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey(), getId(), getTeamId(), isActive());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FeatureFlag.class.getSimpleName() + "[", "]")
                .add("key='" + key + "'")
                .add("name='" + name + "'")
                .add("id='" + id + "'")
                .add("teamId='" + teamId + "'")
                .add("rolloutPercentage=" + rolloutPercentage)
                .add("isSimpleFlag=" + isSimpleFlag)
                .add("active=" + active)
                .add("ensureExperienceContinuity=" + ensureExperienceContinuity)
                .add("filters=" + featureFlagFilter)
                .toString();
    }
}
