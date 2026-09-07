package com.appworks.portal;

import com.appworks.portal.entity.AlertConfiguration;
import com.appworks.portal.entity.AlertFrequency;
import com.appworks.portal.entity.AlertRecipient;
import com.appworks.portal.entity.Customer;
import com.appworks.portal.entity.Environment;
import com.appworks.portal.entity.Metric;
import com.appworks.portal.entity.Run;
import com.appworks.portal.entity.RunSource;
import com.appworks.portal.entity.RunStatus;
import com.appworks.portal.repository.AlertConfigurationRepository;
import com.appworks.portal.repository.AlertRecipientRepository;
import com.appworks.portal.repository.CustomerRepository;
import com.appworks.portal.service.AlertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests AlertService's notify logic: does it skip PASS runs? does it respect
 * frequency throttling (EVERY_FAILURE, HOURLY, DAILY)? does it update lastSentAt
 * to enable that throttling?
 */
@SpringBootTest
@Transactional
class AlertServiceTest {

    @Autowired
    private AlertService alertService;

    @Autowired
    private AlertConfigurationRepository alertConfigurationRepository;

    @Autowired
    private AlertRecipientRepository alertRecipientRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Run createRun(Customer customer, Environment environment, Metric metric,
                         RunStatus status, RunSource source) {
        return Run.builder()
                .customer(customer)
                .environment(environment)
                .metric(metric)
                .source(source)
                .status(status)
                .startedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .durationMs(100)
                .output("Test output")
                .build();
    }

    @Test
    void notify_withPassRun_doesNotSend() {
        Customer customer = Customer.builder()
                .name("TestCust")
                .code("TEST")
                .contactEmail("test@example.com")
                .build();
        customerRepository.save(customer);

        Environment env = Environment.builder()
                .customer(customer)
                .name("PROD")
                .baseUrl("https://test.com")
                .build();

        Metric metric = Metric.builder()
                .name("TestMetric")
                .serviceKey("SVC_TEST")
                .build();

        Run passRun = createRun(customer, env, metric, RunStatus.PASS, RunSource.MANUAL);

        AlertConfiguration config = AlertConfiguration.builder()
                .customer(customer)
                .frequency(AlertFrequency.EVERY_FAILURE)
                .enabled(true)
                .build();
        alertConfigurationRepository.save(config);

        LocalDateTime beforeNotify = LocalDateTime.now();
        alertService.notify(passRun);
        LocalDateTime afterNotify = LocalDateTime.now();

        Optional<AlertConfiguration> reloaded = alertConfigurationRepository.findByCustomerId(customer.getId());
        assertTrue(reloaded.isPresent());
        // lastSentAt should NOT have been updated, because PASS runs don't trigger alerts
        assertTrue(reloaded.get().getLastSentAt() == null
                || reloaded.get().getLastSentAt().isBefore(beforeNotify));
    }

    @Test
    void notify_withFailRun_sendsOnFirstOccurrence() {
        Customer customer = Customer.builder()
                .name("TestCust2")
                .code("TEST2")
                .contactEmail("test2@example.com")
                .build();
        customerRepository.save(customer);

        Environment env = Environment.builder()
                .customer(customer)
                .name("PROD")
                .baseUrl("https://test.com")
                .build();

        Metric metric = Metric.builder()
                .name("TestMetric")
                .serviceKey("SVC_TEST2")
                .build();

        Run failRun = createRun(customer, env, metric, RunStatus.FAIL, RunSource.MANUAL);

        AlertConfiguration config = AlertConfiguration.builder()
                .customer(customer)
                .frequency(AlertFrequency.EVERY_FAILURE)
                .enabled(true)
                .build();
        alertConfigurationRepository.save(config);

        LocalDateTime beforeNotify = LocalDateTime.now();
        alertService.notify(failRun);
        LocalDateTime afterNotify = LocalDateTime.now();

        Optional<AlertConfiguration> reloaded = alertConfigurationRepository.findByCustomerId(customer.getId());
        assertTrue(reloaded.isPresent());
        // lastSentAt SHOULD have been updated because FAIL triggers alert.
        // Using a small tolerance window (not strict isBefore/isAfter) because on
        // some systems (e.g. Windows) LocalDateTime.now() has coarse clock
        // resolution and back-to-back calls can return identical timestamps.
        assertTrue(reloaded.get().getLastSentAt() != null);
        assertTrue(!reloaded.get().getLastSentAt().isBefore(beforeNotify)
                && !reloaded.get().getLastSentAt().isAfter(afterNotify.plusNanos(100_000_000L)));
    }

    @Test
    void notify_withDailyFrequency_throttlesAfterFirstSend() {
        Customer customer = Customer.builder()
                .name("TestCust3")
                .code("TEST3")
                .contactEmail("test3@example.com")
                .build();
        customerRepository.save(customer);

        Environment env = Environment.builder()
                .customer(customer)
                .name("PROD")
                .baseUrl("https://test.com")
                .build();

        Metric metric = Metric.builder()
                .name("TestMetric")
                .serviceKey("SVC_TEST3")
                .build();

        AlertConfiguration config = AlertConfiguration.builder()
                .customer(customer)
                .frequency(AlertFrequency.DAILY)
                .enabled(true)
                .lastSentAt(LocalDateTime.now().minusHours(6)) // sent 6 hours ago
                .build();
        alertConfigurationRepository.save(config);

        Run failRun = createRun(customer, env, metric, RunStatus.FAIL, RunSource.MANUAL);

        LocalDateTime beforeNotify = LocalDateTime.now();
        alertService.notify(failRun);
        LocalDateTime afterNotify = LocalDateTime.now();

        Optional<AlertConfiguration> reloaded = alertConfigurationRepository.findByCustomerId(customer.getId());
        assertTrue(reloaded.isPresent());
        // lastSentAt should NOT have been updated (still ~6 hours ago) because daily
        // throttle hasn't elapsed yet
        assertTrue(reloaded.get().getLastSentAt().isBefore(beforeNotify.minusHours(5)));
    }

    @Test
    void notify_withDisabledConfig_doesNotSend() {
        Customer customer = Customer.builder()
                .name("TestCust4")
                .code("TEST4")
                .contactEmail("test4@example.com")
                .build();
        customerRepository.save(customer);

        Environment env = Environment.builder()
                .customer(customer)
                .name("PROD")
                .baseUrl("https://test.com")
                .build();

        Metric metric = Metric.builder()
                .name("TestMetric")
                .serviceKey("SVC_TEST4")
                .build();

        AlertConfiguration config = AlertConfiguration.builder()
                .customer(customer)
                .frequency(AlertFrequency.EVERY_FAILURE)
                .enabled(false)
                .build();
        alertConfigurationRepository.save(config);

        Run failRun = createRun(customer, env, metric, RunStatus.FAIL, RunSource.MANUAL);

        LocalDateTime beforeNotify = LocalDateTime.now();
        alertService.notify(failRun);
        LocalDateTime afterNotify = LocalDateTime.now();

        Optional<AlertConfiguration> reloaded = alertConfigurationRepository.findByCustomerId(customer.getId());
        assertTrue(reloaded.isPresent());
        // lastSentAt should NOT have been updated because alerts are disabled
        assertTrue(reloaded.get().getLastSentAt() == null
                || reloaded.get().getLastSentAt().isBefore(beforeNotify));
    }
}
