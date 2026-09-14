package com.aurionpro.ticketboard.capacity.dto;

import com.aurionpro.ticketboard.project.enums.ProjectHealth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectForecastDto {
    private Long projectId;
    private String projectCode;
    private String projectName;
    private Double totalEstimatedHours;
    private Double actualHoursLogged;
    private Double remainingEffortHours;
    private Double teamDailyCapacity;
    private Integer projectedDaysRemaining;
    private LocalDate plannedDeliveryDate;
    private LocalDate projectedDeliveryDate;
    private Integer delayDays;
    private ProjectHealth health;
    private Boolean isDelayed;
}
