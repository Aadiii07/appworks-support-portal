package com.appworks.portal.repository;

import com.appworks.portal.entity.Run;
import com.appworks.portal.entity.RunStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a dynamic Run filter using JPA Specifications instead of the
 * "(? IS NULL OR column = ?)" JPQL pattern that used to live here. That
 * pattern broke against real PostgreSQL with
 * "ERROR: could not determine data type of parameter $9" — found via manual
 * browser testing, since every automated test in this project runs against
 * H2, which is far more lenient about ambiguous null-typed bind parameters
 * than Postgres's JDBC driver.
 *
 * A Specification only adds a predicate to the query when the corresponding
 * filter value is non-null, so there is never an ambiguous, type-less bind
 * parameter sent to the database — the underlying problem is avoided by
 * construction, not patched around with a cast or workaround.
 */
public final class RunSpecifications {

    private RunSpecifications() {
    }

    public static Specification<Run> filter(Long customerId, Long environmentId, Long metricId,
                                              RunStatus status, LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (customerId != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), customerId));
            }
            if (environmentId != null) {
                predicates.add(cb.equal(root.get("environment").get("id"), environmentId));
            }
            if (metricId != null) {
                predicates.add(cb.equal(root.get("metric").get("id"), metricId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startedAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startedAt"), to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
