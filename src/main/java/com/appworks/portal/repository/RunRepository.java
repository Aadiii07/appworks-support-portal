package com.appworks.portal.repository;

import com.appworks.portal.entity.Run;
import com.appworks.portal.entity.RunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Extends JpaSpecificationExecutor for the Run History filter — see
 * RunSpecifications for why: the original "(? IS NULL OR column = ?)" JPQL
 * pattern broke against real PostgreSQL with
 * "ERROR: could not determine data type of parameter $9" (found via manual
 * browser testing, not caught by any automated test, since all testing so
 * far ran against H2, which is more lenient about parameter typing than
 * Postgres). Specifications only add a predicate when a filter value is
 * actually provided, so no ambiguous null-typed parameter is ever sent.
 */
public interface RunRepository extends JpaRepository<Run, Long>, JpaSpecificationExecutor<Run> {

    List<Run> findTop20ByOrderByStartedAtDesc();

    List<Run> findByCustomerIdOrderByStartedAtDesc(Long customerId);

    List<Run> findByStartedAtBetween(LocalDateTime from, LocalDateTime to);

    long countByStartedAtBetween(LocalDateTime from, LocalDateTime to);

    long countByStatusAndStartedAtBetween(RunStatus status, LocalDateTime from, LocalDateTime to);

    /** Used by the Dashboard's per-customer health calculation. */
    long countByCustomerId(Long customerId);

    long countByCustomerIdAndStatus(Long customerId, RunStatus status);

    /**
     * Used by the Dashboard's "All Metrics — Last Run Status" grid: the single
     * most recent run for a given metric, across all customers/environments.
     * Called once per metric (small catalog size in practice — tens, not
     * thousands — so N+1 here is an acceptable trade-off against writing a
     * more complex grouped query).
     */
    java.util.Optional<Run> findTopByMetricIdOrderByStartedAtDesc(Long metricId);

    /**
     * Detaches runs from a schedule before that schedule is deleted, avoiding a
     * foreign-key violation. Run history is preserved (the rows still exist,
     * still show up in Run History and count toward Dashboard stats) — only the
     * link back to the now-deleted schedule is cleared. This bug was found
     * live: deleting a schedule that had already executed at least once threw
     * an unhandled 500, silently swallowed by the frontend's missing error
     * handling, which made it look like the Delete button "did nothing."
     */
    @Modifying
    @Query("UPDATE Run r SET r.schedule = null WHERE r.schedule.id = :scheduleId")
    void detachFromSchedule(@Param("scheduleId") Long scheduleId);
}
