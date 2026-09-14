package com.aurionpro.ticketboard.timetracking.controller;

import com.aurionpro.ticketboard.common.response.ApiResponse;
import com.aurionpro.ticketboard.timetracking.dto.EffortVarianceDto;
import com.aurionpro.ticketboard.timetracking.dto.TimeEntryCreateDto;
import com.aurionpro.ticketboard.timetracking.dto.TimeEntryDto;
import com.aurionpro.ticketboard.timetracking.service.TimeTrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/time-entries")
@RequiredArgsConstructor
public class TimeTrackingController {

    private final TimeTrackingService timeTrackingService;

    @PreAuthorize("hasAuthority('timelog:view')")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<TimeEntryDto>>> getMyTimeEntries() {
        List<TimeEntryDto> entries = timeTrackingService.getMyTimeEntries();
        return ResponseEntity.ok(ApiResponse.ok("My time entries fetched successfully", entries));
    }

    @PreAuthorize("hasAuthority('timelog:view')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<TimeEntryDto>>> getTimeEntriesByUser(@PathVariable Long userId) {
        List<TimeEntryDto> entries = timeTrackingService.getTimeEntriesByUser(userId);
        return ResponseEntity.ok(ApiResponse.ok("User time entries fetched successfully", entries));
    }

    @PreAuthorize("hasAuthority('timelog:view')")
    @GetMapping("/project/{projectId}")
    public ResponseEntity<ApiResponse<List<TimeEntryDto>>> getTimeEntriesByProject(@PathVariable Long projectId) {
        List<TimeEntryDto> entries = timeTrackingService.getTimeEntriesByProject(projectId);
        return ResponseEntity.ok(ApiResponse.ok("Project time entries fetched successfully", entries));
    }

    @PreAuthorize("hasAuthority('timelog:manage')")
    @PostMapping
    public ResponseEntity<ApiResponse<TimeEntryDto>> logTime(@Valid @RequestBody TimeEntryCreateDto dto) {
        TimeEntryDto created = timeTrackingService.logTime(dto);
        return ResponseEntity.ok(ApiResponse.ok("Time logged successfully", created));
    }

    @PreAuthorize("hasAuthority('timelog:view')")
    @GetMapping("/variance")
    public ResponseEntity<ApiResponse<List<EffortVarianceDto>>> getEffortVariances() {
        List<EffortVarianceDto> variances = timeTrackingService.getAllEffortVariances();
        return ResponseEntity.ok(ApiResponse.ok("Effort variances fetched successfully", variances));
    }

    @PreAuthorize("hasAuthority('timelog:manage')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTimeEntry(@PathVariable Long id) {
        timeTrackingService.deleteTimeEntry(id);
        return ResponseEntity.ok(ApiResponse.ok("Time entry deleted successfully", null));
    }
}
