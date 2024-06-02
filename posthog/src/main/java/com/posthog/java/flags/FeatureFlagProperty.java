package com.posthog.java.flags;

import java.util.*;

public class FeatureFlagProperty {
    
    private final String key;
    private final String operator;
    private final List<String> value;
    private final String type;
    private final boolean negation;

    private FeatureFlagProperty(final Builder builder) {
        this.key = builder.key;
        this.operator = builder.operator;
        this.value = builder.value;
        this.type = builder.type;
        this.negation = builder.negation;
    }

    public static class Builder {
        private final String key;

        private String operator;
        private String type;
        private List<String> value = new ArrayList<>();
        private boolean negation = false;

        public Builder(String key) {
            this.key = key;
        }

        public Builder operator(String operator) {
            this.operator = operator;
            return this;
        }

        public Builder value(List<String> value) {
            this.value = value;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder negation(boolean negation) {
            this.negation = negation;
            return this;
        }

        public FeatureFlagProperty build() {
            return new FeatureFlagProperty(this);
        }
    }

    public String getKey() {
        return key;
    }

    public Optional<FeatureFlagPropertyOperator> getOperator() {
        return Optional.of(FeatureFlagPropertyOperator.fromString(operator));
    }

    public List<String> getValue() {
        return value;
    }

    public Optional<String> getType() {
        return Optional.ofNullable(type);
    }

    public boolean isNegation() {
        return negation;
    }

    public boolean isCohort() {
        return type.equals("cohort");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeatureFlagProperty that = (FeatureFlagProperty) o;
        return Objects.equals(getKey(), that.getKey());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getKey());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FeatureFlagProperty.class.getSimpleName() + "[", "]")
                .add("key='" + key + "'")
                .add("operator='" + operator + "'")
                .add("value=" + value)
                .add("type='" + type + "'")
                .add("negation=" + negation)
                .toString();
    }
}
