package com.aurionpro.ticketboard.risk.controller;

import com.aurionpro.ticketboard.common.response.ApiResponse;
import com.aurionpro.ticketboard.risk.dto.*;
import com.aurionpro.ticketboard.risk.enums.IssueSeverity;
import com.aurionpro.ticketboard.risk.enums.IssueStatus;
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
    @GetMapping("/issues")
    public ResponseEntity<ApiResponse<List<IssueDto>>> getIssues(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) IssueSeverity severity,
            @RequestParam(required = false) IssueStatus status,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Long milestoneId,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(required = false) String search) {
        List<IssueDto> issues = riskService.getIssues(projectId, severity, status, assigneeId, milestoneId, overdue, search);
        return ResponseEntity.ok(ApiResponse.ok("Issues fetched successfully", issues));
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
    public ResponseEntity<ApiResponse<IssueDto>> updateIssue(@PathVariable Long id, @RequestBody IssueDto dto) {
        IssueDto updated = riskService.updateIssue(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Issue updated successfully", updated));
    }

    @PreAuthorize("hasAnyAuthority('risk:create', 'risk:edit')")
    @PatchMapping("/issues/{id}")
    public ResponseEntity<ApiResponse<IssueDto>> patchIssue(@PathVariable Long id, @RequestBody IssueDto dto) {
        IssueDto updated = riskService.updateIssue(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Issue updated successfully", updated));
    }

    @PreAuthorize("hasAuthority('risk:view')")
    @GetMapping("/issues/{id}")
    public ResponseEntity<ApiResponse<IssueDto>> getIssueById(@PathVariable Long id) {
        IssueDto issue = riskService.getIssueById(id);
        return ResponseEntity.ok(ApiResponse.ok("Issue fetched successfully", issue));
    }

    @PreAuthorize("hasAuthority('risk:view')")
    @GetMapping("/issues/{id}/comments")
    public ResponseEntity<ApiResponse<List<IssueCommentDto>>> getComments(@PathVariable Long id) {
        List<IssueCommentDto> comments = riskService.getComments(id);
        return ResponseEntity.ok(ApiResponse.ok("Comments fetched successfully", comments));
    }

    @PreAuthorize("hasAnyAuthority('risk:create', 'risk:edit')")
    @PostMapping("/issues/{id}/comments")
    public ResponseEntity<ApiResponse<IssueCommentDto>> addComment(@PathVariable Long id,
                                                                   @RequestParam Long authorId,
                                                                   @RequestParam String content) {
        IssueCommentDto comment = riskService.addComment(id, authorId, content);
        return ResponseEntity.ok(ApiResponse.ok("Comment added successfully", comment));
    }

    @PreAuthorize("hasAuthority('risk:view')")
    @GetMapping("/issues/{id}/history")
    public ResponseEntity<ApiResponse<List<IssueHistoryDto>>> getHistory(@PathVariable Long id) {
        List<IssueHistoryDto> history = riskService.getHistory(id);
        return ResponseEntity.ok(ApiResponse.ok("Issue history fetched successfully", history));
    }

    @PreAuthorize("hasAuthority('risk:view')")
    @GetMapping("/issues/{id}/watchers")
    public ResponseEntity<ApiResponse<List<IssueWatcherDto>>> getWatchers(@PathVariable Long id) {
        List<IssueWatcherDto> watchers = riskService.getWatchers(id);
        return ResponseEntity.ok(ApiResponse.ok("Watchers fetched successfully", watchers));
    }

    @PreAuthorize("hasAnyAuthority('risk:create', 'risk:edit')")
    @PostMapping("/issues/{id}/watchers")
    public ResponseEntity<ApiResponse<IssueWatcherDto>> addWatcher(@PathVariable Long id, @RequestParam Long userId) {
        IssueWatcherDto watcher = riskService.addWatcher(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Watcher added successfully", watcher));
    }

    @PreAuthorize("hasAnyAuthority('risk:create', 'risk:edit')")
    @DeleteMapping("/issues/{id}/watchers/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeWatcher(@PathVariable Long id, @PathVariable Long userId) {
        riskService.removeWatcher(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Watcher removed successfully", null));
    }

    @PreAuthorize("hasAnyAuthority('risk:create', 'risk:edit')")
    @DeleteMapping("/issues/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteIssue(@PathVariable Long id) {
        riskService.deleteIssue(id);
        return ResponseEntity.ok(ApiResponse.ok("Issue deleted successfully", null));
    }
}