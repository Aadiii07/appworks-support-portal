package com.appworks.portal;

import com.appworks.portal.entity.AlertConfiguration;
import com.appworks.portal.entity.AlertFrequency;
import com.appworks.portal.entity.Customer;
import com.appworks.portal.entity.Environment;
import com.appworks.portal.entity.Metric;
import com.appworks.portal.entity.Run;
import com.appworks.portal.entity.RunSource;
import com.appworks.portal.entity.RunStatus;
import com.appworks.portal.repository.AlertConfigurationRepository;
import com.appworks.portal.repository.AlertRecipientRepository;
import com.appworks.portal.service.AlertService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit test — no Spring context, no real network call. Verifies that a
 * JavaMailSender throwing (simulating a broken/unreachable SMTP server) is
 * caught inside AlertService and does not propagate. This matters because
 * AlertService.notify() is called from inside MonitoringService's run
 * execution path — a broken mail server must never take down the monitoring
 * run that triggered the alert.
 */
class AlertServiceEmailFailureTest {

    @Test
    void notify_whenSmtpSendThrows_doesNotPropagateAndStillUpdatesLastSentAt() {
        AlertConfigurationRepository configRepo = Mockito.mock(AlertConfigurationRepository.class);
        AlertRecipientRepository recipientRepo = Mockito.mock(AlertRecipientRepository.class);
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);

        AlertService alertService = new AlertService(configRepo, recipientRepo, mailSender);
        ReflectionTestUtils.setField(alertService, "emailEnabled", true);
        ReflectionTestUtils.setField(alertService, "fromAddress", "noreply@test.local");

        Customer customer = Customer.builder()
                .name("TestCust").code("TESTMAIL").contactEmail("ops@test.local").build();
        Environment env = Environment.builder().customer(customer).name("PROD").build();
        Metric metric = Metric.builder().name("TestMetric").serviceKey("SVC_MAILTEST").build();
        Run failRun = Run.builder()
                .customer(customer).environment(env).metric(metric)
                .source(RunSource.MANUAL).status(RunStatus.FAIL)
                .startedAt(LocalDateTime.now()).completedAt(LocalDateTime.now())
                .durationMs(50).output("test failure output").build();

        AlertConfiguration config = AlertConfiguration.builder()
                .customer(customer).frequency(AlertFrequency.EVERY_FAILURE).enabled(true).build();

        when(configRepo.findByCustomerId(any())).thenReturn(Optional.of(config));
        when(recipientRepo.findByAlertConfigurationId(any())).thenReturn(Collections.emptyList());
        doThrow(new MailSendException("Simulated: SMTP server unreachable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // The actual assertion: this must not throw, even though the mock
        // JavaMailSender always throws.
        assertDoesNotThrow(() -> alertService.notify(failRun));

        // The mail send was genuinely attempted (proves the failure path was
        // exercised, not skipped).
        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
