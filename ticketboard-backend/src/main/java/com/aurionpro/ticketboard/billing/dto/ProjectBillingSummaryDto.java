package com.aurionpro.ticketboard.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectBillingSummaryDto {
    private Long projectId;
    private String projectCode;
    private String projectName;
    private String clientName;
    private Long totalBillableApprovedHours;
    private Long billedHours;
    private Long unbilledHours;
    private Double avgHourlyRate;
    private Double potentialRevenue;
}