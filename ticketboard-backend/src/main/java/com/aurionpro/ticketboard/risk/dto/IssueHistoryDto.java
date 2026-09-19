package com.aurionpro.ticketboard.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueHistoryDto {
    private Long id;
    private Long issueId;
    private Long actorId;
    private String actorName;
    private String actionType;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private LocalDateTime occurredAt;
}