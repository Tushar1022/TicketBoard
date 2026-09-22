package com.aurionpro.ticketboard.support.service;

import com.aurionpro.ticketboard.common.exception.ResourceNotFoundException;
import com.aurionpro.ticketboard.support.dto.CreateSupportAnnouncementRequest;
import com.aurionpro.ticketboard.support.dto.SupportAnnouncementDto;
import com.aurionpro.ticketboard.support.entity.SupportAnnouncement;
import com.aurionpro.ticketboard.support.repository.SupportAnnouncementRepository;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SupportAnnouncementServiceImpl implements SupportAnnouncementService {

    private final SupportAnnouncementRepository announcementRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public SupportAnnouncementDto getActiveAnnouncement() {
        return announcementRepository.findFirstByActiveTrueOrderByCreatedAtDesc()
                .map(this::mapToDto)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportAnnouncementDto> getAllAnnouncements() {
        return announcementRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public SupportAnnouncementDto createAnnouncement(CreateSupportAnnouncementRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        String postedByName = user != null ? (user.getFirstName() + " " + user.getLastName()) : "Super Admin";

        // If newly created announcement is active, deactivate existing active announcements to ensure single active spotlight
        if (Boolean.TRUE.equals(request.getActive())) {
            deactivateAllActive();
        }

        SupportAnnouncement announcement = SupportAnnouncement.builder()
                .title(request.getTitle())
                .message(request.getMessage())
                .type(request.getType())
                .active(request.getActive() != null ? request.getActive() : true)
                .postedByName(postedByName)
                .postedByEmail(userEmail)
                .build();

        SupportAnnouncement saved = announcementRepository.save(announcement);
        return mapToDto(saved);
    }

    @Override
    public SupportAnnouncementDto updateAnnouncement(Long id, CreateSupportAnnouncementRequest request, String userEmail) {
        SupportAnnouncement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Support announcement not found with id: " + id));

        if (Boolean.TRUE.equals(request.getActive()) && !Boolean.TRUE.equals(announcement.getActive())) {
            deactivateAllActive();
        }

        announcement.setTitle(request.getTitle());
        announcement.setMessage(request.getMessage());
        announcement.setType(request.getType());
        if (request.getActive() != null) {
            announcement.setActive(request.getActive());
        }

        SupportAnnouncement saved = announcementRepository.save(announcement);
        return mapToDto(saved);
    }

    @Override
    public SupportAnnouncementDto toggleActiveStatus(Long id) {
        SupportAnnouncement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Support announcement not found with id: " + id));

        boolean newStatus = !Boolean.TRUE.equals(announcement.getActive());
        if (newStatus) {
            deactivateAllActive();
        }
        announcement.setActive(newStatus);
        SupportAnnouncement saved = announcementRepository.save(announcement);
        return mapToDto(saved);
    }

    @Override
    public void deleteAnnouncement(Long id) {
        if (!announcementRepository.existsById(id)) {
            throw new ResourceNotFoundException("Support announcement not found with id: " + id);
        }
        announcementRepository.deleteById(id);
    }

    private void deactivateAllActive() {
        List<SupportAnnouncement> activeList = announcementRepository.findByActiveTrueOrderByCreatedAtDesc();
        for (SupportAnnouncement active : activeList) {
            active.setActive(false);
        }
        announcementRepository.saveAll(activeList);
    }

    private SupportAnnouncementDto mapToDto(SupportAnnouncement entity) {
        return SupportAnnouncementDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .type(entity.getType())
                .active(entity.getActive())
                .postedByName(entity.getPostedByName())
                .postedByEmail(entity.getPostedByEmail())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
