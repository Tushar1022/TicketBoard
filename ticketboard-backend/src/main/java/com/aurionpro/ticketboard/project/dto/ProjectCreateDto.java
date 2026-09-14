package com.aurionpro.ticketboard.project.dto;

import com.aurionpro.ticketboard.project.enums.ProjectHealth;
import com.aurionpro.ticketboard.project.enums.ProjectPriority;
import com.aurionpro.ticketboard.project.enums.ProjectStatus;
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
public class ProjectCreateDto {

    private String projectCode;

    @NotBlank(message = "Project name is required")
    private String name;

    private String description;
    private Long clientId;
    private Long projectManagerId;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private LocalDate actualEndDate;
    private ProjectPriority priority;
    private ProjectStatus status;
    private ProjectHealth health;
    private Double budget;
    private Double estimatedHours;
}
