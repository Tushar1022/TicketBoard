package com.aurionpro.ticketboard.dashboard.controller;

import com.aurionpro.ticketboard.common.response.ApiResponse;
import com.aurionpro.ticketboard.dashboard.dto.DeveloperDashboardDto;
import com.aurionpro.ticketboard.dashboard.dto.DeveloperTelemetryDto;
import com.aurionpro.ticketboard.dashboard.dto.ExecutiveDashboardDto;
import com.aurionpro.ticketboard.dashboard.dto.QaDashboardDto;
import com.aurionpro.ticketboard.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboards")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @PreAuthorize("hasAnyAuthority('dashboard:executive', 'admin:access')")
    @GetMapping("/executive")
    public ResponseEntity<ApiResponse<ExecutiveDashboardDto>> getExecutiveDashboard() {
        ExecutiveDashboardDto dashboard = dashboardService.getExecutiveDashboard();
        return ResponseEntity.ok(ApiResponse.ok("Executive dashboard metrics", dashboard));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/developer/telemetry")
    public ResponseEntity<ApiResponse<DeveloperTelemetryDto>> getDeveloperTelemetry() {
        DeveloperTelemetryDto telemetry = dashboardService.getDeveloperTelemetry();
        return ResponseEntity.ok(ApiResponse.ok("Developer workspace telemetry", telemetry));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/developer")
    public ResponseEntity<ApiResponse<DeveloperDashboardDto>> getDeveloperDashboard(
            @RequestParam(required = false) Long userId) {
        DeveloperDashboardDto dashboard = dashboardService.getDeveloperDashboard(userId);
        return ResponseEntity.ok(ApiResponse.ok("Developer dashboard metrics", dashboard));
    }

    @GetMapping("/qa")
    public ResponseEntity<ApiResponse<QaDashboardDto>> getQaDashboard() {
        QaDashboardDto dashboard = dashboardService.getQaDashboard();
        return ResponseEntity.ok(ApiResponse.ok("QA dashboard metrics", dashboard));
    }
}
