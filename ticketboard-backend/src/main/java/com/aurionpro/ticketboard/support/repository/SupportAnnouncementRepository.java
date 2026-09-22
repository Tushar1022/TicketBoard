package com.aurionpro.ticketboard.support.repository;

import com.aurionpro.ticketboard.support.entity.SupportAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportAnnouncementRepository extends JpaRepository<SupportAnnouncement, Long> {
    List<SupportAnnouncement> findByActiveTrueOrderByCreatedAtDesc();
    Optional<SupportAnnouncement> findFirstByActiveTrueOrderByCreatedAtDesc();
    List<SupportAnnouncement> findAllByOrderByCreatedAtDesc();
}
