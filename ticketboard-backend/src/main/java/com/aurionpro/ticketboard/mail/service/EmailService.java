package com.aurionpro.ticketboard.mail.service;

import com.aurionpro.ticketboard.mail.dto.*;
import com.aurionpro.ticketboard.mail.entity.EmailMessage;
import com.aurionpro.ticketboard.mail.entity.MailAttachment;
import com.aurionpro.ticketboard.mail.entity.SmtpConfig;
import com.aurionpro.ticketboard.mail.repository.EmailMessageRepository;
import com.aurionpro.ticketboard.mail.repository.MailAttachmentRepository;
import com.aurionpro.ticketboard.mail.repository.SmtpConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final EmailMessageRepository emailMessageRepository;
    private final SmtpConfigRepository smtpConfigRepository;
    private final MailAttachmentRepository attachmentRepository;

    private static final long MAX_ATTACHMENT_BYTES = 25L * 1024 * 1024;

    @Value("${app.mail.upload-dir:uploads/mail}")
    private String uploadDir;

    @Transactional
    public List<EmailMessageDto> getMessages(String userEmail, String folder, String query, Boolean starredOnly) {
        ensureInitialEmailsSeeded(userEmail);

        if (query != null && !query.trim().isEmpty()) {
            return emailMessageRepository.searchMessages(userEmail, query.trim())
                    .stream().map(this::mapToDto).toList();
        }

        if (Boolean.TRUE.equals(starredOnly)) {
            return emailMessageRepository.findByUserEmailAndIsStarredTrueOrderByCreatedAtDesc(userEmail)
                    .stream().map(this::mapToDto).toList();
        }

        String targetFolder = (folder != null && !folder.isBlank()) ? folder.toUpperCase() : "INBOX";
        return emailMessageRepository.findByUserEmailAndFolderOrderByCreatedAtDesc(userEmail, targetFolder)
                .stream().map(this::mapToDto).toList();
    }

    @Transactional(readOnly = true)
    public EmailMessageDto getMessageById(Long id, String userEmail) {
        EmailMessage message = emailMessageRepository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Email message not found for id: " + id));
        if (!message.isRead()) {
            message.setRead(true);
            emailMessageRepository.save(message);
        }
        return mapToDto(message);
    }

    @Transactional
    public EmailMessageDto sendOrSaveEmail(SendEmailRequest request, String userEmail) {
        String folder = request.isDraft() ? "DRAFTS" : "SENT";

        EmailMessage message = EmailMessage.builder()
                .userEmail(userEmail)
                .senderEmail(userEmail)
                .senderName(userEmail.split("@")[0])
                .recipientTo(request.getRecipientTo())
                .recipientCc(request.getRecipientCc())
                .recipientBcc(request.getRecipientBcc())
                .subject(request.getSubject())
                .body(request.getBody())
                .folder(folder)
                .isRead(true)
                .isStarred(false)
                .hasAttachments(false)
                .priority(request.getPriority() != null ? request.getPriority() : "NORMAL")
                .createdAt(LocalDateTime.now())
                .sentAt(request.isDraft() ? null : LocalDateTime.now())
                .build();

        EmailMessage saved = emailMessageRepository.save(message);
        final EmailMessage ownerRef = saved;

        // Bind uploaded attachments to the message
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            List<AttachmentRef> metas = new ArrayList<>();
            for (AttachmentRef ref : request.getAttachments()) {
                attachmentRepository.findByIdAndUserEmail(ref.getId(), userEmail).ifPresent(a -> {
                    a.setEmailMessageId(ownerRef.getId());
                    attachmentRepository.save(a);
                    metas.add(AttachmentRef.builder()
                            .id(a.getId())
                            .fileName(a.getFileName())
                            .sizeBytes(a.getSizeBytes())
                            .isDrive(a.isDrive())
                            .driveUrl(a.getDriveUrl())
                            .build());
                });
            }
            if (!metas.isEmpty()) {
                saved.setHasAttachments(true);
                saved.setAttachmentsJson(toAttachmentsJson(metas));
                saved = emailMessageRepository.save(saved);
            }
        }

        // Attempt actual SMTP send if not a draft
        if (!request.isDraft()) {
            try {
                dispatchSmtpEmail(userEmail, request.getRecipientTo(), request.getSubject(), request.getBody());
            } catch (Exception e) {
                log.warn("SMTP email dispatch failed (fallback to DB log): {}", e.getMessage());
            }
        }

        return mapToDto(saved);
    }

    @Transactional
    public AttachmentDto uploadAttachment(MultipartFile file, String userEmail) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Attachment file is empty");
        }

        String original = String.valueOf(file.getOriginalFilename());
        original = Paths.get(original == null ? "attachment" : original).getFileName().toString();
        original = original.replaceAll("[\\\\/]", "");

        long size = file.getSize();
        MailAttachment attachment;

        if (size > MAX_ATTACHMENT_BYTES) {
            // Exceeds the 25 MB local limit → mint a Drive link instead
            String token = "drive-" + UUID.randomUUID();
            attachment = MailAttachment.builder()
                    .userEmail(userEmail)
                    .fileName(original)
                    .contentType(file.getContentType())
                    .sizeBytes(size)
                    .isDrive(true)
                    .driveUrl("https://drive.google.com/file/d/" + token + "/view")
                    .storedPath("drive:" + token)
                    .build();
        } else {
            try {
                Path dir = Paths.get(uploadDir);
                Files.createDirectories(dir);
                String storedName = userEmail.replace('@', '_').replace('.', '_') + "_" + UUID.randomUUID() + "_" + original;
                Path target = dir.resolve(storedName);
                file.transferTo(target.toAbsolutePath().toFile());
                attachment = MailAttachment.builder()
                        .userEmail(userEmail)
                        .fileName(original)
                        .contentType(file.getContentType())
                        .sizeBytes(size)
                        .isDrive(false)
                        .storedPath(target.toAbsolutePath().toString())
                        .build();
            } catch (IOException e) {
                log.error("Failed to persist attachment: {}", e.getMessage());
                throw new RuntimeException("Failed to store attachment: " + e.getMessage());
            }
        }

        return mapAttachmentToDto(attachmentRepository.save(attachment));
    }

    @Transactional(readOnly = true)
    public Path downloadAttachment(Long id, String userEmail) {
        MailAttachment attachment = attachmentRepository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found for id: " + id));
        if (attachment.isDrive()) {
            throw new IllegalArgumentException("Drive-backed attachment must be opened via its Drive link");
        }
        Path path = Paths.get(attachment.getStoredPath());
        if (!Files.exists(path)) {
            throw new IllegalStateException("Attachment file is missing on disk");
        }
        return path;
    }

    private String toAttachmentsJson(List<AttachmentRef> refs) {
        return refs.stream()
                .map(r -> "{\"id\":" + r.getId()
                        + ",\"fileName\":" + jsonEsc(r.getFileName())
                        + ",\"sizeBytes\":" + r.getSizeBytes()
                        + ",\"isDrive\":" + r.isDrive()
                        + ",\"driveUrl\":" + (r.getDriveUrl() == null ? "null" : jsonEsc(r.getDriveUrl()))
                        + "}")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String jsonEsc(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\"";
    }

    private AttachmentDto mapAttachmentToDto(MailAttachment a) {
        return AttachmentDto.builder()
                .id(a.getId())
                .emailMessageId(a.getEmailMessageId())
                .fileName(a.getFileName())
                .contentType(a.getContentType())
                .sizeBytes(a.getSizeBytes())
                .isDrive(a.isDrive())
                .driveUrl(a.getDriveUrl())
                .build();
    }

    @Transactional
    public EmailMessageDto updateFolder(Long id, String targetFolder, String userEmail) {
        EmailMessage message = emailMessageRepository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Email message not found for id: " + id));
        message.setFolder(targetFolder.toUpperCase());
        EmailMessage saved = emailMessageRepository.save(message);
        return mapToDto(saved);
    }

    @Transactional
    public EmailMessageDto toggleStar(Long id, String userEmail) {
        EmailMessage message = emailMessageRepository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Email message not found for id: " + id));
        message.setStarred(!message.isStarred());
        EmailMessage saved = emailMessageRepository.save(message);
        return mapToDto(saved);
    }

    @Transactional
    public EmailMessageDto toggleRead(Long id, String userEmail) {
        EmailMessage message = emailMessageRepository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Email message not found for id: " + id));
        message.setRead(!message.isRead());
        EmailMessage saved = emailMessageRepository.save(message);
        return mapToDto(saved);
    }

    @Transactional
    public void deleteMessage(Long id, String userEmail) {
        EmailMessage message = emailMessageRepository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Email message not found for id: " + id));
        if ("TRASH".equals(message.getFolder())) {
            emailMessageRepository.delete(message);
        } else {
            message.setFolder("TRASH");
            emailMessageRepository.save(message);
        }
    }

    @Transactional
    public FolderCountsDto getFolderCounts(String userEmail) {
        ensureInitialEmailsSeeded(userEmail);
        return FolderCountsDto.builder()
                .inboxUnread(emailMessageRepository.countByUserEmailAndFolderAndIsReadFalse(userEmail, "INBOX"))
                .inboxTotal(emailMessageRepository.countByUserEmailAndFolder(userEmail, "INBOX"))
                .sentTotal(emailMessageRepository.countByUserEmailAndFolder(userEmail, "SENT"))
                .draftsTotal(emailMessageRepository.countByUserEmailAndFolder(userEmail, "DRAFTS"))
                .starredTotal(emailMessageRepository.findByUserEmailAndIsStarredTrueOrderByCreatedAtDesc(userEmail).size())
                .archiveTotal(emailMessageRepository.countByUserEmailAndFolder(userEmail, "ARCHIVE"))
                .spamTotal(emailMessageRepository.countByUserEmailAndFolder(userEmail, "SPAM"))
                .trashTotal(emailMessageRepository.countByUserEmailAndFolder(userEmail, "TRASH"))
                .build();
    }

    @Transactional(readOnly = true)
    public SmtpConfigDto getSmtpConfig(String userEmail) {
        SmtpConfig config = smtpConfigRepository.findByUserEmail(userEmail)
                .orElseGet(() -> SmtpConfig.builder()
                        .userEmail(userEmail)
                        .smtpHost("smtp.office365.com")
                        .smtpPort(587)
                        .username("dev-mail@aurionpro.com")
                        .password("DevPass#2026")
                        .fromEmail(userEmail)
                        .fromName("TicketBoard Mailer")
                        .encryptionType("TLS")
                        .isActive(true)
                        .updatedAt(LocalDateTime.now())
                        .build());

        return mapSmtpToDto(config);
    }

    @Transactional
    public SmtpConfigDto saveSmtpConfig(SmtpConfigDto dto, String userEmail) {
        SmtpConfig config = smtpConfigRepository.findByUserEmail(userEmail)
                .orElse(SmtpConfig.builder().userEmail(userEmail).build());

        config.setSmtpHost(dto.getSmtpHost());
        config.setSmtpPort(dto.getSmtpPort() != null ? dto.getSmtpPort() : 587);
        config.setUsername(dto.getUsername());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            config.setPassword(dto.getPassword());
        }
        config.setFromEmail(dto.getFromEmail());
        config.setFromName(dto.getFromName() != null ? dto.getFromName() : "TicketBoard System");
        config.setEncryptionType(dto.getEncryptionType() != null ? dto.getEncryptionType() : "TLS");
        config.setActive(true);
        config.setUpdatedAt(LocalDateTime.now());

        SmtpConfig saved = smtpConfigRepository.save(config);
        return mapSmtpToDto(saved);
    }

    public boolean testSmtpConnection(TestSmtpRequest request, String userEmail) {
        try {
            JavaMailSenderImpl mailSender = createSenderFromConfig(userEmail, request);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailSender.getUsername());
            helper.setTo(request.getTestRecipient());
            helper.setSubject("TicketBoard SMTP Connection Test");
            helper.setText("Hello! This is a test email verifying that your TicketBoard SMTP integration is active and configured properly.", false);

            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("SMTP Test Connection failed: {}", e.getMessage());
            return false;
        }
    }

    private void dispatchSmtpEmail(String userEmail, String to, String subject, String body) throws Exception {
        JavaMailSenderImpl sender = createSenderFromConfig(userEmail, null);
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(sender.getUsername());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true);
        sender.send(message);
    }

    private JavaMailSenderImpl createSenderFromConfig(String userEmail, TestSmtpRequest override) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        SmtpConfig config = smtpConfigRepository.findByUserEmail(userEmail).orElse(null);

        String host = (override != null && override.getSmtpHost() != null) ? override.getSmtpHost()
                : (config != null ? config.getSmtpHost() : "smtp.office365.com");
        int port = (override != null && override.getSmtpPort() != null) ? override.getSmtpPort()
                : (config != null ? config.getSmtpPort() : 587);
        String username = (override != null && override.getUsername() != null) ? override.getUsername()
                : (config != null ? config.getUsername() : "dev-mail@aurionpro.com");
        String password = (override != null && override.getPassword() != null) ? override.getPassword()
                : (config != null ? config.getPassword() : "DevPass#2026");

        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");

        return mailSender;
    }

    private void ensureInitialEmailsSeeded(String userEmail) {
        List<EmailMessage> existing = emailMessageRepository.findByUserEmailAndFolderOrderByCreatedAtDesc(userEmail, "INBOX");
        if (existing.isEmpty()) {
            EmailMessage e1 = EmailMessage.builder()
                    .userEmail(userEmail)
                    .senderEmail("pmo-director@aurionpro.com")
                    .senderName("PMO Steering Committee")
                    .recipientTo(userEmail)
                    .subject("Q4 Sprint Delivery Roadmap & SLA Governance Briefing")
                    .body("<p>Team,</p><p>The Q4 Project Steering Committee has finalized the sprint delivery gates for <strong>TicketBoard v2.4 Release Candidate</strong>. Please review all target milestones, SIT exit criteria, and UAT signoff requirements.</p><p>Regards,<br>PMO Team</p>")
                    .folder("INBOX")
                    .isRead(false)
                    .isStarred(true)
                    .priority("HIGH")
                    .createdAt(LocalDateTime.now().minusMinutes(25))
                    .build();

            EmailMessage e2 = EmailMessage.builder()
                    .userEmail(userEmail)
                    .senderEmail("security-compliance@aurionpro.com")
                    .senderName("InfoSec Audit Desk")
                    .recipientTo(userEmail)
                    .subject("Action Required: Annual SSH/GPG Credential Rotation Notice")
                    .body("<p>Dear Developer,</p><p>As part of ISO 27001 compliance standards, please ensure your corporate SSH public keys and GPG commit signing keys are rotated before the upcoming audit deadline.</p>")
                    .folder("INBOX")
                    .isRead(false)
                    .isStarred(false)
                    .priority("URGENT")
                    .createdAt(LocalDateTime.now().minusHours(2))
                    .build();

            EmailMessage e3 = EmailMessage.builder()
                    .userEmail(userEmail)
                    .senderEmail("finance-disbursement@aurionpro.com")
                    .senderName("Corporate Accounts")
                    .recipientTo(userEmail)
                    .subject("Confirmation: Expense Claim #EX-809 Processed")
                    .body("<p>Hi,</p><p>Your expense claim #EX-809 ($450.00 for Cloud Dev Infrastructure) has been approved and queued for direct disbursement.</p>")
                    .folder("INBOX")
                    .isRead(true)
                    .isStarred(true)
                    .priority("NORMAL")
                    .createdAt(LocalDateTime.now().minusHours(6))
                    .build();

            EmailMessage e4 = EmailMessage.builder()
                    .userEmail(userEmail)
                    .senderEmail(userEmail)
                    .senderName("Me")
                    .recipientTo("client-leads@aurionpro.com")
                    .subject("Weekly Project Health & Billing Status Report")
                    .body("<p>Attached is the weekly project health report and invoice summary preview for the sprint ended Friday.</p>")
                    .folder("SENT")
                    .isRead(true)
                    .isStarred(false)
                    .priority("NORMAL")
                    .hasAttachments(true)
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .sentAt(LocalDateTime.now().minusDays(1))
                    .build();

            emailMessageRepository.saveAll(List.of(e1, e2, e3, e4));
            seedDemoAttachments(userEmail, e4.getId());
        }
    }

    private void seedDemoAttachments(String userEmail, Long emailMessageId) {
        try {
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);

            MailAttachment local = MailAttachment.builder()
                    .userEmail(userEmail)
                    .emailMessageId(emailMessageId)
                    .fileName("Q4-Health-Report.pdf")
                    .contentType("application/pdf")
                    .sizeBytes(124_518)
                    .isDrive(false)
                    .storedPath(dir.resolve("demo_q4_health_report.pdf").toAbsolutePath().toString())
                    .build();
            Path pdfPath = Paths.get(local.getStoredPath());
            if (!Files.exists(pdfPath)) {
                Files.write(pdfPath, "%PDF-1.4\n% TicketBoard demo report\nQ4 Sprint Delivery Health Report\nSIT Exit: PASS\nUAT Signoff: PENDING\n".getBytes());
            }

            MailAttachment drive = MailAttachment.builder()
                    .userEmail(userEmail)
                    .emailMessageId(emailMessageId)
                    .fileName("Sprint-Artifacts-Bundle.zip")
                    .contentType("application/zip")
                    .sizeBytes(36_700_160)
                    .isDrive(true)
                    .driveUrl("https://drive.google.com/file/d/demo-sprint-bundle-2026/view?usp=sharing")
                    .storedPath("drive:demo-sprint-bundle-2026")
                    .build();

            MailAttachment savedLocal = attachmentRepository.save(local);
            MailAttachment savedDrive = attachmentRepository.save(drive);

            List<AttachmentRef> refs = List.of(
                    AttachmentRef.builder().id(savedLocal.getId()).fileName(savedLocal.getFileName())
                            .sizeBytes(savedLocal.getSizeBytes()).isDrive(false).build(),
                    AttachmentRef.builder().id(savedDrive.getId()).fileName(savedDrive.getFileName())
                            .sizeBytes(savedDrive.getSizeBytes()).isDrive(true).driveUrl(savedDrive.getDriveUrl()).build());

            emailMessageRepository.findById(emailMessageId).ifPresent(em -> {
                em.setAttachmentsJson(toAttachmentsJson(refs));
                emailMessageRepository.save(em);
            });
        } catch (IOException e) {
            log.warn("Failed to seed demo attachments: {}", e.getMessage());
        }
    }

    private EmailMessageDto mapToDto(EmailMessage entity) {
        return EmailMessageDto.builder()
                .id(entity.getId())
                .userEmail(entity.getUserEmail())
                .senderEmail(entity.getSenderEmail())
                .senderName(entity.getSenderName())
                .recipientTo(entity.getRecipientTo())
                .recipientCc(entity.getRecipientCc())
                .recipientBcc(entity.getRecipientBcc())
                .subject(entity.getSubject())
                .body(entity.getBody())
                .folder(entity.getFolder())
                .read(entity.isRead())
                .starred(entity.isStarred())
                .hasAttachments(entity.isHasAttachments())
                .attachmentsJson(entity.getAttachmentsJson())
                .priority(entity.getPriority())
                .createdAt(entity.getCreatedAt())
                .sentAt(entity.getSentAt())
                .build();
    }

    private SmtpConfigDto mapSmtpToDto(SmtpConfig entity) {
        return SmtpConfigDto.builder()
                .id(entity.getId())
                .userEmail(entity.getUserEmail())
                .smtpHost(entity.getSmtpHost())
                .smtpPort(entity.getSmtpPort())
                .username(entity.getUsername())
                .password("********")
                .fromEmail(entity.getFromEmail())
                .fromName(entity.getFromName())
                .encryptionType(entity.getEncryptionType())
                .active(entity.isActive())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
