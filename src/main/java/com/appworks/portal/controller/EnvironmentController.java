package com.appworks.portal.controller;

import com.appworks.portal.dto.EnvironmentRequest;
import com.appworks.portal.dto.EnvironmentResponse;
import com.appworks.portal.service.EnvironmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/environments")
@RequiredArgsConstructor
public class EnvironmentController {

    private final EnvironmentService environmentService;

    @PostMapping
    public ResponseEntity<EnvironmentResponse> create(@PathVariable Long customerId,
                                                        @Valid @RequestBody EnvironmentRequest request) {
        EnvironmentResponse created = environmentService.create(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<EnvironmentResponse>> findAll(@PathVariable Long customerId) {
        return ResponseEntity.ok(environmentService.findAllForCustomer(customerId));
    }

    @GetMapping("/{environmentId}")
    public ResponseEntity<EnvironmentResponse> findOne(@PathVariable Long customerId,
                                                         @PathVariable Long environmentId) {
        return ResponseEntity.ok(environmentService.findOne(customerId, environmentId));
    }

    @PutMapping("/{environmentId}")
    public ResponseEntity<EnvironmentResponse> update(@PathVariable Long customerId,
                                                        @PathVariable Long environmentId,
                                                        @Valid @RequestBody EnvironmentRequest request) {
        return ResponseEntity.ok(environmentService.update(customerId, environmentId, request));
    }

    @DeleteMapping("/{environmentId}")
    public ResponseEntity<Void> delete(@PathVariable Long customerId,
                                        @PathVariable Long environmentId) {
        environmentService.delete(customerId, environmentId);
        return ResponseEntity.noContent().build();
    }
}
