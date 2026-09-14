package com.aurionpro.ticketboard.risk.controller;

import com.aurionpro.ticketboard.common.response.ApiResponse;
import com.aurionpro.ticketboard.risk.dto.IssueDto;
import com.aurionpro.ticketboard.risk.dto.RiskDto;
import com.aurionpro.ticketboard.risk.service.RiskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/risks")
@RequiredArgsConstructor
public class RiskController {

    private final RiskService riskService;

    @PreAuthorize("hasAuthority('risk:view')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RiskDto>>> getAllRisks(
            @RequestParam(required = false) Long projectId) {
        List<RiskDto> risks = (projectId != null)
                ? riskService.getRisksByProject(projectId)
                : riskService.getAllRisks();
        return ResponseEntity.ok(ApiResponse.ok("Risks fetched successfully", risks));
    }

    @PreAuthorize("hasAnyAuthority('risk:create', 'risk:edit')")
    @PostMapping
    public ResponseEntity<ApiResponse<RiskDto>> createRisk(@Valid @RequestBody RiskDto dto) {
        RiskDto created = riskService.createRisk(dto);
        return ResponseEntity.ok(ApiResponse.ok("Risk created successfully", created));
    }

    @PreAuthorize("hasAnyAuthority('risk:create', 'risk:edit')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RiskDto>> updateRisk(@PathVariable Long id, @Valid @RequestBody RiskDto dto) {
        RiskDto updated = riskService.updateRisk(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Risk updated successfully", updated));
    }

    @PreAuthorize("hasAnyAuthority('risk:create', 'risk:edit')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRisk(@PathVariable Long id) {
        riskService.deleteRisk(id);
        return ResponseEntity.ok(ApiResponse.ok("Risk deleted successfully", null));
    }

    @PreAuthorize("hasAuthority('risk:view')")
    @GetMapping("/issues/project/{projectId}")
    public ResponseEntity<ApiResponse<List<IssueDto>>> getIssuesByProject(@PathVariable Long projectId) {
        List<IssueDto> issues = riskService.getIssuesByProject(projectId);
        return ResponseEntity.ok(ApiResponse.ok("Issues fetched successfully", issues));
    }

    @PreAuthorize("hasAnyAuthority('risk:create', 'risk:edit')")
    @PostMapping("/issues")
    public ResponseEntity<ApiResponse<IssueDto>> createIssue(@Valid @RequestBody IssueDto dto) {
        IssueDto created = riskService.createIssue(dto);
        return ResponseEntity.ok(ApiResponse.ok("Issue created successfully", created));
    }

    @PreAuthorize("hasAnyAuthority('risk:create', 'risk:edit')")
    @PutMapping("/issues/{id}")
    public ResponseEntity<ApiResponse<IssueDto>> updateIssue(@PathVariable Long id, @Valid @RequestBody IssueDto dto) {
        IssueDto updated = riskService.updateIssue(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Issue updated successfully", updated));
    }
}
