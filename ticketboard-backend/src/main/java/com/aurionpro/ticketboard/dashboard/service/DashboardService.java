package com.aurionpro.ticketboard.dashboard.service;

import com.aurionpro.ticketboard.capacity.dto.EmployeeWorkloadDto;
import com.aurionpro.ticketboard.capacity.dto.ProjectForecastDto;
import com.aurionpro.ticketboard.capacity.service.CapacityPlanningService;
import com.aurionpro.ticketboard.common.exception.ResourceNotFoundException;
import com.aurionpro.ticketboard.dashboard.dto.DeveloperDashboardDto;
import com.aurionpro.ticketboard.dashboard.dto.ExecutiveDashboardDto;
import com.aurionpro.ticketboard.dashboard.dto.QaDashboardDto;
import com.aurionpro.ticketboard.project.entity.Project;
import com.aurionpro.ticketboard.project.repository.ProjectRepository;
import com.aurionpro.ticketboard.release.dto.ReleaseDto;
import com.aurionpro.ticketboard.release.entity.Release;
import com.aurionpro.ticketboard.release.repository.ReleaseRepository;
import com.aurionpro.ticketboard.release.service.ReleaseService;
import com.aurionpro.ticketboard.requirement.repository.RequirementRepository;
import com.aurionpro.ticketboard.risk.repository.RiskRepository;
import com.aurionpro.ticketboard.timetracking.dto.EffortVarianceDto;
import com.aurionpro.ticketboard.timetracking.dto.TimeEntryDto;
import com.aurionpro.ticketboard.timetracking.repository.TimeEntryRepository;
import com.aurionpro.ticketboard.timetracking.service.TimeTrackingService;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import com.aurionpro.ticketboard.workitem.dto.WorkItemDto;
import com.aurionpro.ticketboard.workitem.entity.WorkItem;
import com.aurionpro.ticketboard.workitem.enums.WorkItemSeverity;
import com.aurionpro.ticketboard.workitem.enums.WorkItemStatus;
import com.aurionpro.ticketboard.workitem.enums.WorkItemType;
import com.aurionpro.ticketboard.workitem.repository.WorkItemRepository;
import com.aurionpro.ticketboard.workitem.service.WorkItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProjectRepository projectRepository;
    private final RequirementRepository requirementRepository;
    private final WorkItemRepository workItemRepository;
    private final ReleaseRepository releaseRepository;
    private final RiskRepository riskRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final UserRepository userRepository;
    private final CapacityPlanningService capacityService;
    private final TimeTrackingService timeTrackingService;
    private final WorkItemService workItemService;
    private final ReleaseService releaseService;

    @Transactional(readOnly = true)
    public ExecutiveDashboardDto getExecutiveDashboard() {
        long totalProjects = projectRepository.count();
        long activeProjects = projectRepository.countActiveProjects();
        long completedProjects = projectRepository.countCompletedProjects();
        long delayedProjects = projectRepository.countDelayedProjects();
        long atRiskProjects = projectRepository.countAtRiskProjects();
        long activeReqs = requirementRepository.countActiveRequirements();
        long totalWorkItems = workItemRepository.count();
        long blockedItems = workItemRepository.countBlockedItems();
        long criticalRisks = riskRepository.countCriticalRisks();

        LocalDate today = LocalDate.now();
        List<WorkItem> overdueItems = workItemRepository.findOverdueWorkItems(today);
        long overdueCount = overdueItems.size();

        // Deliveries in next 7 and 30 days
        List<Release> next30DaysReleases = releaseRepository.findUpcomingReleases(today, today.plusDays(30));
        List<Release> next7DaysReleases = releaseRepository.findUpcomingReleases(today, today.plusDays(7));
        List<ReleaseDto> upcomingDeliveries = next30DaysReleases.stream()
                .map(releaseService::mapToDto)
                .collect(Collectors.toList());

        // Overloaded employees
        List<EmployeeWorkloadDto> allWorkloads = capacityService.getAllEmployeeWorkloads();
        List<EmployeeWorkloadDto> overloaded = allWorkloads.stream()
                .filter(EmployeeWorkloadDto::getIsOverloaded)
                .collect(Collectors.toList());

        double avgUtilization = allWorkloads.isEmpty() ? 0.0 :
                allWorkloads.stream().mapToDouble(EmployeeWorkloadDto::getUtilizationPercentage).average().orElse(0.0);

        // Calculate total estimated vs actual effort
        List<Project> allProjectsList = projectRepository.findAll();
        double totalEst = allProjectsList.stream().mapToDouble(p -> p.getEstimatedHours() != null ? p.getEstimatedHours() : 0.0).sum();
        double totalAct = allProjectsList.stream().mapToDouble(p -> p.getActualHours() != null ? p.getActualHours() : 0.0).sum();
        double totalVar = totalAct - totalEst;
        double totalVarPct = totalEst > 0 ? (totalVar / totalEst) * 100.0 : 0.0;

        // Delivery success rate
        long completedReleases = releaseRepository.countTotalCompletedReleases();
        long successfulReleases = releaseRepository.countSuccessfulReleases();
        double successRate = completedReleases > 0 ? ((double) successfulReleases / completedReleases) * 100.0 : 0.0;

        List<ProjectForecastDto> forecasts = capacityService.getProjectDeliveryForecasts();
        List<EffortVarianceDto> topVariances = timeTrackingService.getAllEffortVariances();

        return ExecutiveDashboardDto.builder()
                .totalProjects(totalProjects)
                .activeProjects(activeProjects)
                .completedProjects(completedProjects)
                .activeRequirements(activeReqs)
                .totalWorkItems(totalWorkItems)
                .delayedProjectsCount(delayedProjects)
                .atRiskProjectsCount(atRiskProjects)
                .overdueWorkItemsCount(overdueCount)
                .blockedWorkItemsCount(blockedItems)
                .criticalRisksCount(criticalRisks)
                .upcomingDeliveries(upcomingDeliveries)
                .upcomingDeliveriesNext7DaysCount(next7DaysReleases.size())
                .upcomingDeliveriesNext30DaysCount(next30DaysReleases.size())
                .overloadedEmployees(overloaded)
                .onTimeDeliveryRate(Math.round(successRate * 10.0) / 10.0)
                .totalEstimatedHours(Math.round(totalEst * 10.0) / 10.0)
                .totalActualHours(Math.round(totalAct * 10.0) / 10.0)
                .totalEffortVarianceHours(Math.round(totalVar * 10.0) / 10.0)
                .totalEffortVariancePercentage(Math.round(totalVarPct * 10.0) / 10.0)
                .averageTeamUtilization(Math.round(avgUtilization * 10.0) / 10.0)
                .projectForecasts(forecasts)
                .topVariances(topVariances)
                .build();
    }

    @Transactional(readOnly = true)
    public DeveloperDashboardDto getDeveloperDashboard(Long userId) {
        User user = (userId != null) ? userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId))
                : getCurrentUser();

        if (user == null) {
            throw new ResourceNotFoundException("User not authenticated");
        }

        List<WorkItem> assignedItems = workItemRepository.findByAssigneeId(user.getId());

        List<WorkItemDto> activeTasks = assignedItems.stream()
                .filter(w -> w.getStatus() != WorkItemStatus.COMPLETED && w.getStatus() != WorkItemStatus.CLOSED && w.getType() != WorkItemType.BUG)
                .map(workItemService::mapToDto)
                .collect(Collectors.toList());

        List<WorkItemDto> bugs = assignedItems.stream()
                .filter(w -> w.getStatus() != WorkItemStatus.COMPLETED && w.getStatus() != WorkItemStatus.CLOSED && w.getType() == WorkItemType.BUG)
                .map(workItemService::mapToDto)
                .collect(Collectors.toList());

        List<WorkItemDto> blockedTasks = assignedItems.stream()
                .filter(w -> w.getStatus() == WorkItemStatus.BLOCKED)
                .map(workItemService::mapToDto)
                .collect(Collectors.toList());

        LocalDate now = LocalDate.now();
        List<WorkItemDto> upcomingDeadlines = assignedItems.stream()
                .filter(w -> w.getStatus() != WorkItemStatus.COMPLETED && w.getStatus() != WorkItemStatus.CLOSED && w.getDueDate() != null)
                .sorted((a, b) -> a.getDueDate().compareTo(b.getDueDate()))
                .limit(5)
                .map(workItemService::mapToDto)
                .collect(Collectors.toList());

        LocalDate monday = now.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        Double loggedThisWeek = timeEntryRepository.sumHoursByUserAndDateBetween(user.getId(), monday, sunday);

        List<TimeEntryDto> recentEntries = timeEntryRepository.findByUserIdOrderByWorkDateDesc(user.getId()).stream()
                .limit(10)
                .map(timeTrackingService::mapToDto)
                .collect(Collectors.toList());

        return DeveloperDashboardDto.builder()
                .userId(user.getId())
                .userName(user.getFullName())
                .hoursLoggedThisWeek(loggedThisWeek != null ? loggedThisWeek : 0.0)
                .weeklyCapacityHours((user.getDailyCapacityHours() != null ? user.getDailyCapacityHours() : 8.0) * 5.0)
                .assignedTasksCount(activeTasks.size())
                .openBugsCount(bugs.size())
                .blockedTasksCount(blockedTasks.size())
                .myActiveTasks(activeTasks)
                .myBugs(bugs)
                .myBlockedTasks(blockedTasks)
                .upcomingDeadlines(upcomingDeadlines)
                .recentTimeEntries(recentEntries)
                .build();
    }

    @Transactional(readOnly = true)
    public QaDashboardDto getQaDashboard() {
        List<WorkItem> testingQueueItems = workItemRepository.findByStatus(WorkItemStatus.TESTING);
        // TODO: Optimize with filtered queries instead of findAll()
        List<WorkItem> bugs = workItemRepository.findAll().stream()
                .filter(w -> w.getType() == WorkItemType.BUG && w.getStatus() != WorkItemStatus.CLOSED)
                .collect(Collectors.toList());

        List<WorkItemDto> testingQueue = testingQueueItems.stream()
                .map(workItemService::mapToDto)
                .collect(Collectors.toList());

        List<WorkItemDto> openDefects = bugs.stream()
                .map(workItemService::mapToDto)
                .collect(Collectors.toList());

        List<WorkItemDto> criticalDefects = bugs.stream()
                .filter(b -> b.getSeverity() == WorkItemSeverity.CRITICAL || b.getSeverity() == WorkItemSeverity.BLOCKER)
                .map(workItemService::mapToDto)
                .collect(Collectors.toList());

        int blockerCount = (int) bugs.stream().filter(b -> b.getSeverity() == WorkItemSeverity.BLOCKER).count();
        int resolvedPendingCount = (int) bugs.stream().filter(b -> b.getStatus() == WorkItemStatus.IN_REVIEW || b.getStatus() == WorkItemStatus.TESTING).count();

        return QaDashboardDto.builder()
                .testingQueueCount(testingQueue.size())
                .openBugsCount(openDefects.size())
                .criticalBugsCount(criticalDefects.size())
                .blockerBugsCount(blockerCount)
                .resolvedPendingVerificationCount(resolvedPendingCount)
                .testingQueue(testingQueue)
                .openDefects(openDefects)
                .criticalDefects(criticalDefects)
                .build();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return userRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }
}
