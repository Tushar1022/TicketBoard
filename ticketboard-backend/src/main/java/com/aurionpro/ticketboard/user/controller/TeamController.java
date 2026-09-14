package com.aurionpro.ticketboard.user.controller;

import com.aurionpro.ticketboard.common.response.ApiResponse;
import com.aurionpro.ticketboard.user.dto.TeamDto;
import com.aurionpro.ticketboard.user.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TeamDto>>> getAllTeams(
            @RequestParam(required = false) Long departmentId) {
        List<TeamDto> teams = (departmentId != null) ? teamService.getTeamsByDepartment(departmentId) : teamService.getAllTeams();
        return ResponseEntity.ok(ApiResponse.ok("Teams fetched successfully", teams));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TeamDto>> getTeamById(@PathVariable Long id) {
        TeamDto team = teamService.getTeamById(id);
        return ResponseEntity.ok(ApiResponse.ok("Team fetched successfully", team));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('admin:master-data')")
    public ResponseEntity<ApiResponse<TeamDto>> createTeam(@Valid @RequestBody TeamDto dto) {
        TeamDto created = teamService.createTeam(dto);
        return ResponseEntity.ok(ApiResponse.ok("Team created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:master-data')")
    public ResponseEntity<ApiResponse<TeamDto>> updateTeam(@PathVariable Long id, @Valid @RequestBody TeamDto dto) {
        TeamDto updated = teamService.updateTeam(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Team updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:master-data')")
    public ResponseEntity<ApiResponse<Void>> deleteTeam(@PathVariable Long id) {
        teamService.deleteTeam(id);
        return ResponseEntity.ok(ApiResponse.ok("Team deleted successfully", null));
    }
}
