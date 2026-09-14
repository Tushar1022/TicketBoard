package com.aurionpro.ticketboard.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamDto {
    private Long id;
    private String name;
    private String code;
    private String description;
    private Long departmentId;
    private String departmentName;
    private Long teamLeadId;
    private String teamLeadName;
}
