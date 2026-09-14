package com.aurionpro.ticketboard.support.dto;

import com.aurionpro.ticketboard.support.entity.SupportCategory;
import com.aurionpro.ticketboard.support.entity.TicketPriority;
import com.aurionpro.ticketboard.support.entity.TicketStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicketDto {
    private Long id;
    private String ticketCode;
    private String subject;
    private SupportCategory category;
    private TicketPriority priority;
    private String targetRole;
    private TicketStatus status;
    private Long createdById;
    private String createdByName;
    private String createdByEmail;
    private Long assignedToId;
    private String assignedToName;
    private String description;
    private String resolutionNotes;
    private String systemDiagnostics;
    private List<TicketCommentDto> comments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
