package com.aurionpro.ticketboard.workitem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDocumentDto {
    private Long id;
    private Long workItemId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String fileUrl;
    private String downloadUrl;
    private Long uploadedById;
    private String uploadedByName;
    private LocalDateTime uploadedAt;
}
