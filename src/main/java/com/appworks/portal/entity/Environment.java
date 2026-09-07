package com.appworks.portal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents one environment (PROD / STAGING / DEV) belonging to a customer.
 * A customer can have multiple environments, each with its own AppWorks base URL —
 * this is why environment is a separate table rather than 3 columns on Customer.
 */
@Entity
@Table(name = "environment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Environment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /** e.g. PROD, STAGING, DEV — free text so the org isn't locked to exactly 3 names */
    @Column(nullable = false)
    private String name;

    @Column(name = "base_url")
    private String baseUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

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
