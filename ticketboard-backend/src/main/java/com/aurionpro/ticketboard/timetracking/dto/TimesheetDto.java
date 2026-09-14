package com.aurionpro.ticketboard.timetracking.dto;

import com.aurionpro.ticketboard.timetracking.enums.TimesheetStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimesheetDto {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double totalHours;
    private TimesheetStatus status;
    private Long reviewerId;
    private String reviewerName;
    private String rejectionReason;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private List<TimeEntryDto> entries;
}
