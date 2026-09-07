package com.appworks.portal;

import com.appworks.portal.entity.Customer;
import com.appworks.portal.entity.Environment;
import com.appworks.portal.entity.Metric;
import com.appworks.portal.entity.Schedule;
import com.appworks.portal.repository.CustomerRepository;
import com.appworks.portal.repository.EnvironmentRepository;
import com.appworks.portal.repository.MetricRepository;
import com.appworks.portal.repository.ScheduleRepository;
import com.appworks.portal.service.SchedulePollingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests SchedulePollingService.poll() to verify:
 * - Enabled schedules with a cron due to fire have their lastExecutedAt updated
 * - Disabled schedules are never executed
 * - Schedules not yet due remain unchanged
 *
 * The poll() method is @Scheduled, so it doesn't run automatically in tests.
 * We call it directly via reflection to drive the test.
 */
@SpringBootTest
@Transactional
class SchedulePollingServiceTest {

    @Autowired
    private SchedulePollingService schedulePollingService;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private MetricRepository metricRepository;

    private void callPoll() throws Exception {
        java.lang.reflect.Method method = SchedulePollingService.class.getDeclaredMethod("poll");
        method.setAccessible(true);
        method.invoke(schedulePollingService);
    }

    @Test
    void poll_withDueSchedule_executesAndUpdatesLastExecutedAt() throws Exception {
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
        environmentRepository.save(env);

        Metric metric = Metric.builder()
                .name("TestMetric")
                .serviceKey("SVC_TEST")
                .build();
        metricRepository.save(metric);

        // Cron that fires every minute: */1 * * * * normalized to 0 */1 * * * *
        Schedule schedule = Schedule.builder()
                .customer(customer)
                .environment(env)
                .metric(metric)
                .cronExpression("0 * * * * *")
                .enabled(true)
                .lastExecutedAt(LocalDateTime.now().minusMinutes(5))
                .build();
        Schedule saved = scheduleRepository.save(schedule);

        LocalDateTime beforePoll = LocalDateTime.now();
        callPoll();
        LocalDateTime afterPoll = LocalDateTime.now();

        Optional<Schedule> reloaded = scheduleRepository.findById(saved.getId());
        assertTrue(reloaded.isPresent());
        // lastExecutedAt should have been updated to roughly "now"
        assertNotNull(reloaded.get().getLastExecutedAt());
        assertTrue(!reloaded.get().getLastExecutedAt().isBefore(beforePoll)
                && !reloaded.get().getLastExecutedAt().isAfter(afterPoll.plusNanos(100_000_000L)));
    }

    @Test
    void poll_withFutureSchedule_doesNotExecute() throws Exception {
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
        environmentRepository.save(env);

        Metric metric = Metric.builder()
                .name("TestMetric")
                .serviceKey("SVC_TEST2")
                .build();
        metricRepository.save(metric);

        // Cron that fires once a year (Jan 1st, 00:00:00) — guarantees the next
        // fire time is always far in the future relative to "now", regardless of
        // exactly when this test happens to run. An earlier version of this test
        // used an hourly cron with a hand-computed "2 hours ago" baseline, which
        // was fragile: depending on the wall-clock minute the test ran in, the
        // schedule could end up genuinely overdue and correctly fire — that was
        // a bug in the test's assumption, not in the polling logic itself.
        LocalDateTime lastExecutedAt = LocalDateTime.now();

        Schedule schedule = Schedule.builder()
                .customer(customer)
                .environment(env)
                .metric(metric)
                .cronExpression("0 0 0 1 1 *")
                .enabled(true)
                .lastExecutedAt(lastExecutedAt)
                .build();
        Schedule saved = scheduleRepository.save(schedule);

        callPoll();

        Optional<Schedule> reloaded = scheduleRepository.findById(saved.getId());
        assertTrue(reloaded.isPresent());
        // lastExecutedAt should NOT have changed — next Jan 1st is always in the future
        assertTrue(reloaded.get().getLastExecutedAt().isEqual(lastExecutedAt));
    }

    @Test
    void poll_withDisabledSchedule_doesNotExecute() throws Exception {
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
        environmentRepository.save(env);

        Metric metric = Metric.builder()
                .name("TestMetric")
                .serviceKey("SVC_TEST3")
                .build();
        metricRepository.save(metric);

        // Cron that would fire every minute, but schedule is disabled
        LocalDateTime lastExecutedAt = LocalDateTime.now().minusMinutes(5);
        Schedule schedule = Schedule.builder()
                .customer(customer)
                .environment(env)
                .metric(metric)
                .cronExpression("0 * * * * *")
                .enabled(false)
                .lastExecutedAt(lastExecutedAt)
                .build();
        Schedule saved = scheduleRepository.save(schedule);

        callPoll();

        Optional<Schedule> reloaded = scheduleRepository.findById(saved.getId());
        assertTrue(reloaded.isPresent());
        // lastExecutedAt should NOT have changed because the schedule is disabled
        assertEquals(lastExecutedAt, reloaded.get().getLastExecutedAt());
    }

    @Test
    void poll_withNeverExecutedSchedule_executeIfDue() throws Exception {
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
        environmentRepository.save(env);

        Metric metric = Metric.builder()
                .name("TestMetric")
                .serviceKey("SVC_TEST4")
                .build();
        metricRepository.save(metric);

        // Schedule that's never been executed (lastExecutedAt = null)
        // Cron every minute should fire
        Schedule schedule = Schedule.builder()
                .customer(customer)
                .environment(env)
                .metric(metric)
                .cronExpression("0 * * * * *")
                .enabled(true)
                .lastExecutedAt(null)
                .build();
        Schedule saved = scheduleRepository.save(schedule);

        LocalDateTime beforePoll = LocalDateTime.now();
        callPoll();
        LocalDateTime afterPoll = LocalDateTime.now();

        Optional<Schedule> reloaded = scheduleRepository.findById(saved.getId());
        assertTrue(reloaded.isPresent());
        // lastExecutedAt should have been set to roughly "now"
        assertNotNull(reloaded.get().getLastExecutedAt());
        assertTrue(!reloaded.get().getLastExecutedAt().isBefore(beforePoll)
                && !reloaded.get().getLastExecutedAt().isAfter(afterPoll.plusNanos(100_000_000L)));
    }

    private void assertEquals(LocalDateTime expected, LocalDateTime actual) {
        assertTrue(actual != null && actual.isAfter(expected.minusSeconds(1))
                && actual.isBefore(expected.plusSeconds(1)),
                "Expected " + expected + " but got " + actual);
    }
}
