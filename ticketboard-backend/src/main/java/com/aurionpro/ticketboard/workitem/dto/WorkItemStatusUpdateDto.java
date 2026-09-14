package com.aurionpro.ticketboard.workitem.dto;

import com.aurionpro.ticketboard.workitem.enums.WorkItemStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkItemStatusUpdateDto {

    @NotNull(message = "Status is required")
    private WorkItemStatus status;

    private String comment;
    private String blockedReason;
    private String blockedOwner;
    private LocalDateTime expectedResolutionDate;
}
