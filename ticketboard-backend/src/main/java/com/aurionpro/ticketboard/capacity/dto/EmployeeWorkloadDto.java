package com.aurionpro.ticketboard.capacity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeWorkloadDto {
    private Long userId;
    private String employeeId;
    private String employeeName;
    private String designation;
    private String teamName;
    private Double monthlyCapacityHours; // e.g. 160 hrs
    private Double allocatedHours;       // sum of estimated hours on active assigned tasks
    private Double actualHoursLogged;    // logged in current month
    private Double remainingCapacity;    // capacity - allocated
    private Double utilizationPercentage;// (allocated / capacity) * 100
    private Boolean isOverloaded;        // utilization > 100%
    private Integer activeTaskCount;
    private Integer openBugCount;
}
