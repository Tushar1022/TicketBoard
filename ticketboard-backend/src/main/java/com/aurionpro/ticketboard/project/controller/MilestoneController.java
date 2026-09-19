package com.aurionpro.ticketboard.project.controller;

import com.aurionpro.ticketboard.common.response.ApiResponse;
import com.aurionpro.ticketboard.project.dto.MilestoneDto;
import com.aurionpro.ticketboard.project.enums.MilestoneStatus;
import com.aurionpro.ticketboard.project.service.MilestoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/milestones")
@RequiredArgsConstructor
public class MilestoneController {

    private final MilestoneService milestoneService;

    @PreAuthorize("hasAuthority('milestone:view')")
    @GetMapping("/project/{projectId}")
    public ResponseEntity<ApiResponse<List<MilestoneDto>>> getMilestonesByProject(
            @PathVariable Long projectId,
            @RequestParam(required = false) MilestoneStatus status,
            @RequestParam(required = false) String flag,
            @RequestParam(required = false) Boolean overdue) {
        List<MilestoneDto> milestones = milestoneService.getMilestonesByProject(projectId, status, flag, overdue);
        return ResponseEntity.ok(ApiResponse.ok("Milestones fetched successfully", milestones));
    }

    @PreAuthorize("hasAuthority('milestone:view')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MilestoneDto>> getMilestoneById(@PathVariable Long id) {
        MilestoneDto milestone = milestoneService.getMilestoneById(id);
        return ResponseEntity.ok(ApiResponse.ok("Milestone fetched successfully", milestone));
    }

    @PreAuthorize("hasAuthority('milestone:create')")
    @PostMapping
    public ResponseEntity<ApiResponse<MilestoneDto>> createMilestone(@Valid @RequestBody MilestoneDto dto) {
        MilestoneDto created = milestoneService.createMilestone(dto);
        return ResponseEntity.ok(ApiResponse.ok("Milestone created successfully", created));
    }

    @PreAuthorize("hasAuthority('milestone:edit')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MilestoneDto>> updateMilestone(@PathVariable Long id, @RequestBody MilestoneDto dto) {
        MilestoneDto updated = milestoneService.updateMilestone(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Milestone updated successfully", updated));
    }

    @PreAuthorize("hasAuthority('milestone:edit')")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<MilestoneDto>> patchMilestone(@PathVariable Long id, @RequestBody MilestoneDto dto) {
        MilestoneDto updated = milestoneService.updateMilestone(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Milestone updated successfully", updated));
    }

    @PreAuthorize("hasAuthority('milestone:delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMilestone(@PathVariable Long id) {
        milestoneService.deleteMilestone(id);
        return ResponseEntity.ok(ApiResponse.ok("Milestone deleted successfully", null));
    }
}