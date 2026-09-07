package com.appworks.portal;

import com.appworks.portal.entity.ComparisonOperator;
import com.appworks.portal.entity.Metric;
import com.appworks.portal.entity.RunStatus;
import com.appworks.portal.service.MonitoringService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests MonitoringService's evaluate() method directly via reflection, since it's
 * a private method. The tests verify threshold logic:
 * - null value or null operator -> PASS
 * - value breaches critical threshold -> FAIL
 * - value breaches warning threshold (but not critical) -> WARNING
 * - value within thresholds -> PASS
 * - handles both GREATER_THAN and LESS_THAN operators
 */
@SpringBootTest
class MonitoringServiceThresholdTest {

    @Autowired
    private MonitoringService monitoringService;

    private RunStatus evaluate(Metric metric, Double value) throws Exception {
        java.lang.reflect.Method method = MonitoringService.class.getDeclaredMethod("evaluate", Metric.class, Double.class);
        method.setAccessible(true);
        return (RunStatus) method.invoke(monitoringService, metric, value);
    }

    @Test
    void evaluate_withNullValue_returnsPass() throws Exception {
        Metric metric = Metric.builder()
                .name("TestMetric")
                .serviceKey("SVC_TEST")
                .warningThreshold(100.0)
                .criticalThreshold(200.0)
                .comparisonOperator(ComparisonOperator.GREATER_THAN)
                .build();

        RunStatus result = evaluate(metric, null);
        assertEquals(RunStatus.PASS, result);
    }

    @Test
    void evaluate_withNullOperator_returnsPass() throws Exception {
        Metric metric = Metric.builder()
                .name("TestMetric")
                .serviceKey("SVC_TEST")
                .warningThreshold(100.0)
                .criticalThreshold(200.0)
                .comparisonOperator(null)
                .build();

        RunStatus result = evaluate(metric, 150.0);
        assertEquals(RunStatus.PASS, result);
    }

    @Test
    void evaluate_greaterThan_valueBreachesCritical_returnsFail() throws Exception {
        Metric metric = Metric.builder()
                .name("QueueDepth")
                .serviceKey("SVC_QUEUE")
                .warningThreshold(100.0)
                .criticalThreshold(200.0)
                .comparisonOperator(ComparisonOperator.GREATER_THAN)
                .build();

        RunStatus result = evaluate(metric, 250.0);
        assertEquals(RunStatus.FAIL, result);
    }

    @Test
    void evaluate_greaterThan_valueAtCritical_returnsFail() throws Exception {
        Metric metric = Metric.builder()
                .name("QueueDepth")
                .serviceKey("SVC_QUEUE")
                .warningThreshold(100.0)
                .criticalThreshold(200.0)
                .comparisonOperator(ComparisonOperator.GREATER_THAN)
                .build();

        RunStatus result = evaluate(metric, 200.0);
        assertEquals(RunStatus.FAIL, result);
    }

    @Test
    void evaluate_greaterThan_valueBreachesWarning_returnsWarning() throws Exception {
        Metric metric = Metric.builder()
                .name("QueueDepth")
                .serviceKey("SVC_QUEUE")
                .warningThreshold(100.0)
                .criticalThreshold(200.0)
                .comparisonOperator(ComparisonOperator.GREATER_THAN)
                .build();

        RunStatus result = evaluate(metric, 150.0);
        assertEquals(RunStatus.WARNING, result);
    }

    @Test
    void evaluate_greaterThan_valueAtWarning_returnsWarning() throws Exception {
        Metric metric = Metric.builder()
                .name("QueueDepth")
                .serviceKey("SVC_QUEUE")
                .warningThreshold(100.0)
                .criticalThreshold(200.0)
                .comparisonOperator(ComparisonOperator.GREATER_THAN)
                .build();

        RunStatus result = evaluate(metric, 100.0);
        assertEquals(RunStatus.WARNING, result);
    }

    @Test
    void evaluate_greaterThan_valueWithinThresholds_returnsPass() throws Exception {
        Metric metric = Metric.builder()
                .name("QueueDepth")
                .serviceKey("SVC_QUEUE")
                .warningThreshold(100.0)
                .criticalThreshold(200.0)
                .comparisonOperator(ComparisonOperator.GREATER_THAN)
                .build();

        RunStatus result = evaluate(metric, 50.0);
        assertEquals(RunStatus.PASS, result);
    }

    @Test
    void evaluate_lessThan_valueBreachesCritical_returnsFail() throws Exception {
        Metric metric = Metric.builder()
                .name("Availability")
                .serviceKey("SVC_AVAIL")
                .warningThreshold(80.0)
                .criticalThreshold(50.0)
                .comparisonOperator(ComparisonOperator.LESS_THAN)
                .build();

        RunStatus result = evaluate(metric, 40.0);
        assertEquals(RunStatus.FAIL, result);
    }

    @Test
    void evaluate_lessThan_valueBreachesWarning_returnsWarning() throws Exception {
        Metric metric = Metric.builder()
                .name("Availability")
                .serviceKey("SVC_AVAIL")
                .warningThreshold(80.0)
                .criticalThreshold(50.0)
                .comparisonOperator(ComparisonOperator.LESS_THAN)
                .build();

        RunStatus result = evaluate(metric, 70.0);
        assertEquals(RunStatus.WARNING, result);
    }

    @Test
    void evaluate_lessThan_valueWithinThresholds_returnsPass() throws Exception {
        Metric metric = Metric.builder()
                .name("Availability")
                .serviceKey("SVC_AVAIL")
                .warningThreshold(80.0)
                .criticalThreshold(50.0)
                .comparisonOperator(ComparisonOperator.LESS_THAN)
                .build();

        RunStatus result = evaluate(metric, 95.0);
        assertEquals(RunStatus.PASS, result);
    }

    @Test
    void evaluate_noWarningThreshold_skipWarning() throws Exception {
        Metric metric = Metric.builder()
                .name("TestMetric")
                .serviceKey("SVC_TEST")
                .warningThreshold(null)
                .criticalThreshold(200.0)
                .comparisonOperator(ComparisonOperator.GREATER_THAN)
                .build();

        // 150 is above the missing warning threshold, but below critical
        // The logic should skip the warning check and go straight to critical
        RunStatus result = evaluate(metric, 150.0);
        assertEquals(RunStatus.PASS, result);
    }

    @Test
    void evaluate_noCriticalThreshold_skipCritical() throws Exception {
        Metric metric = Metric.builder()
                .name("TestMetric")
                .serviceKey("SVC_TEST")
                .warningThreshold(100.0)
                .criticalThreshold(null)
                .comparisonOperator(ComparisonOperator.GREATER_THAN)
                .build();

        // 150 is above the warning threshold, but critical is null
        // The logic should still evaluate the warning
        RunStatus result = evaluate(metric, 150.0);
        assertEquals(RunStatus.WARNING, result);
    }
}
