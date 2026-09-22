package com.aurionpro.ticketboard.focus.service;

import com.aurionpro.ticketboard.common.exception.BadRequestException;
import com.aurionpro.ticketboard.focus.dto.FocusSessionDto;
import com.aurionpro.ticketboard.focus.entity.FocusSession;
import com.aurionpro.ticketboard.focus.entity.FocusSessionStatus;
import com.aurionpro.ticketboard.focus.repository.FocusSessionRepository;
import com.aurionpro.ticketboard.timetracking.dto.TimeEntryCreateDto;
import com.aurionpro.ticketboard.timetracking.dto.TimeEntryDto;
import com.aurionpro.ticketboard.timetracking.service.TimeTrackingService;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FocusSessionService {

    private final FocusSessionRepository focusSessionRepository;
    private final WorkItemRepository workItemRepository;
    private final UserRepository userRepository;
    private final TimeTrackingService timeTrackingService;
    private final WorkspaceRealtimeService realtimeService;

    @Transactional
    public FocusSessionDto start(Long workItemId, String description) {
        User user = getCurrentUser();
        if (user == null) throw new BadRequestException("User must be authenticated");

        focusSessionRepository.findFirstByUserIdAndStatusOrderByUpdatedAtDesc(user.getId(), FocusSessionStatus.RUNNING)
                .ifPresent(running -> pauseInternal(running, user.getId()));

        WorkItem workItem = workItemRepository.findById(workItemId)
                .orElseThrow(() -> new BadRequestException("Work item not found: " + workItemId));

        FocusSession session = FocusSession.builder()
                .userId(user.getId())
                .workItemId(workItemId)
                .status(FocusSessionStatus.RUNNING)
                .accumulatedSeconds(0)
                .startedAt(LocalDateTime.now())
                .lastResumeAt(LocalDateTime.now())
                .description(description)
                .build();
        session.touch();
        FocusSession saved = focusSessionRepository.save(session);
        FocusSessionDto dto = mapToDto(saved, workItem);
        realtimeService.notifyFocusSession(user.getId(), "FOCUS_STARTED", dto);
        return dto;
    }

    @Transactional
    public FocusSessionDto pause() {
        User user = getCurrentUser();
        if (user == null) throw new BadRequestException("User must be authenticated");
        FocusSession session = focusSessionRepository
                .findFirstByUserIdAndStatusOrderByUpdatedAtDesc(user.getId(), FocusSessionStatus.RUNNING)
                .orElseThrow(() -> new BadRequestException("No running focus session"));
        pauseInternal(session, user.getId());
        FocusSessionDto dto = mapToDto(session, resolveWorkItem(session.getWorkItemId()));
        realtimeService.notifyFocusSession(user.getId(), "FOCUS_PAUSED", dto);
        return dto;
    }

    private void pauseInternal(FocusSession session, Long userId) {
        session.setAccumulatedSeconds(elapsedSeconds(session));
        session.setStatus(FocusSessionStatus.PAUSED);
        session.setLastResumeAt(null);
        session.touch();
        focusSessionRepository.save(session);
    }

    @Transactional
    public FocusSessionDto resume() {
        User user = getCurrentUser();
        if (user == null) throw new BadRequestException("User must be authenticated");
        FocusSession session = focusSessionRepository
                .findFirstByUserIdAndStatusOrderByUpdatedAtDesc(user.getId(), FocusSessionStatus.PAUSED)
                .orElseThrow(() -> new BadRequestException("No paused focus session"));
        session.setStatus(FocusSessionStatus.RUNNING);
        session.setLastResumeAt(LocalDateTime.now());
        session.touch();
        focusSessionRepository.save(session);
        FocusSessionDto dto = mapToDto(session, resolveWorkItem(session.getWorkItemId()));
        realtimeService.notifyFocusSession(user.getId(), "FOCUS_RESUMED", dto);
        return dto;
    }

    @Transactional
    public FocusSessionDto reset() {
        User user = getCurrentUser();
        if (user == null) throw new BadRequestException("User must be authenticated");
        FocusSession session = focusSessionRepository
                .findFirstByUserIdAndStatusOrderByUpdatedAtDesc(user.getId(), FocusSessionStatus.RUNNING)
                .orElseGet(() -> focusSessionRepository
                        .findFirstByUserIdAndStatusOrderByUpdatedAtDesc(user.getId(), FocusSessionStatus.PAUSED)
                        .orElse(null));
        if (session == null) throw new BadRequestException("No focus session to reset");
        session.setAccumulatedSeconds(0);
        session.setStatus(FocusSessionStatus.PAUSED);
        session.setLastResumeAt(null);
        session.touch();
        focusSessionRepository.save(session);
        FocusSessionDto dto = mapToDto(session, resolveWorkItem(session.getWorkItemId()));
        realtimeService.notifyFocusSession(user.getId(), "FOCUS_RESET", dto);
        return dto;
    }

    @Transactional
    public TimeEntryDto logTime(String description) {
        User user = getCurrentUser();
        if (user == null) throw new BadRequestException("User must be authenticated");
        FocusSession session = focusSessionRepository
                .findFirstByUserIdAndStatusOrderByUpdatedAtDesc(user.getId(), FocusSessionStatus.RUNNING)
                .orElseGet(() -> focusSessionRepository
                        .findFirstByUserIdAndStatusOrderByUpdatedAtDesc(user.getId(), FocusSessionStatus.PAUSED)
                        .orElseThrow(() -> new BadRequestException("No focus session with time to log")));

        WorkItem workItem = resolveWorkItem(session.getWorkItemId());
        long totalSeconds = elapsedSeconds(session);
        if (totalSeconds < 60) {
            throw new BadRequestException("Focus session must be at least 1 minute to log time");
        }
        double hours = Math.round((totalSeconds / 3600.0) * 100.0) / 100.0;

        TimeEntryCreateDto dto = TimeEntryCreateDto.builder()
                .projectId(workItem.getProject() != null ? workItem.getProject().getId() : null)
                .workItemId(workItem.getId())
                .workDate(java.time.LocalDate.now())
                .totalHours(hours)
                .description(description != null && !description.isBlank()
                        ? description
                        : (workItem.getTicketNumber() + " — " + workItem.getTitle()))
                .build();

        TimeEntryDto entry = timeTrackingService.logTime(dto);

        session.setStatus(FocusSessionStatus.LOGGED);
        session.setAccumulatedSeconds(totalSeconds);
        session.setLastResumeAt(null);
        session.touch();
        focusSessionRepository.save(session);
        realtimeService.notifyFocusSession(user.getId(), "FOCUS_LOGGED", null);
        realtimeService.notifyDashboardSync(user.getId());

        return entry;
    }

    @Transactional(readOnly = true)
    public FocusSessionDto getCurrent() {
        User user = getCurrentUser();
        if (user == null) return null;
        FocusSession session = focusSessionRepository
                .findFirstByUserIdAndStatusOrderByUpdatedAtDesc(user.getId(), FocusSessionStatus.RUNNING)
                .orElseGet(() -> focusSessionRepository
                        .findFirstByUserIdAndStatusOrderByUpdatedAtDesc(user.getId(), FocusSessionStatus.PAUSED)
                        .orElse(null));
        if (session == null) return null;
        return mapToDto(session, resolveWorkItem(session.getWorkItemId()));
    }

    @Transactional(readOnly = true)
    public List<FocusSessionDto> getHistory() {
        User user = getCurrentUser();
        if (user == null) return List.of();
        List<FocusSession> sessions = focusSessionRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());
        List<FocusSessionDto> result = new ArrayList<>();
        for (FocusSession s : sessions) {
            result.add(mapToDto(s, resolveWorkItem(s.getWorkItemId())));
        }
        return result;
    }

    private WorkItem resolveWorkItem(Long workItemId) {
        if (workItemId == null) return null;
        return workItemRepository.findById(workItemId).orElse(null);
    }

    private long elapsedSeconds(FocusSession session) {
        long base = session.getAccumulatedSeconds();
        if (session.getStatus() == FocusSessionStatus.RUNNING && session.getLastResumeAt() != null) {
            base += Duration.between(session.getLastResumeAt(), LocalDateTime.now()).getSeconds();
        }
        return Math.max(0, base);
    }

    private FocusSessionDto mapToDto(FocusSession session, WorkItem workItem) {
        return FocusSessionDto.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .workItemId(session.getWorkItemId())
                .workItemTicketNumber(workItem != null ? workItem.getTicketNumber() : null)
                .workItemTitle(workItem != null ? workItem.getTitle() : null)
                .projectId(workItem != null && workItem.getProject() != null ? workItem.getProject().getId() : null)
                .status(session.getStatus())
                .elapsedSeconds(elapsedSeconds(session))
                .accumulatedSeconds(session.getAccumulatedSeconds())
                .startedAt(session.getStartedAt())
                .description(session.getDescription())
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