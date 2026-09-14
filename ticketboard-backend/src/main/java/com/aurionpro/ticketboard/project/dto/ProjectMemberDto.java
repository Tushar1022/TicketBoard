package com.aurionpro.ticketboard.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberDto {
    private Long id;
    private Long projectId;
    private Long userId;
    private String userName;
    private String userEmail;
    private String projectRole;
    private Double allocatedHoursPerDay;
    private LocalDate allocationStartDate;
    private LocalDate allocationEndDate;
}
