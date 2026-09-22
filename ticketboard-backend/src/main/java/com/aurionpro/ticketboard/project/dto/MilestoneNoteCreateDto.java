package com.aurionpro.ticketboard.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneNoteCreateDto {

    @NotBlank(message = "Note content is required")
    @Size(max = 5000, message = "Note content must be at most 5000 characters")
    private String content;

    private Boolean pinned;
}