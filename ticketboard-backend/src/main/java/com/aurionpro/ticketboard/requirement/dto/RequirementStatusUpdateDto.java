package com.aurionpro.ticketboard.requirement.dto;

import com.aurionpro.ticketboard.requirement.enums.RequirementStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementStatusUpdateDto {

    @NotNull(message = "Status is required")
    private RequirementStatus status;

    private String comment;
}
