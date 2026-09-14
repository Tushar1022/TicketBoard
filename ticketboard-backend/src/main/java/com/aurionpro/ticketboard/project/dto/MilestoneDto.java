package com.aurionpro.ticketboard.project.dto;

import com.aurionpro.ticketboard.project.enums.MilestoneStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneDto {
    private Long id;
    private Long projectId;

    @NotBlank(message = "Milestone name is required")
    private String name;

    private String description;
    private LocalDate plannedDate;
    private LocalDate actualDate;
    private Long ownerId;
    private String ownerName;
    private MilestoneStatus status;
    private Double completionPercentage;
}
