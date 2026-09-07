package com.appworks.portal.controller;

import com.appworks.portal.dto.MetricRequest;
import com.appworks.portal.dto.MetricResponse;
import com.appworks.portal.service.MetricService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
public class MetricController {

    private final MetricService metricService;

    @PostMapping
    public ResponseEntity<MetricResponse> create(@Valid @RequestBody MetricRequest request) {
        MetricResponse created = metricService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<MetricResponse>> findAll() {
        return ResponseEntity.ok(metricService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MetricResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(metricService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MetricResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody MetricRequest request) {
        return ResponseEntity.ok(metricService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        metricService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
