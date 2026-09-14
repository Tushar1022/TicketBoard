package com.aurionpro.ticketboard.dashboard.dto;

import com.aurionpro.ticketboard.capacity.dto.EmployeeWorkloadDto;
import com.aurionpro.ticketboard.capacity.dto.ProjectForecastDto;
import com.aurionpro.ticketboard.release.dto.ReleaseDto;
import com.aurionpro.ticketboard.timetracking.dto.EffortVarianceDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutiveDashboardDto {
    // 1. What is happening?
    private long totalProjects;
    private long activeProjects;
    private long completedProjects;
    private long activeRequirements;
    private long totalWorkItems;

    // 2. What is delayed & at risk?
    private long delayedProjectsCount;
    private long atRiskProjectsCount;
    private long overdueWorkItemsCount;
    private long blockedWorkItemsCount;
    private long criticalRisksCount;

    // 3. What is coming? (Next 7 / 30 days)
    private List<ReleaseDto> upcomingDeliveries;
    private int upcomingDeliveriesNext7DaysCount;
    private int upcomingDeliveriesNext30DaysCount;

    // 4. Who is overloaded?
    private List<EmployeeWorkloadDto> overloadedEmployees;

    // 5. Are we on schedule? (KPIs)
    private double onTimeDeliveryRate;
    private double totalEstimatedHours;
    private double totalActualHours;
    private double totalEffortVarianceHours;
    private double totalEffortVariancePercentage;
    private double averageTeamUtilization;

    private List<ProjectForecastDto> projectForecasts;
    private List<EffortVarianceDto> topVariances;
}
