package com.aurionpro.ticketboard.project.dto;

import com.aurionpro.ticketboard.project.enums.ProjectHealth;
import com.aurionpro.ticketboard.project.enums.ProjectPriority;
import com.aurionpro.ticketboard.project.enums.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDto {
    private Long id;
    private String projectCode;
    private String name;
    private String description;
    private Long clientId;
    private String clientName;
    private Long projectManagerId;
    private String projectManagerName;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private LocalDate actualEndDate;
    private ProjectPriority priority;
    private ProjectStatus status;
    private ProjectHealth health;
    private Double budget;
    private Double estimatedHours;
    private Double actualHours;
    private Double completionPercentage;
    private Integer requirementCount;
    private Integer taskCount;
    private Integer openBugCount;
    private List<ProjectMemberDto> members;
    private List<MilestoneDto> milestones;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
