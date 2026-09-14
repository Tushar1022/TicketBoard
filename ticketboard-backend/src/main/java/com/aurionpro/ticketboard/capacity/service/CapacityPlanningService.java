package com.aurionpro.ticketboard.capacity.service;

import com.aurionpro.ticketboard.capacity.dto.EmployeeWorkloadDto;
import com.aurionpro.ticketboard.capacity.dto.ProjectForecastDto;
import com.aurionpro.ticketboard.capacity.dto.RequirementForecastDto;
import com.aurionpro.ticketboard.capacity.dto.TeamCapacityDto;
import com.aurionpro.ticketboard.project.entity.Project;
import com.aurionpro.ticketboard.project.entity.ProjectMember;
import com.aurionpro.ticketboard.project.enums.ProjectHealth;
import com.aurionpro.ticketboard.project.enums.ProjectStatus;
import com.aurionpro.ticketboard.project.repository.ProjectRepository;
import com.aurionpro.ticketboard.requirement.entity.Requirement;
import com.aurionpro.ticketboard.requirement.repository.RequirementRepository;
import com.aurionpro.ticketboard.timetracking.repository.TimeEntryRepository;
import com.aurionpro.ticketboard.user.entity.Team;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.enums.UserStatus;
import com.aurionpro.ticketboard.user.repository.TeamRepository;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import com.aurionpro.ticketboard.workitem.entity.WorkItem;
import com.aurionpro.ticketboard.workitem.enums.WorkItemStatus;
import com.aurionpro.ticketboard.workitem.enums.WorkItemType;
import com.aurionpro.ticketboard.workitem.repository.WorkItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CapacityPlanningService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final WorkItemRepository workItemRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final ProjectRepository projectRepository;
    private final RequirementRepository requirementRepository;

    @Transactional(readOnly = true)
    public List<EmployeeWorkloadDto> getAllEmployeeWorkloads() {
        List<User> users = userRepository.findByStatus(UserStatus.ACTIVE);
        return users.stream().map(this::calculateEmployeeWorkload).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmployeeWorkloadDto getEmployeeWorkload(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;
        return calculateEmployeeWorkload(user);
    }

    @Transactional(readOnly = true)
    public List<TeamCapacityDto> getTeamCapacities() {
        List<Team> teams = teamRepository.findAll();
        List<TeamCapacityDto> result = new ArrayList<>();

        for (Team team : teams) {
            List<User> members = userRepository.findByTeamId(team.getId());
            List<EmployeeWorkloadDto> memberWorkloads = members.stream()
                    .map(this::calculateEmployeeWorkload)
                    .collect(Collectors.toList());

            double totalCapacity = memberWorkloads.stream().mapToDouble(EmployeeWorkloadDto::getMonthlyCapacityHours).sum();
            double totalDemand = memberWorkloads.stream().mapToDouble(EmployeeWorkloadDto::getAllocatedHours).sum();
            double gap = totalDemand - totalCapacity;
            double utilPct = totalCapacity > 0 ? (totalDemand / totalCapacity) * 100.0 : 0.0;

            result.add(TeamCapacityDto.builder()
                    .teamId(team.getId())
                    .teamName(team.getName())
                    .memberCount(members.size())
                    .totalMonthlyCapacityHours(Math.round(totalCapacity * 10.0) / 10.0)
                    .totalAllocatedDemandHours(Math.round(totalDemand * 10.0) / 10.0)
                    .capacityGapHours(Math.round(gap * 10.0) / 10.0)
                    .teamUtilizationPercentage(Math.round(utilPct * 10.0) / 10.0)
                    .isShortage(gap > 0)
                    .members(memberWorkloads)
                    .build());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<ProjectForecastDto> getProjectDeliveryForecasts() {
        List<Project> activeProjects = projectRepository.findAll().stream()
                .filter(p -> p.getStatus() != ProjectStatus.COMPLETED && p.getStatus() != ProjectStatus.CLOSED)
                .collect(Collectors.toList());

        List<ProjectForecastDto> forecasts = new ArrayList<>();

        for (Project p : activeProjects) {
            double estimated = p.getEstimatedHours() != null ? p.getEstimatedHours() : 0.0;
            double actual = p.getActualHours() != null ? p.getActualHours() : 0.0;
            double remaining = Math.max(0.0, estimated - actual);

            // Calculate team daily capacity on project
            double dailyCapacity = 0.0;
            if (p.getMembers() != null && !p.getMembers().isEmpty()) {
                for (ProjectMember m : p.getMembers()) {
                    dailyCapacity += (m.getAllocatedHoursPerDay() != null ? m.getAllocatedHoursPerDay() : 8.0);
                }
            } else {
                dailyCapacity = 16.0; // default 2 developers
            }

            int daysRequired = (int) Math.ceil(remaining / Math.max(1.0, dailyCapacity));
            LocalDate projectedDate = LocalDate.now().plusDays(daysRequired);

            int delayDays = 0;
            boolean isDelayed = false;
            if (p.getPlannedEndDate() != null) {
                if (projectedDate.isAfter(p.getPlannedEndDate())) {
                    delayDays = (int) ChronoUnit.DAYS.between(p.getPlannedEndDate(), projectedDate);
                    isDelayed = true;
                }
            }

            ProjectHealth health = isDelayed ? (delayDays > 7 ? ProjectHealth.RED : ProjectHealth.AMBER) : ProjectHealth.GREEN;

            forecasts.add(ProjectForecastDto.builder()
                    .projectId(p.getId())
                    .projectCode(p.getProjectCode())
                    .projectName(p.getName())
                    .totalEstimatedHours(Math.round(estimated * 10.0) / 10.0)
                    .actualHoursLogged(Math.round(actual * 10.0) / 10.0)
                    .remainingEffortHours(Math.round(remaining * 10.0) / 10.0)
                    .teamDailyCapacity(Math.round(dailyCapacity * 10.0) / 10.0)
                    .projectedDaysRemaining(daysRequired)
                    .plannedDeliveryDate(p.getPlannedEndDate())
                    .projectedDeliveryDate(projectedDate)
                    .delayDays(delayDays)
                    .health(health)
                    .isDelayed(isDelayed)
                    .build());
        }

        return forecasts;
    }

    @Transactional(readOnly = true)
    public List<RequirementForecastDto> getRequirementForecasts() {
        List<RequirementForecastDto> forecasts = new ArrayList<>();

        int[] periods = {30, 60, 90};
        LocalDate today = LocalDate.now();

        List<User> activeUsers = userRepository.findByStatus(UserStatus.ACTIVE);
        double totalDailyCapacity = activeUsers.stream()
                .mapToDouble(u -> u.getDailyCapacityHours() != null ? u.getDailyCapacityHours() : 8.0)
                .sum();

        for (int days : periods) {
            LocalDate targetDate = today.plusDays(days);
            List<Requirement> upcoming = requirementRepository.findForecastUpcomingRequirements(today, targetDate);

            int reqCount = upcoming.size();
            double demandHours = upcoming.stream()
                    .mapToDouble(r -> r.getEstimatedEffortHours() != null ? r.getEstimatedEffortHours() : 0.0)
                    .sum();

            // Working days approx (5/7)
            int workingDays = (int) (days * 5.0 / 7.0);
            double availableCapacity = totalDailyCapacity * workingDays;
            double gap = demandHours - availableCapacity;
            double utilPct = availableCapacity > 0 ? (demandHours / availableCapacity) * 100.0 : 0.0;

            forecasts.add(RequirementForecastDto.builder()
                    .periodName("Next " + days + " Days")
                    .days(days)
                    .upcomingRequirementCount(reqCount)
                    .estimatedDemandHours(Math.round(demandHours * 10.0) / 10.0)
                    .availableTeamCapacityHours(Math.round(availableCapacity * 10.0) / 10.0)
                    .capacityGapHours(Math.round(gap * 10.0) / 10.0)
                    .utilizationPercentage(Math.round(utilPct * 10.0) / 10.0)
                    .isShortage(gap > 0)
                    .build());
        }

        return forecasts;
    }

    private EmployeeWorkloadDto calculateEmployeeWorkload(User user) {
        double dailyCap = user.getDailyCapacityHours() != null ? user.getDailyCapacityHours() : 8.0;
        double monthlyCap = dailyCap * 20.0; // 20 working days = 160 hrs

        List<WorkItem> assignedItems = workItemRepository.findByAssigneeId(user.getId());

        double allocated = 0.0;
        int activeTasks = 0;
        int openBugs = 0;

        for (WorkItem item : assignedItems) {
            if (item.getStatus() != WorkItemStatus.COMPLETED && item.getStatus() != WorkItemStatus.CLOSED) {
                allocated += (item.getEstimatedHours() != null ? item.getEstimatedHours() : 0.0);
                if (item.getType() == WorkItemType.BUG) {
                    openBugs++;
                } else {
                    activeTasks++;
                }
            }
        }

        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
        Double actualLogged = timeEntryRepository.sumHoursByUserAndDateBetween(user.getId(), startOfMonth, endOfMonth);
        double actual = actualLogged != null ? actualLogged : 0.0;

        double remaining = monthlyCap - allocated;
        double utilPct = monthlyCap > 0 ? (allocated / monthlyCap) * 100.0 : 0.0;

        return EmployeeWorkloadDto.builder()
                .userId(user.getId())
                .employeeId(user.getEmployeeId())
                .employeeName(user.getFullName())
                .designation(user.getDesignation())
                .teamName(user.getTeam() != null ? user.getTeam().getName() : "Unassigned")
                .monthlyCapacityHours(monthlyCap)
                .allocatedHours(Math.round(allocated * 10.0) / 10.0)
                .actualHoursLogged(Math.round(actual * 10.0) / 10.0)
                .remainingCapacity(Math.round(remaining * 10.0) / 10.0)
                .utilizationPercentage(Math.round(utilPct * 10.0) / 10.0)
                .isOverloaded(utilPct > 100.0)
                .activeTaskCount(activeTasks)
                .openBugCount(openBugs)
                .build();
    }
}
