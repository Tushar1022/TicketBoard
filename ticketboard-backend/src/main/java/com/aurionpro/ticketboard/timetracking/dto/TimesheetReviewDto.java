package com.aurionpro.ticketboard.timetracking.dto;

import com.aurionpro.ticketboard.timetracking.enums.TimesheetStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimesheetReviewDto {

    @NotNull(message = "Status is required")
    private TimesheetStatus status; // APPROVED or REJECTED

    private String rejectionReason;
}
