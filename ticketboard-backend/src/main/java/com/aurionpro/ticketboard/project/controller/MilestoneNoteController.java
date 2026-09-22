package com.aurionpro.ticketboard.project.controller;

import com.aurionpro.ticketboard.common.response.ApiResponse;
import com.aurionpro.ticketboard.project.dto.MilestoneNoteCreateDto;
import com.aurionpro.ticketboard.project.dto.MilestoneNoteDto;
import com.aurionpro.ticketboard.project.service.MilestoneNoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/milestones/{milestoneId}/notes")
@RequiredArgsConstructor
public class MilestoneNoteController {

    private final MilestoneNoteService milestoneNoteService;

    @PreAuthorize("hasAuthority('milestone:view')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MilestoneNoteDto>>> getNotesByMilestone(@PathVariable Long milestoneId) {
        List<MilestoneNoteDto> notes = milestoneNoteService.getNotesByMilestone(milestoneId);
        return ResponseEntity.ok(ApiResponse.ok("Milestone notes fetched successfully", notes));
    }

    @PreAuthorize("hasAuthority('milestone:edit')")
    @PostMapping
    public ResponseEntity<ApiResponse<MilestoneNoteDto>> addNote(
            @PathVariable Long milestoneId,
            @Valid @RequestBody MilestoneNoteCreateDto dto) {
        MilestoneNoteDto created = milestoneNoteService.addNote(milestoneId, dto);
        return ResponseEntity.ok(ApiResponse.ok("Note added successfully", created));
    }

    @PreAuthorize("hasAuthority('milestone:edit')")
    @PutMapping("/{noteId}")
    public ResponseEntity<ApiResponse<MilestoneNoteDto>> updateNote(
            @PathVariable Long milestoneId,
            @PathVariable Long noteId,
            @Valid @RequestBody MilestoneNoteCreateDto dto) {
        MilestoneNoteDto updated = milestoneNoteService.updateNote(noteId, dto);
        return ResponseEntity.ok(ApiResponse.ok("Note updated successfully", updated));
    }

    @PreAuthorize("hasAuthority('milestone:edit')")
    @DeleteMapping("/{noteId}")
    public ResponseEntity<ApiResponse<Void>> deleteNote(@PathVariable Long milestoneId, @PathVariable Long noteId) {
        milestoneNoteService.deleteNote(noteId);
        return ResponseEntity.ok(ApiResponse.ok("Note deleted successfully", null));
    }
}