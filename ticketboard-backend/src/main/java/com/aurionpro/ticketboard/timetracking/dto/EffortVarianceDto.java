package com.aurionpro.ticketboard.timetracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EffortVarianceDto {
    private Long projectId;
    private String projectCode;
    private String projectName;
    private Double estimatedHours;
    private Double actualHours;
    private Double varianceHours;
    private Double variancePercentage;
    private String status; // OVER_BUDGET, UNDER_BUDGET, ON_TRACK
}
