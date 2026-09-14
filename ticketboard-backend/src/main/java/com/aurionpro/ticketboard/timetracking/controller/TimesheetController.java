package com.aurionpro.ticketboard.timetracking.controller;

import com.aurionpro.ticketboard.common.response.ApiResponse;
import com.aurionpro.ticketboard.timetracking.dto.TimesheetDto;
import com.aurionpro.ticketboard.timetracking.dto.TimesheetReviewDto;
import com.aurionpro.ticketboard.timetracking.service.TimesheetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/timesheets")
@RequiredArgsConstructor
public class TimesheetController {

    private final TimesheetService timesheetService;

    @GetMapping("/week")
    public ResponseEntity<ApiResponse<TimesheetDto>> getWeeklyTimesheet(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateInWeek) {
        TimesheetDto timesheet = timesheetService.getOrCreateWeeklyTimesheet(userId, dateInWeek);
        return ResponseEntity.ok(ApiResponse.ok("Weekly timesheet fetched successfully", timesheet));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<TimesheetDto>>> getTimesheetsByUser(@PathVariable Long userId) {
        List<TimesheetDto> timesheets = timesheetService.getTimesheetsByUser(userId);
        return ResponseEntity.ok(ApiResponse.ok("User timesheets fetched successfully", timesheets));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('timelog:approve')")
    public ResponseEntity<ApiResponse<List<TimesheetDto>>> getPendingTimesheets() {
        List<TimesheetDto> pending = timesheetService.getPendingTimesheets();
        return ResponseEntity.ok(ApiResponse.ok("Pending timesheets fetched successfully", pending));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TimesheetDto>> getTimesheetById(@PathVariable Long id) {
        TimesheetDto timesheet = timesheetService.getTimesheetById(id);
        return ResponseEntity.ok(ApiResponse.ok("Timesheet fetched successfully", timesheet));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<TimesheetDto>> submitTimesheet(@PathVariable Long id) {
        TimesheetDto submitted = timesheetService.submitTimesheet(id);
        return ResponseEntity.ok(ApiResponse.ok("Timesheet submitted successfully for review", submitted));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAuthority('timelog:approve')")
    public ResponseEntity<ApiResponse<TimesheetDto>> reviewTimesheet(
            @PathVariable Long id,
            @Valid @RequestBody TimesheetReviewDto dto) {
        TimesheetDto reviewed = timesheetService.reviewTimesheet(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Timesheet review submitted successfully", reviewed));
    }
}
