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
public class ProjectDocumentDto {
    private Long id;
    private Long projectId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String description;
    private String fileUrl;
    private String downloadUrl;
    private Long uploadedById;
    private String uploadedByName;
    private LocalDateTime uploadedAt;
}