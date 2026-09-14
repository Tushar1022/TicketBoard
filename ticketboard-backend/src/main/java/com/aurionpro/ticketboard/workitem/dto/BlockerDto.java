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
public class BlockerDto {
    private Long id;
    private Long workItemId;
    private String workItemTicketNumber;
    private String workItemTitle;
    private String reason;
    private String owner;
    private LocalDateTime blockedSince;
    private LocalDateTime expectedResolutionDate;
    private LocalDateTime resolvedAt;
    private String resolutionNotes;
    private Long resolvedById;
    private String resolvedByName;
    private Double durationHours;
}
