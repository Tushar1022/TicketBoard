package com.aurionpro.ticketboard.requirement.dto;

import com.aurionpro.ticketboard.requirement.enums.RequirementPriority;
import com.aurionpro.ticketboard.requirement.enums.RequirementStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementCreateDto {

    private String reqNumber;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    private String businessObjective;
    private String acceptanceCriteria;
    private RequirementPriority priority;
    private String requester;

    @NotNull(message = "Project ID is required")
    private Long projectId;

    private Long ownerId;
    private Double estimatedEffortHours;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
    private RequirementStatus status;
    private String deliveryVersion;
    private String changeReason;
}
