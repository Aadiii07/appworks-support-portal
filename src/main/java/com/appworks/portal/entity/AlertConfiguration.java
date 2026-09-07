package com.appworks.portal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One alert configuration per customer (enforced via unique=true on customer_id) —
 * a single frequency/enabled setting per customer, with the actual recipient
 * email list stored separately in AlertRecipient.
 */
@Entity
@Table(name = "alert_configuration")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AlertFrequency frequency = AlertFrequency.EVERY_FAILURE;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /** Updated by AlertService each time an alert is actually sent, for throttling. */
    @Column(name = "last_sent_at")
    private LocalDateTime lastSentAt;
}
