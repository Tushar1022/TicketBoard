package com.aurionpro.ticketboard.support.controller;

import com.aurionpro.ticketboard.common.response.ApiResponse;
import com.aurionpro.ticketboard.support.dto.CreateSupportAnnouncementRequest;
import com.aurionpro.ticketboard.support.dto.SupportAnnouncementDto;
import com.aurionpro.ticketboard.support.service.SupportAnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/support/announcements")
@RequiredArgsConstructor
public class SupportAnnouncementController {

    private final SupportAnnouncementService announcementService;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<SupportAnnouncementDto>> getActiveAnnouncement() {
        SupportAnnouncementDto dto = announcementService.getActiveAnnouncement();
        return ResponseEntity.ok(ApiResponse.ok("Active support announcement fetched", dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<SupportAnnouncementDto>>> getAllAnnouncements() {
        List<SupportAnnouncementDto> list = announcementService.getAllAnnouncements();
        return ResponseEntity.ok(ApiResponse.ok("All support announcements fetched", list));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<SupportAnnouncementDto>> createAnnouncement(
            @Valid @RequestBody CreateSupportAnnouncementRequest request,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        String email = authentication.getName();
        SupportAnnouncementDto dto = announcementService.createAnnouncement(request, email);
        return ResponseEntity.ok(ApiResponse.ok("Support announcement created successfully", dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<SupportAnnouncementDto>> updateAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody CreateSupportAnnouncementRequest request,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        String email = authentication.getName();
        SupportAnnouncementDto dto = announcementService.updateAnnouncement(id, request, email);
        return ResponseEntity.ok(ApiResponse.ok("Support announcement updated successfully", dto));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<SupportAnnouncementDto>> toggleActiveStatus(@PathVariable Long id) {
        SupportAnnouncementDto dto = announcementService.toggleActiveStatus(id);
        return ResponseEntity.ok(ApiResponse.ok("Support announcement status toggled successfully", dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id);
        return ResponseEntity.ok(ApiResponse.ok("Support announcement deleted successfully", null));
    }
}
