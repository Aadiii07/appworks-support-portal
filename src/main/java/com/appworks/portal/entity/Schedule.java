package com.appworks.portal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Ties a Metric to a specific Customer + Environment on a cron schedule.
 * Table is named monitor_schedule (not "schedule") to avoid colliding with the
 * SQL reserved word in some database dialects.
 */
@Entity
@Table(name = "monitor_schedule",
        uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "environment_id", "metric_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id", nullable = false)
    private Environment environment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_id", nullable = false)
    private Metric metric;

    @Column(name = "cron_expression", nullable = false)
    private String cronExpression;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /** Set by SchedulePollingService each time this schedule actually runs. */
    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;

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
