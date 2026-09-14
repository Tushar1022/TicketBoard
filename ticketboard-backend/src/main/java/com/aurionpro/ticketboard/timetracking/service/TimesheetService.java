package com.aurionpro.ticketboard.timetracking.service;

import com.aurionpro.ticketboard.audit.enums.TimelineEventType;
import com.aurionpro.ticketboard.audit.service.ActivityLogService;
import com.aurionpro.ticketboard.common.exception.BadRequestException;
import com.aurionpro.ticketboard.common.exception.ResourceNotFoundException;
import com.aurionpro.ticketboard.timetracking.dto.TimeEntryDto;
import com.aurionpro.ticketboard.timetracking.dto.TimesheetDto;
import com.aurionpro.ticketboard.timetracking.dto.TimesheetReviewDto;
import com.aurionpro.ticketboard.timetracking.entity.TimeEntry;
import com.aurionpro.ticketboard.timetracking.entity.Timesheet;
import com.aurionpro.ticketboard.timetracking.enums.TimeEntryStatus;
import com.aurionpro.ticketboard.timetracking.enums.TimesheetStatus;
import com.aurionpro.ticketboard.timetracking.repository.TimeEntryRepository;
import com.aurionpro.ticketboard.timetracking.repository.TimesheetRepository;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimesheetService {

    private final TimesheetRepository timesheetRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final UserRepository userRepository;
    private final TimeTrackingService timeTrackingService;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<TimesheetDto> getTimesheetsByUser(Long userId) {
        return timesheetRepository.findByUserIdOrderByStartDateDesc(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TimesheetDto> getPendingTimesheets() {
        return timesheetRepository.findByStatus(TimesheetStatus.SUBMITTED).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TimesheetDto getTimesheetById(Long id) {
        Timesheet sheet = timesheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet", "id", id));
        return mapToDto(sheet);
    }

    @Transactional
    public TimesheetDto getOrCreateWeeklyTimesheet(Long userId, LocalDate dateInWeek) {
        User user = (userId != null) ? userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId))
                : getCurrentUser();

        if (user == null) {
            throw new BadRequestException("User not authenticated or specified");
        }

        LocalDate start = (dateInWeek != null ? dateInWeek : LocalDate.now()).with(DayOfWeek.MONDAY);
        LocalDate end = start.plusDays(6);

        Timesheet timesheet = timesheetRepository.findByUserIdAndStartDateAndEndDate(user.getId(), start, end)
                .orElseGet(() -> {
                    Timesheet newSheet = Timesheet.builder()
                            .user(user)
                            .startDate(start)
                            .endDate(end)
                            .totalHours(0.0)
                            .status(TimesheetStatus.DRAFT)
                            .build();
                    return timesheetRepository.save(newSheet);
                });

        // Link any unlinked time entries for that week
        List<TimeEntry> entries = timeEntryRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(user.getId(), start, end);
        double totalHours = 0.0;
        for (TimeEntry e : entries) {
            if (e.getTimesheet() == null) {
                e.setTimesheet(timesheet);
                timeEntryRepository.save(e);
            }
            totalHours += (e.getTotalHours() != null ? e.getTotalHours() : 0.0);
        }

        timesheet.setTotalHours(Math.round(totalHours * 10.0) / 10.0);
        Timesheet saved = timesheetRepository.save(timesheet);

        return mapToDto(saved);
    }

    @Transactional
    public TimesheetDto submitTimesheet(Long id) {
        Timesheet timesheet = timesheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet", "id", id));

        if (timesheet.getStatus() == TimesheetStatus.APPROVED) {
            throw new BadRequestException("Timesheet is already approved");
        }

        timesheet.setStatus(TimesheetStatus.SUBMITTED);
        timesheet.setSubmittedAt(LocalDateTime.now());
        Timesheet saved = timesheetRepository.save(timesheet);

        activityLogService.logEvent(
                "TIMESHEET",
                saved.getId(),
                "TS-" + saved.getId(),
                TimelineEventType.STATUS_CHANGED,
                String.format("%s submitted weekly timesheet (%s to %s) with %.1f hours",
                        saved.getUser().getFullName(), saved.getStartDate(), saved.getEndDate(), saved.getTotalHours()),
                "Awaiting manager review",
                TimesheetStatus.DRAFT.name(),
                TimesheetStatus.SUBMITTED.name()
        );

        return mapToDto(saved);
    }

    @Transactional
    public TimesheetDto reviewTimesheet(Long id, TimesheetReviewDto dto) {
        Timesheet timesheet = timesheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet", "id", id));

        User reviewer = getCurrentUser();

        timesheet.setStatus(dto.getStatus());
        timesheet.setReviewer(reviewer);
        timesheet.setReviewedAt(LocalDateTime.now());

        if (dto.getStatus() == TimesheetStatus.REJECTED) {
            timesheet.setRejectionReason(dto.getRejectionReason());
        } else if (dto.getStatus() == TimesheetStatus.APPROVED) {
            timesheet.setRejectionReason(null);
            // Mark all entries as APPROVED
            List<TimeEntry> entries = timeEntryRepository.findByTimesheetId(timesheet.getId());
            for (TimeEntry e : entries) {
                e.setStatus(TimeEntryStatus.APPROVED);
                timeEntryRepository.save(e);
            }
        }

        Timesheet saved = timesheetRepository.save(timesheet);

        activityLogService.logEvent(
                "TIMESHEET",
                saved.getId(),
                "TS-" + saved.getId(),
                dto.getStatus() == TimesheetStatus.APPROVED ? TimelineEventType.APPROVED : TimelineEventType.REJECTED,
                String.format("Timesheet (%s to %s) %s by %s",
                        saved.getStartDate(), saved.getEndDate(), saved.getStatus(), reviewer != null ? reviewer.getFullName() : "Reviewer"),
                dto.getRejectionReason(),
                TimesheetStatus.SUBMITTED.name(),
                saved.getStatus().name()
        );

        return mapToDto(saved);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return userRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }

    public TimesheetDto mapToDto(Timesheet t) {
        List<TimeEntryDto> entryDtos = timeEntryRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                t.getUser().getId(), t.getStartDate(), t.getEndDate()
        ).stream().map(timeTrackingService::mapToDto).collect(Collectors.toList());

        return TimesheetDto.builder()
                .id(t.getId())
                .userId(t.getUser().getId())
                .userName(t.getUser().getFullName())
                .userEmail(t.getUser().getEmail())
                .startDate(t.getStartDate())
                .endDate(t.getEndDate())
                .totalHours(t.getTotalHours())
                .status(t.getStatus())
                .reviewerId(t.getReviewer() != null ? t.getReviewer().getId() : null)
                .reviewerName(t.getReviewer() != null ? t.getReviewer().getFullName() : null)
                .rejectionReason(t.getRejectionReason())
                .submittedAt(t.getSubmittedAt())
                .reviewedAt(t.getReviewedAt())
                .entries(entryDtos)
                .build();
    }
}
