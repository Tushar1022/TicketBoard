package com.aurionpro.ticketboard.audit.dto;

import com.aurionpro.ticketboard.audit.enums.TimelineEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogDto {
    private Long id;
    private String entityType;
    private Long entityId;
    private String entityCode;
    private TimelineEventType eventType;
    private String summary;
    private String details;
    private String oldValue;
    private String newValue;
    private Long userId;
    private String userFullName;
    private LocalDateTime timestamp;
}
