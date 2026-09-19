package com.aurionpro.ticketboard.support.service;

import com.aurionpro.ticketboard.common.exception.ResourceNotFoundException;
import com.aurionpro.ticketboard.document.storage.DocumentStorageService;
import com.aurionpro.ticketboard.support.dto.*;
import com.aurionpro.ticketboard.support.entity.*;
import com.aurionpro.ticketboard.support.repository.SupportTicketActivityLogRepository;
import com.aurionpro.ticketboard.support.repository.SupportTicketRepository;
import com.aurionpro.ticketboard.support.repository.TicketCommentRepository;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SupportTicketServiceImpl implements SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final TicketCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final DocumentStorageService documentStorageService;
    private final SupportTicketActivityLogRepository activityLogRepository;

    @Override
    public SupportTicketDto createTicket(CreateSupportTicketRequest request, String currentUserEmail) {
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseGet(() -> null);

        Long nextId = ticketRepository.findMaxId();
        long newIdNumber = (nextId == null ? 1000L : nextId) + 1;
        String ticketCode = "SUP-" + newIdNumber;

        String userName = user != null ? (user.getFirstName() + " " + user.getLastName()) : "System User";
        Long userId = user != null ? user.getId() : 99L;

        SupportTicket ticket = SupportTicket.builder()
                .ticketCode(ticketCode)
                .subject(request.getSubject())
                .category(request.getCategory())
                .priority(request.getPriority())
                .targetRole(request.getTargetRole())
                .status(TicketStatus.OPEN)
                .createdById(userId)
                .createdByName(userName)
                .createdByEmail(currentUserEmail)
                .description(request.getDescription())
                .systemDiagnostics(request.getSystemDiagnostics() != null ? request.getSystemDiagnostics() : "")
                .customCategoryName(request.getCustomCategoryName())
                .projectId(request.getProjectId())
                .projectName(request.getProjectName())
                .moduleName(request.getModuleName())
                .build();

        ticket.setCreatedBy(currentUserEmail);
        ticket.setUpdatedBy(currentUserEmail);

        SupportTicket saved = ticketRepository.save(ticket);
        return mapToDto(saved);
    }

    @Override
    public SupportTicketDto uploadAttachment(Long ticketId, org.springframework.web.multipart.MultipartFile file, String currentUserEmail) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", "id", ticketId));

        String storedPath = documentStorageService.store(file, "support-tickets");
        String downloadUrl = "http://localhost:8080/uploads/" + storedPath;

        if (ticket.getAttachments() == null) {
            ticket.setAttachments(new java.util.ArrayList<>());
        }
        ticket.getAttachments().add(downloadUrl);
        ticket.setUpdatedBy(currentUserEmail);

        SupportTicket updated = ticketRepository.save(ticket);
        return mapToDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicketDto> getTicketsForUser(String userEmail) {
        return ticketRepository.findByCreatedByEmailOrderByCreatedAtDesc(userEmail)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicketDto> getAllTicketsForAdmin() {
        return ticketRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SupportTicketDto getTicketById(Long id) {
        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", "id", id));
        return mapToDto(ticket);
    }

    @Override
    public SupportTicketDto updateTicketStatus(Long id, UpdateTicketStatusRequest request, String adminEmail) {
        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", "id", id));

        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        String adminName = request.getAssignedToName() != null ? request.getAssignedToName() :
                (admin != null ? admin.getFirstName() + " " + admin.getLastName() : "Super Admin");

        ticket.setStatus(request.getStatus());
        ticket.setAssignedToName(adminName);
        if (admin != null) {
            ticket.setAssignedToId(admin.getId());
        }
        if (request.getResolutionNotes() != null) {
            ticket.setResolutionNotes(request.getResolutionNotes());
        }
        ticket.setUpdatedBy(adminEmail);

        SupportTicket updated = ticketRepository.save(ticket);
        return mapToDto(updated);
    }

    @Override
    public TicketCommentDto addComment(Long ticketId, AddTicketCommentRequest request, String currentUserEmail) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", "id", ticketId));

        User user = userRepository.findByEmail(currentUserEmail).orElse(null);
        String authorName = user != null ? user.getFirstName() + " " + user.getLastName() : "User";
        Long authorId = user != null ? user.getId() : 99L;
        String authorRole = user != null && user.getRoles() != null && !user.getRoles().isEmpty()
                ? user.getRoles().iterator().next().getName().name()
                : "USER";

        TicketComment comment = TicketComment.builder()
                .ticket(ticket)
                .authorId(authorId)
                .authorName(authorName)
                .authorRole(authorRole)
                .commentText(request.getCommentText())
                .build();

        comment.setCreatedBy(currentUserEmail);
        comment.setUpdatedBy(currentUserEmail);

        TicketComment saved = commentRepository.save(comment);

        ticket.getComments().add(saved);
        ticketRepository.save(ticket);

        return mapToCommentDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public long getOpenTicketsCount() {
        return ticketRepository.countByStatusIn(Arrays.asList(TicketStatus.OPEN, TicketStatus.IN_REVIEW));
    }

    @Override
    @Transactional(readOnly = true)
    public SupportStatsDto getStats() {
        Map<TicketStatus, Long> byStatus = new EnumMap<>(TicketStatus.class);
        for (TicketStatus status : TicketStatus.values()) {
            byStatus.put(status, ticketRepository.countByStatus(status));
        }

        Map<TicketPriority, Long> byPriority = new EnumMap<>(TicketPriority.class);
        for (TicketPriority priority : TicketPriority.values()) {
            byPriority.put(priority, ticketRepository.countByPriority(priority));
        }

        Map<SupportCategory, Long> byCategory = new EnumMap<>(SupportCategory.class);
        for (SupportCategory category : SupportCategory.values()) {
            byCategory.put(category, ticketRepository.countByCategory(category));
        }

        Map<String, Long> byTargetRole = new java.util.LinkedHashMap<>();
        byTargetRole.put("ROLE_SUPER_ADMIN", ticketRepository.countByTargetRole("ROLE_SUPER_ADMIN"));
        byTargetRole.put("ROLE_ADMIN", ticketRepository.countByTargetRole("ROLE_ADMIN"));

        return SupportStatsDto.builder()
                .totalTickets(ticketRepository.count())
                .openTickets(byStatus.getOrDefault(TicketStatus.OPEN, 0L))
                .inReviewTickets(byStatus.getOrDefault(TicketStatus.IN_REVIEW, 0L))
                .resolvedTickets(byStatus.getOrDefault(TicketStatus.RESOLVED, 0L))
                .closedTickets(byStatus.getOrDefault(TicketStatus.CLOSED, 0L))
                .urgentTickets(byPriority.getOrDefault(TicketPriority.URGENT, 0L))
                .highPriorityTickets(byPriority.getOrDefault(TicketPriority.HIGH, 0L))
                .mediumPriorityTickets(byPriority.getOrDefault(TicketPriority.MEDIUM, 0L))
                .lowPriorityTickets(byPriority.getOrDefault(TicketPriority.LOW, 0L))
                .unassignedTickets(ticketRepository.countByAssignedToIdIsNull())
                .byStatus(byStatus)
                .byPriority(byPriority)
                .byCategory(byCategory)
                .byTargetRole(byTargetRole)
                .build();
    }

    private SupportTicketDto mapToDto(SupportTicket t) {
        List<TicketCommentDto> commentDtos = t.getComments() != null ?
                t.getComments().stream().map(this::mapToCommentDto).collect(Collectors.toList()) :
                List.of();

        return SupportTicketDto.builder()
                .id(t.getId())
                .ticketCode(t.getTicketCode())
                .subject(t.getSubject())
                .category(t.getCategory())
                .priority(t.getPriority())
                .targetRole(t.getTargetRole())
                .status(t.getStatus())
                .createdById(t.getCreatedById())
                .createdByName(t.getCreatedByName())
                .createdByEmail(t.getCreatedByEmail())
                .assignedToId(t.getAssignedToId())
                .assignedToName(t.getAssignedToName())
                .description(t.getDescription())
                .resolutionNotes(t.getResolutionNotes())
                .systemDiagnostics(t.getSystemDiagnostics())
                .customCategoryName(t.getCustomCategoryName())
                .projectId(t.getProjectId())
                .projectName(t.getProjectName())
                .moduleName(t.getModuleName())
                .attachments(t.getAttachments() != null ? new java.util.ArrayList<>(t.getAttachments()) : List.of())
                .comments(commentDtos)
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private TicketCommentDto mapToCommentDto(TicketComment c) {
        return TicketCommentDto.builder()
                .id(c.getId())
                .ticketId(c.getTicket() != null ? c.getTicket().getId() : null)
                .authorId(c.getAuthorId())
                .authorName(c.getAuthorName())
                .authorRole(c.getAuthorRole())
                .commentText(c.getCommentText())
                .createdAt(c.getCreatedAt())
                .build();
    }

    // ═══ Phase 3.1: ticket ACTIVITY — now real (write + read) ═══

    /**
     * Append an immutable activity-row reflecting the just-applied change.
     * Only rows that carry a USER-VISIBLE "what changed" meaning are written
     * (status/priority transition, assignment, colocated comment, attachment);
     * this is what feeds GET /{id}/history and the admin/user timeline.
     */
    private void recordActivity(SupportTicketActivityLog log) {
        activityLogRepository.save(log);
    }

    public List<SupportTicketActivityDto> getActivityLogForTicket(Long ticketId) {
        return activityLogRepository.findByTicketIdOrderByOccurredAtDesc(ticketId)
                .stream()
                .map(this::mapToActivityDto)
                .collect(java.util.stream.Collectors.toList());
    }

    private SupportTicketActivityDto mapToActivityDto(SupportTicketActivityLog a) {
        return SupportTicketActivityDto.builder()
                .id(a.getId())
                .ticketIdMonad(a.getTicketId())
                .actorName(a.getActorName())
                .actorRole(a.getActorRole())
                .statusBefore(a.getStatusBefore())
                .statusAfter(a.getStatusAfter())
                .priorityBefore(a.getPriorityBefore())
                .priorityAfter(a.getPriorityAfter())
                .assignmentBefore(a.getAssignedToBefore())
                .assignmentAfter(a.getAssignedToAfter())
                .actionType(a.getActionType())
                .summary(a.getSummary())
                .detail(a.getDetail())
                .occurredAt(a.getOccurredAt())
                .build();
    }
}
