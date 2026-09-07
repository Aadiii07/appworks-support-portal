package com.appworks.portal.exception;

/**
 * Thrown when a delete is rejected because another record still depends on it
 * (e.g. deleting a Metric that's still assigned to a customer via CustomerMetric).
 * Distinct from DuplicateResourceException even though both map to HTTP 409 —
 * the cause is different and callers may want to handle them differently later.
 */
public class ResourceInUseException extends RuntimeException {
    public ResourceInUseException(String message) {
        super(message);
    }
}
