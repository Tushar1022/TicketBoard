package com.aurionpro.ticketboard.support.dto;

import com.aurionpro.ticketboard.support.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTicketStatusRequest {

    @NotNull(message = "Ticket status is required")
    private TicketStatus status;

    private String resolutionNotes;

    private String assignedToName;
}
