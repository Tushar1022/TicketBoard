package com.aurionpro.ticketboard.support.service;

import com.aurionpro.ticketboard.support.dto.CreateSupportAnnouncementRequest;
import com.aurionpro.ticketboard.support.dto.SupportAnnouncementDto;

import java.util.List;

public interface SupportAnnouncementService {
    SupportAnnouncementDto getActiveAnnouncement();
    List<SupportAnnouncementDto> getAllAnnouncements();
    SupportAnnouncementDto createAnnouncement(CreateSupportAnnouncementRequest request, String userEmail);
    SupportAnnouncementDto updateAnnouncement(Long id, CreateSupportAnnouncementRequest request, String userEmail);
    SupportAnnouncementDto toggleActiveStatus(Long id);
    void deleteAnnouncement(Long id);
}
