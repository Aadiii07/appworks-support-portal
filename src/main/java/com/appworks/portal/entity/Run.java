package com.appworks.portal.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "monitor_run")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Run {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "environment_id", nullable = false)
    private Environment environment;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "metric_id", nullable = false)
    private Metric metric;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "schedule_id")
    private Schedule schedule;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private RunStatus status;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private RunSource source;
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;
    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;
    @Column(name = "duration_ms", nullable = false)
    private long durationMs;
    @Column(length = 4000)
    private String output;
    @Column(name = "raw_value")
    private Double rawValue;
    @Column(name = "error_message", length = 2000)
    private String errorMessage;
}
