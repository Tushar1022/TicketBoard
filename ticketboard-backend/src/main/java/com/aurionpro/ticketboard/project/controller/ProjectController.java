package com.aurionpro.ticketboard.project.controller;

import com.aurionpro.ticketboard.common.response.ApiResponse;
import com.aurionpro.ticketboard.project.dto.ProjectCreateDto;
import com.aurionpro.ticketboard.project.dto.ProjectDto;
import com.aurionpro.ticketboard.project.dto.ProjectMemberDto;
import com.aurionpro.ticketboard.project.dto.ProjectStatsDto;
import com.aurionpro.ticketboard.project.enums.ProjectStatus;
import com.aurionpro.ticketboard.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectDto>>> getAllProjects(
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(required = false) Long memberUserId) {
        List<ProjectDto> projects;
        if (memberUserId != null) {
            projects = projectService.getProjectsByMember(memberUserId);
        } else if (status != null) {
            projects = projectService.getProjectsByStatus(status);
        } else {
            projects = projectService.getAllProjects();
        }
        return ResponseEntity.ok(ApiResponse.ok("Projects fetched successfully", projects));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectDto>> getProjectById(@PathVariable Long id) {
        ProjectDto project = projectService.getProjectById(id);
        return ResponseEntity.ok(ApiResponse.ok("Project fetched successfully", project));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<ApiResponse<ProjectStatsDto>> getProjectStats(@PathVariable Long id) {
        ProjectStatsDto stats = projectService.getProjectStats(id);
        return ResponseEntity.ok(ApiResponse.ok("Project stats fetched successfully", stats));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('project:create')")
    public ResponseEntity<ApiResponse<ProjectDto>> createProject(@Valid @RequestBody ProjectCreateDto dto) {
        ProjectDto created = projectService.createProject(dto);
        return ResponseEntity.ok(ApiResponse.ok("Project created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('project:edit')")
    public ResponseEntity<ApiResponse<ProjectDto>> updateProject(@PathVariable Long id, @Valid @RequestBody ProjectCreateDto dto) {
        ProjectDto updated = projectService.updateProject(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Project updated successfully", updated));
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAuthority('project:manage-members')")
    public ResponseEntity<ApiResponse<ProjectMemberDto>> addMember(@PathVariable Long id, @Valid @RequestBody ProjectMemberDto dto) {
        ProjectMemberDto member = projectService.addProjectMember(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Project member added successfully", member));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @PreAuthorize("hasAuthority('project:manage-members')")
    public ResponseEntity<ApiResponse<Void>> removeMember(@PathVariable Long id, @PathVariable Long memberId) {
        projectService.removeProjectMember(id, memberId);
        return ResponseEntity.ok(ApiResponse.ok("Project member removed successfully", null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('project:delete')")
    public ResponseEntity<ApiResponse<Void>> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok(ApiResponse.ok("Project deleted successfully", null));
    }
}
