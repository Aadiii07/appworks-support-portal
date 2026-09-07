package com.appworks.portal.service;

import com.appworks.portal.dto.MetricRequest;
import com.appworks.portal.dto.MetricResponse;
import com.appworks.portal.entity.Metric;
import com.appworks.portal.exception.DuplicateResourceException;
import com.appworks.portal.exception.ResourceInUseException;
import com.appworks.portal.exception.ResourceNotFoundException;
import com.appworks.portal.repository.CustomerMetricRepository;
import com.appworks.portal.repository.MetricRepository;
import com.appworks.portal.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MetricService {

    private final MetricRepository metricRepository;
    private final CustomerMetricRepository customerMetricRepository;
    private final ScheduleRepository scheduleRepository;

    public MetricResponse create(MetricRequest request) {
        if (metricRepository.existsByServiceKeyIgnoreCase(request.getServiceKey())) {
            throw new DuplicateResourceException(
                    "Metric with service key '" + request.getServiceKey() + "' already exists");
        }

        Metric metric = Metric.builder()
                .name(request.getName())
                .serviceKey(request.getServiceKey())
                .description(request.getDescription())
                .endpoint(request.getEndpoint())
                .enabled(request.getEnabled() == null || request.getEnabled())
                .warningThreshold(request.getWarningThreshold())
                .criticalThreshold(request.getCriticalThreshold())
                .comparisonOperator(request.getComparisonOperator())
                .build();

        Metric saved = metricRepository.save(metric);
        return MetricResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<MetricResponse> findAll() {
        return metricRepository.findAll().stream()
                .map(MetricResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public MetricResponse findById(Long id) {
        return MetricResponse.fromEntity(getEntityOrThrow(id));
    }

    public MetricResponse update(Long id, MetricRequest request) {
        Metric metric = getEntityOrThrow(id);

        if (!metric.getServiceKey().equalsIgnoreCase(request.getServiceKey())
                && metricRepository.existsByServiceKeyIgnoreCaseAndIdNot(request.getServiceKey(), id)) {
            throw new DuplicateResourceException(
                    "Metric with service key '" + request.getServiceKey() + "' already exists");
        }

        metric.setName(request.getName());
        metric.setServiceKey(request.getServiceKey());
        metric.setDescription(request.getDescription());
        metric.setEndpoint(request.getEndpoint());
        if (request.getEnabled() != null) {
            metric.setEnabled(request.getEnabled());
        }
        metric.setWarningThreshold(request.getWarningThreshold());
        metric.setCriticalThreshold(request.getCriticalThreshold());
        metric.setComparisonOperator(request.getComparisonOperator());

        Metric saved = metricRepository.save(metric);
        return MetricResponse.fromEntity(saved);
    }

    /**
     * Hard delete — but blocked if any customer still has this metric assigned
     * (via CustomerMetric), rather than letting a raw FK-violation reach the
     * client as a 500. The caller must un-assign the metric from all customers
     * first. This is a deliberate check, not the database's default behavior.
     */
    /**
     * Hard delete — but blocked if any customer still has this metric assigned
     * (via CustomerMetric) or any schedule still references it, rather than
     * letting a raw FK-violation reach the client as a 500. The schedule check
     * was missing before this audit: ScheduleRepository already had
     * existsByMetricId defined but nothing called it.
     */
    public void delete(Long id) {
        Metric metric = getEntityOrThrow(id);
        if (customerMetricRepository.existsByMetricId(id)) {
            throw new ResourceInUseException(
                    "Cannot delete metric '" + metric.getServiceKey()
                            + "': it is still assigned to one or more customers. Unassign it first.");
        }
        if (scheduleRepository.existsByMetricId(id)) {
            throw new ResourceInUseException(
                    "Cannot delete metric '" + metric.getServiceKey()
                            + "': it is still referenced by one or more schedules. Delete those first.");
        }
        metricRepository.delete(metric);
    }

    private Metric getEntityOrThrow(Long id) {
        return metricRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Metric not found with id " + id));
    }
}
