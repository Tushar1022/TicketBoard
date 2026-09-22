package com.aurionpro.ticketboard.mail.repository;

import com.aurionpro.ticketboard.mail.entity.MailAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MailAttachmentRepository extends JpaRepository<MailAttachment, Long> {
    Optional<MailAttachment> findByIdAndUserEmail(Long id, String userEmail);
    List<MailAttachment> findByEmailMessageId(Long emailMessageId);
}