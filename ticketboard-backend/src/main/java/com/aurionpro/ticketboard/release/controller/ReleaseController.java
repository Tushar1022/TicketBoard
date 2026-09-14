package com.aurionpro.ticketboard.release.controller;

import com.aurionpro.ticketboard.common.response.ApiResponse;
import com.aurionpro.ticketboard.release.dto.ReleaseCreateDto;
import com.aurionpro.ticketboard.release.dto.ReleaseDto;
import com.aurionpro.ticketboard.release.service.ReleaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/releases")
@RequiredArgsConstructor
public class ReleaseController {

    private final ReleaseService releaseService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReleaseDto>>> getAllReleases(
            @RequestParam(required = false) Long projectId) {
        List<ReleaseDto> releases = (projectId != null)
                ? releaseService.getReleasesByProject(projectId)
                : releaseService.getAllReleases();
        return ResponseEntity.ok(ApiResponse.ok("Releases fetched successfully", releases));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReleaseDto>> getReleaseById(@PathVariable Long id) {
        ReleaseDto release = releaseService.getReleaseById(id);
        return ResponseEntity.ok(ApiResponse.ok("Release fetched successfully", release));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('release:create')")
    public ResponseEntity<ApiResponse<ReleaseDto>> createRelease(@Valid @RequestBody ReleaseCreateDto dto) {
        ReleaseDto created = releaseService.createRelease(dto);
        return ResponseEntity.ok(ApiResponse.ok("Release created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('release:edit')")
    public ResponseEntity<ApiResponse<ReleaseDto>> updateRelease(@PathVariable Long id, @Valid @RequestBody ReleaseCreateDto dto) {
        ReleaseDto updated = releaseService.updateRelease(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Release updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('release:delete')")
    public ResponseEntity<ApiResponse<Void>> deleteRelease(@PathVariable Long id) {
        releaseService.deleteRelease(id);
        return ResponseEntity.ok(ApiResponse.ok("Release deleted successfully", null));
    }
}
