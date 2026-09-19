package com.aurionpro.ticketboard.support.service;

import com.aurionpro.ticketboard.support.dto.*;
import java.util.List;

public interface SupportTicketService {

    SupportTicketDto createTicket(CreateSupportTicketRequest request, String currentUserEmail);

    List<SupportTicketDto> getTicketsForUser(String userEmail);

    List<SupportTicketDto> getAllTicketsForAdmin();

    SupportTicketDto getTicketById(Long id);

    SupportTicketDto updateTicketStatus(Long id, UpdateTicketStatusRequest request, String adminEmail);

    TicketCommentDto addComment(Long ticketId, AddTicketCommentRequest request, String currentUserEmail);

    SupportTicketDto uploadAttachment(Long ticketId, org.springframework.web.multipart.MultipartFile file, String currentUserEmail);

    long getOpenTicketsCount();

    SupportStatsDto getStats();

    List<SupportTicketActivityDto> getActivityLogForTicket(Long ticketId);
}
