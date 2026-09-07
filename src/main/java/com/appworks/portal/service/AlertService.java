package com.appworks.portal.service;

import com.appworks.portal.entity.AlertConfiguration;
import com.appworks.portal.entity.AlertFrequency;
import com.appworks.portal.entity.AlertRecipient;
import com.appworks.portal.entity.Run;
import com.appworks.portal.entity.RunStatus;
import com.appworks.portal.repository.AlertConfigurationRepository;
import com.appworks.portal.repository.AlertRecipientRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Decides whether a completed Run should trigger an alert, based on the
 * customer's AlertConfiguration and frequency throttling.
 *
 * Real SMTP sending is now wired in (JavaMailSender), gated behind
 * app.alerts.email-enabled (default false in application.yml, since no real
 * SMTP server/credentials have been provided). With it false, behavior is
 * unchanged from before: it logs what would be sent. Flip it to true and fill
 * in spring.mail.* in application.yml once real SMTP details exist, and it
 * will actually send. Email failures are caught and logged, not allowed to
 * propagate — a broken mail server must never take down the monitoring run
 * that triggered the alert.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertConfigurationRepository alertConfigurationRepository;
    private final AlertRecipientRepository alertRecipientRepository;
    private final JavaMailSender mailSender;

    @Value("${app.alerts.email-enabled:false}")
    private boolean emailEnabled;

    @Value("${app.alerts.from-address:noreply@supportportal.local}")
    private String fromAddress;

    public void notify(Run run) {
        if (run.getStatus() == RunStatus.PASS) {
            return; // Nothing to alert on — PASS runs don't trigger alerts.
        }

        alertConfigurationRepository.findByCustomerId(run.getCustomer().getId())
                .filter(AlertConfiguration::isEnabled)
                .filter(this::isDue)
                .ifPresent(config -> sendAlert(config, run));
    }

    private void sendAlert(AlertConfiguration config, Run run) {
        List<String> recipients = alertRecipientRepository.findByAlertConfigurationId(config.getId()).stream()
                .map(AlertRecipient::getEmail)
                .toList();
        String recipientDisplay = recipients.isEmpty()
                ? run.getCustomer().getContactEmail()
                : String.join(", ", recipients);

        String subject = "[" + run.getStatus() + "] " + run.getCustomer().getName()
                + " / " + run.getMetric().getName();
        String body = "Customer: " + run.getCustomer().getName() + "\n"
                + "Environment: " + run.getEnvironment().getName() + "\n"
                + "Metric: " + run.getMetric().getName() + " (" + run.getMetric().getServiceKey() + ")\n"
                + "Status: " + run.getStatus() + "\n"
                + "Output: " + (run.getOutput() != null ? run.getOutput() : run.getErrorMessage()) + "\n"
                + "Time: " + run.getStartedAt();

        if (!emailEnabled) {
            log.warn("MOCK EMAIL ALERT to [{}]: {}\n{}", recipientDisplay, subject, body);
            config.setLastSentAt(LocalDateTime.now());
            return;
        }

        String[] toAddresses = recipients.isEmpty()
                ? new String[]{run.getCustomer().getContactEmail()}
                : recipients.toArray(new String[0]);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toAddresses);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Sent alert email to [{}] for {} / {}",
                    String.join(", ", toAddresses), run.getCustomer().getName(), run.getMetric().getName());
        } catch (Exception ex) {
            // A broken SMTP config must not crash the monitoring run that
            // triggered this. Log loudly and move on — lastSentAt is still
            // updated below so a persistently broken mail server doesn't
            // retry-storm on every subsequent run.
            log.error("Failed to send alert email to [{}]: {}", recipientDisplay, ex.getMessage(), ex);
        }

        config.setLastSentAt(LocalDateTime.now());
    }

    private boolean isDue(AlertConfiguration config) {
        if (config.getFrequency() == AlertFrequency.EVERY_FAILURE || config.getLastSentAt() == null) {
            return true;
        }
        int throttleHours = config.getFrequency() == AlertFrequency.HOURLY ? 1 : 24;
        return config.getLastSentAt().isBefore(LocalDateTime.now().minusHours(throttleHours));
    }
}
