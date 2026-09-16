package com.aurionpro.ticketboard.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectStatsDto {
    private int totalTasks;
    private int openTasks;
    private int completedTasks;
    private int totalRequirements;
    private int openBugs;
    private int totalMilestones;
    private int achievedMilestones;
    private int totalMembers;
    private double totalEstimatedHours;
    private double totalActualHours;
    private double completionPercentage;
    private Map<String, Integer> tasksByStatus;
    private Map<String, Integer> tasksByPriority;
    private Map<String, Integer> issuesBySeverity;
}