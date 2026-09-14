package com.aurionpro.ticketboard.capacity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamCapacityDto {
    private Long teamId;
    private String teamName;
    private Integer memberCount;
    private Double totalMonthlyCapacityHours; // e.g. 5 * 160 = 800 hrs
    private Double totalAllocatedDemandHours; // total demand from requirements
    private Double capacityGapHours;          // demand - capacity (> 0 means shortage!)
    private Double teamUtilizationPercentage;
    private Boolean isShortage;
    private List<EmployeeWorkloadDto> members;
}
