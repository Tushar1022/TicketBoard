package com.aurionpro.ticketboard.support.service;

import com.aurionpro.ticketboard.common.exception.ResourceNotFoundException;
import com.aurionpro.ticketboard.support.dto.*;
import com.aurionpro.ticketboard.support.entity.*;
import com.aurionpro.ticketboard.support.repository.SupportTicketRepository;
import com.aurionpro.ticketboard.support.repository.TicketCommentRepository;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SupportTicketServiceImpl implements SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final TicketCommentRepository commentRepository;
    private final UserRepository userRepository;

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
                .build();

        ticket.setCreatedBy(currentUserEmail);
        ticket.setUpdatedBy(currentUserEmail);

        SupportTicket saved = ticketRepository.save(ticket);
        return mapToDto(saved);
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
}
