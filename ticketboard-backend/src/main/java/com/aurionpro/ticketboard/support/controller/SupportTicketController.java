package com.aurionpro.ticketboard.support.controller;

import com.aurionpro.ticketboard.common.response.ApiResponse;
import com.aurionpro.ticketboard.support.dto.*;
import com.aurionpro.ticketboard.support.service.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/support/tickets")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    @PostMapping
    public ResponseEntity<ApiResponse<SupportTicketDto>> createTicket(
            @Valid @RequestBody CreateSupportTicketRequest request,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        String email = authentication.getName();
        SupportTicketDto ticket = supportTicketService.createTicket(request, email);
        return ResponseEntity.ok(ApiResponse.ok("Support ticket raised successfully", ticket));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<SupportTicketDto>>> getMyTickets(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        String email = authentication.getName();
        List<SupportTicketDto> tickets = supportTicketService.getTicketsForUser(email);
        return ResponseEntity.ok(ApiResponse.ok("User support tickets fetched", tickets));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN', 'ROLE_ADMIN', 'ROLE_PROJECT_MANAGER', 'ROLE_TEAM_LEAD')")
    public ResponseEntity<ApiResponse<List<SupportTicketDto>>> getAdminQueue() {
        List<SupportTicketDto> tickets = supportTicketService.getAllTicketsForAdmin();
        return ResponseEntity.ok(ApiResponse.ok("Admin support ticket queue fetched", tickets));
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN', 'ROLE_ADMIN', 'ROLE_PROJECT_MANAGER', 'ROLE_TEAM_LEAD')")
    public ResponseEntity<ApiResponse<SupportStatsDto>> getAdminStats() {
        SupportStatsDto stats = supportTicketService.getStats();
        return ResponseEntity.ok(ApiResponse.ok("Admin support ticket statistics fetched", stats));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupportTicketDto>> getTicketById(@PathVariable Long id) {
        SupportTicketDto ticket = supportTicketService.getTicketById(id);
        return ResponseEntity.ok(ApiResponse.ok("Support ticket details fetched", ticket));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN', 'ROLE_ADMIN', 'ROLE_PROJECT_MANAGER', 'ROLE_TEAM_LEAD')")
    public ResponseEntity<ApiResponse<SupportTicketDto>> updateTicketStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketStatusRequest request,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        String email = authentication.getName();
        SupportTicketDto updated = supportTicketService.updateTicketStatus(id, request, email);
        return ResponseEntity.ok(ApiResponse.ok("Support ticket status updated successfully", updated));
    }

    @PostMapping(value = "/{id}/attachments", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SupportTicketDto>> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        String email = authentication.getName();
        SupportTicketDto updated = supportTicketService.uploadAttachment(id, file, email);
        return ResponseEntity.ok(ApiResponse.ok("Attachment uploaded to support ticket", updated));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<TicketCommentDto>> addComment(
            @PathVariable Long id,
            @Valid @RequestBody AddTicketCommentRequest request,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        String email = authentication.getName();
        TicketCommentDto comment = supportTicketService.addComment(id, request, email);
        return ResponseEntity.ok(ApiResponse.ok("Comment added to support ticket", comment));
    }

    @GetMapping("/open-count")
    public ResponseEntity<ApiResponse<Long>> getOpenTicketsCount() {
        long count = supportTicketService.getOpenTicketsCount();
        return ResponseEntity.ok(ApiResponse.ok("Open support tickets count fetched", count));
    }
}
