package com.appworks.portal.controller;

import com.appworks.portal.dto.ScheduleRequest;
import com.appworks.portal.dto.ScheduleResponse;
import com.appworks.portal.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping
    public ResponseEntity<ScheduleResponse> create(@Valid @RequestBody ScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.create(request));
    }

    /** Optional ?customerId= filter, since the reference UI's Schedules screen filters by customer. */
    @GetMapping
    public List<ScheduleResponse> findAll(@RequestParam(required = false) Long customerId) {
        return scheduleService.findAll(customerId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduleResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduleResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody ScheduleRequest request) {
        return ResponseEntity.ok(scheduleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
