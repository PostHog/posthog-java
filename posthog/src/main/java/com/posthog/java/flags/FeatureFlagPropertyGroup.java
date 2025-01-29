package com.posthog.java.flags;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class FeatureFlagPropertyGroup {
    private final String type;
    private final List<Object> values;

    private FeatureFlagPropertyGroup(final Builder builder) {
        this.type = builder.type;
        this.values = builder.values;
    }

    public static class Builder {
        private String type;
        private List<Object> values;

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder values(List<Object> values) {
            this.values = values;
            return this;
        }

        public FeatureFlagPropertyGroup build() {
            return new FeatureFlagPropertyGroup(this);
        }
    }

    public String getType() {
        return type;
    }

    public List<Object> getValues() {
        return values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeatureFlagPropertyGroup that = (FeatureFlagPropertyGroup) o;
        return Objects.equals(getType(), that.getType()) && Objects.equals(getValues(), that.getValues());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), getValues());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FeatureFlagPropertyGroup.class.getSimpleName() + "[", "]")
                .add("type='" + type + "'")
                .add("values=" + values)
                .toString();
    }
}
