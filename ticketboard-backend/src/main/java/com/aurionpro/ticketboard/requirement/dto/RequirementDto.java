package com.aurionpro.ticketboard.requirement.dto;

import com.aurionpro.ticketboard.requirement.enums.RequirementPriority;
import com.aurionpro.ticketboard.requirement.enums.RequirementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementDto {
    private Long id;
    private String reqNumber;
    private String title;
    private String description;
    private String businessObjective;
    private String acceptanceCriteria;
    private RequirementPriority priority;
    private String requester;
    private Long projectId;
    private String projectCode;
    private String projectName;
    private Long ownerId;
    private String ownerName;
    private Double estimatedEffortHours;
    private Double actualEffortHours;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
    private RequirementStatus status;
    private String deliveryVersion;
    private Integer scopeVersion;
    private Double originalEstimateHours;
    private Boolean scopeCreepFlag;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
