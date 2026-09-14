package com.aurionpro.ticketboard.requirement.controller;

import com.aurionpro.ticketboard.common.response.ApiResponse;
import com.aurionpro.ticketboard.requirement.dto.RequirementCreateDto;
import com.aurionpro.ticketboard.requirement.dto.RequirementDto;
import com.aurionpro.ticketboard.requirement.dto.RequirementHistoryDto;
import com.aurionpro.ticketboard.requirement.dto.RequirementStatusUpdateDto;
import com.aurionpro.ticketboard.requirement.service.RequirementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/requirements")
@RequiredArgsConstructor
public class RequirementController {

    private final RequirementService requirementService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RequirementDto>>> getAllRequirements(
            @RequestParam(required = false) Long projectId) {
        List<RequirementDto> requirements = (projectId != null)
                ? requirementService.getRequirementsByProject(projectId)
                : requirementService.getAllRequirements();
        return ResponseEntity.ok(ApiResponse.ok("Requirements fetched successfully", requirements));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RequirementDto>> getRequirementById(@PathVariable Long id) {
        RequirementDto requirement = requirementService.getRequirementById(id);
        return ResponseEntity.ok(ApiResponse.ok("Requirement fetched successfully", requirement));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<RequirementHistoryDto>>> getRequirementHistory(@PathVariable Long id) {
        List<RequirementHistoryDto> history = requirementService.getRequirementHistory(id);
        return ResponseEntity.ok(ApiResponse.ok("Requirement history fetched successfully", history));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('requirement:create')")
    public ResponseEntity<ApiResponse<RequirementDto>> createRequirement(@Valid @RequestBody RequirementCreateDto dto) {
        RequirementDto created = requirementService.createRequirement(dto);
        return ResponseEntity.ok(ApiResponse.ok("Requirement created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('requirement:edit')")
    public ResponseEntity<ApiResponse<RequirementDto>> updateRequirement(@PathVariable Long id, @Valid @RequestBody RequirementCreateDto dto) {
        RequirementDto updated = requirementService.updateRequirement(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Requirement updated successfully", updated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<RequirementDto>> updateStatus(@PathVariable Long id, @Valid @RequestBody RequirementStatusUpdateDto dto) {
        RequirementDto updated = requirementService.updateStatus(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Requirement status updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('requirement:delete')")
    public ResponseEntity<ApiResponse<Void>> deleteRequirement(@PathVariable Long id) {
        requirementService.deleteRequirement(id);
        return ResponseEntity.ok(ApiResponse.ok("Requirement deleted successfully", null));
    }
}
