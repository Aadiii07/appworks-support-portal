package com.appworks.portal.controller;

import com.appworks.portal.dto.PageResponse;
import com.appworks.portal.dto.RunRequest;
import com.appworks.portal.dto.RunResponse;
import com.appworks.portal.entity.RunStatus;
import com.appworks.portal.repository.RunRepository;
import com.appworks.portal.repository.RunSpecifications;
import com.appworks.portal.service.MonitoringService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Manual "Run Now", a basic unfiltered recent list, and a filterable/paginated
 * Run History endpoint. Kept as two separate endpoints (GET /runs vs
 * GET /runs/history) rather than adding query params to the existing GET /runs,
 * because changing that endpoint's response shape (flat list -> paginated
 * wrapper) would have silently broken existing callers and the existing test
 * for it.
 */
@RestController
@RequestMapping("/api/v1/runs")
@RequiredArgsConstructor
public class RunController {

    private final MonitoringService monitoringService;
    private final RunRepository runRepository;

    @PostMapping
    public ResponseEntity<List<RunResponse>> runNow(@Valid @RequestBody RunRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(monitoringService.executeManual(request));
    }

    @GetMapping
    public List<RunResponse> recentHistory() {
        return runRepository.findTop20ByOrderByStartedAtDesc().stream()
                .map(RunResponse::from)
                .toList();
    }

    /**
     * All filters are optional — omit any of them to not filter on that field.
     * Example: GET /api/v1/runs/history?customerId=1&status=FAIL&page=0&size=20
     */
    @GetMapping("/history")
    public PageResponse<RunResponse> history(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long environmentId,
            @RequestParam(required = false) Long metricId,
            @RequestParam(required = false) RunStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt"));
        return PageResponse.from(
                runRepository.findAll(
                                RunSpecifications.filter(customerId, environmentId, metricId, status, from, to),
                                pageable)
                        .map(RunResponse::from));
    }
}
