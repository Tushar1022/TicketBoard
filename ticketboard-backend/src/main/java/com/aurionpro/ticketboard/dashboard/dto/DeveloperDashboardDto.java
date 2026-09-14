package com.aurionpro.ticketboard.dashboard.dto;

import com.aurionpro.ticketboard.timetracking.dto.TimeEntryDto;
import com.aurionpro.ticketboard.workitem.dto.WorkItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeveloperDashboardDto {
    private Long userId;
    private String userName;
    private double hoursLoggedThisWeek;
    private double weeklyCapacityHours;
    private int assignedTasksCount;
    private int openBugsCount;
    private int blockedTasksCount;
    private List<WorkItemDto> myActiveTasks;
    private List<WorkItemDto> myBugs;
    private List<WorkItemDto> myBlockedTasks;
    private List<WorkItemDto> upcomingDeadlines;
    private List<TimeEntryDto> recentTimeEntries;
}
