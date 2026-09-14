package com.aurionpro.ticketboard.project.controller;

import com.aurionpro.ticketboard.common.response.ApiResponse;
import com.aurionpro.ticketboard.project.dto.MilestoneDto;
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

    @GetMapping("/project/{projectId}")
    public ResponseEntity<ApiResponse<List<MilestoneDto>>> getMilestonesByProject(@PathVariable Long projectId) {
        List<MilestoneDto> milestones = milestoneService.getMilestonesByProject(projectId);
        return ResponseEntity.ok(ApiResponse.ok("Milestones fetched successfully", milestones));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('milestone:create')")
    public ResponseEntity<ApiResponse<MilestoneDto>> createMilestone(@Valid @RequestBody MilestoneDto dto) {
        MilestoneDto created = milestoneService.createMilestone(dto);
        return ResponseEntity.ok(ApiResponse.ok("Milestone created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('milestone:edit')")
    public ResponseEntity<ApiResponse<MilestoneDto>> updateMilestone(@PathVariable Long id, @Valid @RequestBody MilestoneDto dto) {
        MilestoneDto updated = milestoneService.updateMilestone(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Milestone updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('milestone:delete')")
    public ResponseEntity<ApiResponse<Void>> deleteMilestone(@PathVariable Long id) {
        milestoneService.deleteMilestone(id);
        return ResponseEntity.ok(ApiResponse.ok("Milestone deleted successfully", null));
    }
}
