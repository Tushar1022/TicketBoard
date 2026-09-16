package com.aurionpro.ticketboard.support.dto;

import com.aurionpro.ticketboard.support.entity.SupportCategory;
import com.aurionpro.ticketboard.support.entity.TicketPriority;
import com.aurionpro.ticketboard.support.entity.TicketStatus;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportStatsDto {

    private long totalTickets;
    private long openTickets;
    private long inReviewTickets;
    private long resolvedTickets;
    private long closedTickets;
    private long urgentTickets;
    private long highPriorityTickets;
    private long mediumPriorityTickets;
    private long lowPriorityTickets;
    private long unassignedTickets;

    private Map<TicketStatus, Long> byStatus;
    private Map<TicketPriority, Long> byPriority;
    private Map<SupportCategory, Long> byCategory;
    private Map<String, Long> byTargetRole;
}