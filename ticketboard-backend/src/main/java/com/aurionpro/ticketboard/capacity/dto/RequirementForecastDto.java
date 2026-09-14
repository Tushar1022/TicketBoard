package com.aurionpro.ticketboard.capacity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementForecastDto {
    private String periodName; // e.g. "Next 30 Days", "Next 60 Days", "Next 90 Days"
    private Integer days;
    private Integer upcomingRequirementCount;
    private Double estimatedDemandHours;
    private Double availableTeamCapacityHours;
    private Double capacityGapHours; // Demand - Available
    private Double utilizationPercentage;
    private Boolean isShortage;
}
