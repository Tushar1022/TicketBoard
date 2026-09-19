package com.aurionpro.ticketboard.project.dto;

import com.aurionpro.ticketboard.project.enums.MilestoneProgressSource;
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
    private String milestoneCode;
    private Long projectId;
    private String projectCode;
    private String projectName;

    @NotBlank(message = "Milestone name is required")
    private String name;

    private String description;
    private LocalDate startDate;
    private LocalDate plannedDate;
    private LocalDate targetDate;
    private LocalDate actualDate;
    private String priority;
    private String flag;
    private Long parentMilestoneId;
    private String parentName;
    private Long releaseId;
    private String releaseVersion;
    private Long ownerId;
    private String ownerName;
    private MilestoneStatus status;
    private Double completionPercentage;
    private MilestoneProgressSource progressSource;
    private Long linkedTaskCount;
    private Long linkedIssueCount;
}