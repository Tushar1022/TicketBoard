package com.aurionpro.ticketboard.timetracking.dto;

import com.aurionpro.ticketboard.timetracking.enums.TimeEntryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntryDto {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private Long projectId;
    private String projectCode;
    private String projectName;
    private Long requirementId;
    private String reqNumber;
    private String reqTitle;
    private Long workItemId;
    private String workItemTicketNumber;
    private String workItemTitle;
    private Long timesheetId;
    private LocalDate workDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer breakMinutes;
    private Double totalHours;
    private String description;
    private TimeEntryStatus status;
    private LocalDateTime createdAt;
}
