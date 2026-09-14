package com.aurionpro.ticketboard.timetracking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntryCreateDto {

    private Long userId;

    @NotNull(message = "Project ID is required")
    private Long projectId;

    private Long requirementId;
    private Long workItemId;

    @NotNull(message = "Work date is required")
    private LocalDate workDate;

    private LocalTime startTime;
    private LocalTime endTime;
    private Integer breakMinutes;

    @NotNull(message = "Total hours is required")
    @Positive(message = "Total hours must be greater than 0")
    private Double totalHours;

    private String description;
}
