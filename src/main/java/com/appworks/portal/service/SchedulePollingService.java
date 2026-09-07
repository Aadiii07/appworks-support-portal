package com.appworks.portal.service;

import com.appworks.portal.entity.Schedule;
import com.appworks.portal.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Polls all enabled schedules on a fixed interval (default 60s, configurable via
 * app.scheduler.poll-ms) and executes any whose cron expression says they're due.
 * "Due" means: computing the next fire time after lastExecutedAt (or 1 minute ago,
 * for a schedule that's never run) lands at-or-before now.
 *
 * This means the actual execution granularity is bounded by the poll interval —
 * a "*&#47;5 * * * *" schedule with the default 60s poll can fire slightly late,
 * never early. That's an acceptable trade-off for a support/monitoring tool,
 * not a bug, but worth knowing if exact timing ever matters.
 */
@Component
@RequiredArgsConstructor
public class SchedulePollingService {

    private final ScheduleRepository scheduleRepository;
    private final MonitoringService monitoringService;

    @Scheduled(fixedDelayString = "${app.scheduler.poll-ms:60000}")
    @Transactional
    public void poll() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

        for (Schedule schedule : scheduleRepository.findByEnabledTrue()) {
            LocalDateTime baseline = schedule.getLastExecutedAt() != null
                    ? schedule.getLastExecutedAt()
                    : now.minusMinutes(1);

            LocalDateTime nextDue = CronExpression.parse(schedule.getCronExpression()).next(baseline);

            if (nextDue != null && !nextDue.isAfter(now)) {
                monitoringService.executeSchedule(schedule);
            }
        }
    }
}
