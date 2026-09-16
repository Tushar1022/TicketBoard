package com.aurionpro.ticketboard.dashboard.controller;

import com.aurionpro.ticketboard.common.response.ApiResponse;
import com.aurionpro.ticketboard.dashboard.dto.ReportCatalogItemDto;
import com.aurionpro.ticketboard.dashboard.dto.ReportDataResponse;
import com.aurionpro.ticketboard.dashboard.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("hasAnyAuthority('report:view', 'report:generate')")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/catalog")
    public ResponseEntity<ApiResponse<List<ReportCatalogItemDto>>> getReportCatalog() {
        return ResponseEntity.ok(ApiResponse.ok("Report catalog loaded successfully", reportService.getReportCatalog()));
    }

    @GetMapping("/data/{category}")
    public ResponseEntity<ApiResponse<ReportDataResponse>> getReportData(
            @PathVariable String category,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        ReportDataResponse report = reportService.getReportData(category, projectId, status, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok("Real-time report dataset generated successfully", report));
    }
}