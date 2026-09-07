package com.appworks.portal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A metric is a global, org-wide catalog entry (e.g. "Queue Depth Monitor" / SVC_QUEUE_03).
 * It is NOT customer-specific — which customers actually run it is controlled by
 * CustomerMetric. This mirrors the reference: metrics are configured once in
 * "AppWorks Config" and then enabled per customer.
 */
@Entity
@Table(name = "metric", uniqueConstraints = @UniqueConstraint(columnNames = "service_key"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Metric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** e.g. SVC_QUEUE_03 — the AppWorks service key this metric maps to. */
    @Column(name = "service_key", nullable = false, unique = true)
    private String serviceKey;

    @Column(length = 1000)
    private String description;

    /** e.g. /api/v1/metrics/queue/03 — the AppWorks endpoint invoked for this metric. */
    @Column
    private String endpoint;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /**
     * Threshold fields used by the monitoring engine (Milestone 5) to turn a raw
     * numeric result into PASS / WARNING / FAIL. All nullable: a metric with no
     * thresholds configured is simply not evaluated numerically yet.
     */
    @Column(name = "warning_threshold")
    private Double warningThreshold;

    @Column(name = "critical_threshold")
    private Double criticalThreshold;

    @Enumerated(EnumType.STRING)
    @Column(name = "comparison_operator")
    private ComparisonOperator comparisonOperator;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
