package com.aurionpro.ticketboard.audit.controller;

import com.aurionpro.ticketboard.audit.dto.ActivityLogDto;
import com.aurionpro.ticketboard.audit.service.ActivityLogService;
import com.aurionpro.ticketboard.common.response.ApiResponse;
import com.aurionpro.ticketboard.common.response.PaginatedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/activity-logs")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @GetMapping("/timeline")
    public ResponseEntity<ApiResponse<List<ActivityLogDto>>> getTimeline(
            @RequestParam String entityType,
            @RequestParam Long entityId) {
        List<ActivityLogDto> timeline = activityLogService.getTimeline(entityType, entityId);
        return ResponseEntity.ok(ApiResponse.ok("Timeline fetched successfully", timeline));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<ActivityLogDto>>> getRecentActivities() {
        List<ActivityLogDto> activities = activityLogService.getRecentActivities();
        return ResponseEntity.ok(ApiResponse.ok("Recent activities fetched successfully", activities));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<ActivityLogDto>>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ActivityLogDto> logs = activityLogService.getAllLogs(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok("Audit logs fetched successfully", PaginatedResponse.fromPage(logs)));
    }
}
