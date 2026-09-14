package com.aurionpro.ticketboard.comment.controller;

import com.aurionpro.ticketboard.comment.dto.CommentDto;
import com.aurionpro.ticketboard.comment.service.CommentService;
import com.aurionpro.ticketboard.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/comments")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentDto>>> getComments(
            @RequestParam String entityType,
            @RequestParam Long entityId) {
        List<CommentDto> comments = commentService.getComments(entityType, entityId);
        return ResponseEntity.ok(ApiResponse.ok("Comments fetched successfully", comments));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CommentDto>> addComment(@Valid @RequestBody CommentDto dto) {
        CommentDto created = commentService.addComment(dto);
        return ResponseEntity.ok(ApiResponse.ok("Comment added successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CommentDto>> updateComment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String content = body.getOrDefault("content", "");
        CommentDto updated = commentService.updateComment(id, content);
        return ResponseEntity.ok(ApiResponse.ok("Comment updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return ResponseEntity.ok(ApiResponse.ok("Comment deleted successfully", null));
    }
}
