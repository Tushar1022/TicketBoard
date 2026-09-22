package com.aurionpro.ticketboard.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneNoteDto {
    private Long id;
    private Long milestoneId;
    private Long authorId;
    private String authorName;
    private String authorEmail;
    private String content;
    private Boolean pinned;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}