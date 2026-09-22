package com.aurionpro.ticketboard.timetracking.service;

import com.aurionpro.ticketboard.audit.enums.TimelineEventType;
import com.aurionpro.ticketboard.audit.service.ActivityLogService;
import com.aurionpro.ticketboard.common.exception.BadRequestException;
import com.aurionpro.ticketboard.common.exception.ResourceNotFoundException;
import com.aurionpro.ticketboard.project.entity.Project;
import com.aurionpro.ticketboard.project.repository.ProjectRepository;
import com.aurionpro.ticketboard.project.service.ProjectService;
import com.aurionpro.ticketboard.requirement.entity.Requirement;
import com.aurionpro.ticketboard.requirement.repository.RequirementRepository;
import com.aurionpro.ticketboard.timetracking.dto.EffortVarianceDto;
import com.aurionpro.ticketboard.timetracking.dto.TimeEntryCreateDto;
import com.aurionpro.ticketboard.timetracking.dto.TimeEntryDto;
import com.aurionpro.ticketboard.timetracking.entity.TimeEntry;
import com.aurionpro.ticketboard.timetracking.enums.TimeEntryStatus;
import com.aurionpro.ticketboard.timetracking.repository.TimeEntryRepository;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import com.aurionpro.ticketboard.websocket.WorkspaceRealtimeService;
import com.aurionpro.ticketboard.workitem.entity.WorkItem;
import com.aurionpro.ticketboard.workitem.repository.WorkItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimeTrackingService {

    private final TimeEntryRepository timeEntryRepository;
    private final ProjectRepository projectRepository;
    private final RequirementRepository requirementRepository;
    private final WorkItemRepository workItemRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final ActivityLogService activityLogService;
    private final WorkspaceRealtimeService realtimeService;

    @Transactional(readOnly = true)
    public List<TimeEntryDto> getTimeEntriesByUser(Long userId) {
        return timeEntryRepository.findByUserIdOrderByWorkDateDesc(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TimeEntryDto> getTimeEntriesByProject(Long projectId) {
        return timeEntryRepository.findByProjectId(projectId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TimeEntryDto> getMyTimeEntries() {
        User currentUser = getCurrentUser();
        if (currentUser == null) return List.of();
        return getTimeEntriesByUser(currentUser.getId());
    }

    @Transactional
    public TimeEntryDto logTime(TimeEntryCreateDto dto) {
        User user = null;
        if (dto.getUserId() != null) {
            user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", dto.getUserId()));
        } else {
            user = getCurrentUser();
            if (user == null) throw new BadRequestException("User must be specified or authenticated");
        }

        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", dto.getProjectId()));

        Requirement requirement = null;
        if (dto.getRequirementId() != null) {
            requirement = requirementRepository.findById(dto.getRequirementId())
                    .orElseThrow(() -> new ResourceNotFoundException("Requirement", "id", dto.getRequirementId()));
        }

        WorkItem workItem = null;
        if (dto.getWorkItemId() != null) {
            workItem = workItemRepository.findById(dto.getWorkItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("WorkItem", "id", dto.getWorkItemId()));
            if (requirement == null && workItem.getRequirement() != null) {
                requirement = workItem.getRequirement();
            }
        }

        TimeEntry timeEntry = TimeEntry.builder()
                .user(user)
                .project(project)
                .requirement(requirement)
                .workItem(workItem)
                .workDate(dto.getWorkDate() != null ? dto.getWorkDate() : LocalDate.now())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .breakMinutes(dto.getBreakMinutes() != null ? dto.getBreakMinutes() : 0)
                .totalHours(dto.getTotalHours())
                .description(dto.getDescription())
                .status(TimeEntryStatus.LOGGED)
                .build();

        TimeEntry saved = timeEntryRepository.save(timeEntry);

        // Update actual hours on WorkItem
        if (workItem != null) {
            Double actualWorkItemHours = timeEntryRepository.sumHoursByWorkItemId(workItem.getId());
            workItem.setActualHours(actualWorkItemHours != null ? actualWorkItemHours : 0.0);
            workItemRepository.save(workItem);
        }

        // Update actual hours on Requirement
        if (requirement != null) {
            Double actualReqHours = timeEntryRepository.sumHoursByRequirementId(requirement.getId());
            requirement.setActualEffortHours(actualReqHours != null ? actualReqHours : 0.0);
            requirementRepository.save(requirement);
        }

        // Update actual hours on Project
        Double actualProjHours = timeEntryRepository.sumHoursByProjectId(project.getId());
        project.setActualHours(actualProjHours != null ? actualProjHours : 0.0);
        projectService.recalculateProjectMetrics(project);
        projectRepository.save(project);

        activityLogService.logEvent(
                workItem != null ? "WORK_ITEM" : "PROJECT",
                workItem != null ? workItem.getId() : project.getId(),
                workItem != null ? workItem.getTicketNumber() : project.getProjectCode(),
                TimelineEventType.HOURS_LOGGED,
                String.format("%s logged %.1f hours for date %s: %s", user.getFullName(), saved.getTotalHours(), saved.getWorkDate(), saved.getDescription()),
                "Hours logged",
                null,
                String.valueOf(saved.getTotalHours())
        );

        realtimeService.notifyDashboardSync(user.getId());

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<EffortVarianceDto> getAllEffortVariances() {
        List<Project> projects = projectRepository.findAll();
        List<EffortVarianceDto> variances = new ArrayList<>();

        for (Project p : projects) {
            double estimated = p.getEstimatedHours() != null ? p.getEstimatedHours() : 0.0;
            double actual = p.getActualHours() != null ? p.getActualHours() : 0.0;
            double variance = actual - estimated;
            double variancePct = estimated > 0 ? (variance / estimated) * 100.0 : 0.0;

            String status = "ON_TRACK";
            if (variancePct > 15.0) {
                status = "OVER_BUDGET";
            } else if (variancePct < -15.0) {
                status = "UNDER_BUDGET";
            }

            variances.add(EffortVarianceDto.builder()
                    .projectId(p.getId())
                    .projectCode(p.getProjectCode())
                    .projectName(p.getName())
                    .estimatedHours(estimated)
                    .actualHours(actual)
                    .varianceHours(Math.round(variance * 10.0) / 10.0)
                    .variancePercentage(Math.round(variancePct * 10.0) / 10.0)
                    .status(status)
                    .build());
        }

        return variances;
    }

    @Transactional
    public void deleteTimeEntry(Long id) {
        TimeEntry entry = timeEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TimeEntry", "id", id));
                
        WorkItem workItem = entry.getWorkItem();
        if (workItem != null) {
            Double actualHours = workItem.getActualHours();
            if (actualHours != null) {
                workItem.setActualHours(Math.max(0, actualHours - entry.getTotalHours()));
            }
            workItemRepository.save(workItem);
        }
        
        timeEntryRepository.delete(entry);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return userRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }

    public TimeEntryDto mapToDto(TimeEntry t) {
        return TimeEntryDto.builder()
                .id(t.getId())
                .userId(t.getUser().getId())
                .userName(t.getUser().getFullName())
                .userEmail(t.getUser().getEmail())
                .projectId(t.getProject().getId())
                .projectCode(t.getProject().getProjectCode())
                .projectName(t.getProject().getName())
                .requirementId(t.getRequirement() != null ? t.getRequirement().getId() : null)
                .reqNumber(t.getRequirement() != null ? t.getRequirement().getReqNumber() : null)
                .reqTitle(t.getRequirement() != null ? t.getRequirement().getTitle() : null)
                .workItemId(t.getWorkItem() != null ? t.getWorkItem().getId() : null)
                .workItemTicketNumber(t.getWorkItem() != null ? t.getWorkItem().getTicketNumber() : null)
                .workItemTitle(t.getWorkItem() != null ? t.getWorkItem().getTitle() : null)
                .timesheetId(t.getTimesheet() != null ? t.getTimesheet().getId() : null)
                .workDate(t.getWorkDate())
                .startTime(t.getStartTime())
                .endTime(t.getEndTime())
                .breakMinutes(t.getBreakMinutes())
                .totalHours(t.getTotalHours())
                .description(t.getDescription())
                .status(t.getStatus())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
