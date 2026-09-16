package com.aurionpro.ticketboard.support.dto;

import com.aurionpro.ticketboard.support.entity.TicketPriority;
import com.aurionpro.ticketboard.support.entity.TicketStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO for one row of the per-ticket support activity timeline
 * (actionType = CREATE | STATUS_CHANGE | ASSIGNMENT | COMMENT | ATTACHMENT).
 * Drives the "history / what changed & who" panel in both user + admin capsules.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicketActivityDto {

    private Long id;
    private Long ticketIdMonad;
    private String actorName;
    private String actorRole;
    private TicketStatus statusBefore;
    private TicketStatus statusAfter;
    private TicketPriority priorityBefore;
    private TicketPriority priorityAfter;
    private String assignmentBefore;
    private String assignmentAfter;
    private String actionType;
    private String summary;
    private String detail;
    private LocalDateTime occurredAt;
}
