package com.aurionpro.ticketboard.audit.service;

import com.aurionpro.ticketboard.audit.dto.ActivityLogDto;
import com.aurionpro.ticketboard.audit.entity.ActivityLog;
import com.aurionpro.ticketboard.audit.enums.TimelineEventType;
import com.aurionpro.ticketboard.audit.repository.ActivityLogRepository;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void logEvent(String entityType, Long entityId, String entityCode,
                         TimelineEventType eventType, String summary, String details,
                         String oldValue, String newValue) {
        try {
            User currentUser = null;
            String userFullName = "System";

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                currentUser = userRepository.findByEmail(auth.getName()).orElse(null);
                if (currentUser != null) {
                    userFullName = currentUser.getFullName();
                }
            }

            ActivityLog logEntry = ActivityLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .entityCode(entityCode)
                    .eventType(eventType)
                    .summary(summary)
                    .details(details)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .user(currentUser)
                    .userFullName(userFullName)
                    .timestamp(LocalDateTime.now())
                    .build();

            activityLogRepository.save(logEntry);
        } catch (Exception ex) {
            log.error("Error writing activity log: {}", ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<ActivityLogDto> getTimeline(String entityType, Long entityId) {
        return activityLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ActivityLogDto> getRecentActivities() {
        return activityLogRepository.findTop20ByOrderByTimestampDesc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ActivityLogDto> getAllLogs(Pageable pageable) {
        return activityLogRepository.findAllByOrderByTimestampDesc(pageable).map(this::mapToDto);
    }

    public ActivityLogDto mapToDto(ActivityLog log) {
        return ActivityLogDto.builder()
                .id(log.getId())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .entityCode(log.getEntityCode())
                .eventType(log.getEventType())
                .summary(log.getSummary())
                .details(log.getDetails())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .userId(log.getUser() != null ? log.getUser().getId() : null)
                .userFullName(log.getUserFullName())
                .timestamp(log.getTimestamp())
                .build();
    }
}
