package com.aurionpro.ticketboard.requirement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementHistoryDto {
    private Long id;
    private Long requirementId;
    private Integer versionNumber;
    private String fieldChanged;
    private String oldValue;
    private String newValue;
    private String changeReason;
    private Long changedById;
    private String changedByName;
    private LocalDateTime changedAt;
}
