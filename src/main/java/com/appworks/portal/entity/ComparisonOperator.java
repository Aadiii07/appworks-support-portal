package com.appworks.portal.entity;

/**
 * Determines how a metric's raw numeric value is compared against its threshold
 * during monitoring evaluation (built in Milestone 5). Stored on Metric now so the
 * schema doesn't need to change later.
 */
public enum ComparisonOperator {
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL
}
