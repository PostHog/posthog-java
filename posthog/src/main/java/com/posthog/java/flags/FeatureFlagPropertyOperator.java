package com.posthog.java.flags;

public enum FeatureFlagPropertyOperator {
    EXACT("exact"),
    IS_NOT("is_not"),
    IS_SET("is_set"),
    CONTAINS_INSENSITIVE("icontains"),
    NOT_CONTAINS_INSENSITIVE("not_icontains"),
    REGEX("regex"),
    NOT_REGEX("not_regex"),
    GREATER_THAN("gt"),
    GREATER_THAN_OR_EQUAL("gte"),
    LESS_THAN("lt"),
    LESS_THAN_OR_EQUAL("lte");

    private final String operator;

    FeatureFlagPropertyOperator(String operator) {
        this.operator = operator;
    }

    public String getOperator() {
        return operator;
    }

    public static FeatureFlagPropertyOperator fromString(String operator) {
        for (FeatureFlagPropertyOperator op : FeatureFlagPropertyOperator.values()) {
            if (op.getOperator().equalsIgnoreCase(operator)) {
                return op;
            }
        }
        throw new IllegalArgumentException("No enum constant with operator: " + operator);
    }
}
