package com.appworks.portal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "alert_recipient",
        uniqueConstraints = @UniqueConstraint(columnNames = {"alert_configuration_id", "email"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_configuration_id", nullable = false)
    private AlertConfiguration alertConfiguration;

    @Column(nullable = false)
    private String email;
}
