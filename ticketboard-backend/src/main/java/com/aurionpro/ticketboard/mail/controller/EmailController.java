package com.aurionpro.ticketboard.mail.controller;

import com.aurionpro.ticketboard.common.response.ApiResponse;
import com.aurionpro.ticketboard.mail.dto.*;
import com.aurionpro.ticketboard.mail.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/mail")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<List<EmailMessageDto>>> getMessages(
            @RequestParam(required = false, defaultValue = "INBOX") String folder,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "false") Boolean starred,
            Authentication authentication) {
        String email = getEmail(authentication);
        List<EmailMessageDto> list = emailService.getMessages(email, folder, search, starred);
        return ResponseEntity.ok(ApiResponse.success("Email messages retrieved successfully", list));
    }

    @GetMapping("/messages/{id}")
    public ResponseEntity<ApiResponse<EmailMessageDto>> getMessageById(
            @PathVariable Long id,
            Authentication authentication) {
        String email = getEmail(authentication);
        EmailMessageDto message = emailService.getMessageById(id, email);
        return ResponseEntity.ok(ApiResponse.success("Email message retrieved successfully", message));
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<EmailMessageDto>> sendEmail(
            @Valid @RequestBody SendEmailRequest request,
            Authentication authentication) {
        String email = getEmail(authentication);
        request.setDraft(false);
        EmailMessageDto sent = emailService.sendOrSaveEmail(request, email);
        return ResponseEntity.ok(ApiResponse.success("Email sent successfully", sent));
    }

    @PostMapping("/drafts")
    public ResponseEntity<ApiResponse<EmailMessageDto>> saveDraft(
            @Valid @RequestBody SendEmailRequest request,
            Authentication authentication) {
        String email = getEmail(authentication);
        request.setDraft(true);
        EmailMessageDto draft = emailService.sendOrSaveEmail(request, email);
        return ResponseEntity.ok(ApiResponse.success("Draft saved successfully", draft));
    }

    @PatchMapping("/messages/{id}/folder")
    public ResponseEntity<ApiResponse<EmailMessageDto>> updateFolder(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            Authentication authentication) {
        String email = getEmail(authentication);
        String targetFolder = payload.getOrDefault("folder", "INBOX");
        EmailMessageDto updated = emailService.updateFolder(id, targetFolder, email);
        return ResponseEntity.ok(ApiResponse.success("Email moved to " + targetFolder, updated));
    }

    @PatchMapping("/messages/{id}/star")
    public ResponseEntity<ApiResponse<EmailMessageDto>> toggleStar(
            @PathVariable Long id,
            Authentication authentication) {
        String email = getEmail(authentication);
        EmailMessageDto updated = emailService.toggleStar(id, email);
        return ResponseEntity.ok(ApiResponse.success("Email star status updated", updated));
    }

    @PatchMapping("/messages/{id}/read")
    public ResponseEntity<ApiResponse<EmailMessageDto>> toggleRead(
            @PathVariable Long id,
            Authentication authentication) {
        String email = getEmail(authentication);
        EmailMessageDto updated = emailService.toggleRead(id, email);
        return ResponseEntity.ok(ApiResponse.success("Email read status updated", updated));
    }

    @DeleteMapping("/messages/{id}")
    public ResponseEntity<ApiResponse<String>> deleteMessage(
            @PathVariable Long id,
            Authentication authentication) {
        String email = getEmail(authentication);
        emailService.deleteMessage(id, email);
        return ResponseEntity.ok(ApiResponse.success("Email message deleted", "DELETED"));
    }

    @GetMapping("/counts")
    public ResponseEntity<ApiResponse<FolderCountsDto>> getFolderCounts(Authentication authentication) {
        String email = getEmail(authentication);
        FolderCountsDto counts = emailService.getFolderCounts(email);
        return ResponseEntity.ok(ApiResponse.success("Folder counts retrieved successfully", counts));
    }

    @PostMapping("/attachments")
    public ResponseEntity<ApiResponse<AttachmentDto>> uploadAttachment(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        String email = getEmail(authentication);
        AttachmentDto uploaded = emailService.uploadAttachment(file, email);
        return ResponseEntity.ok(ApiResponse.success("Attachment uploaded successfully", uploaded));
    }

    @GetMapping("/attachments/{id}/download")
    public ResponseEntity<InputStreamResource> downloadAttachment(
            @PathVariable Long id,
            Authentication authentication) throws IOException {
        String email = getEmail(authentication);
        Path file = emailService.downloadAttachment(id, email);
        String contentType = Files.probeContentType(file);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        String disposition = "attachment; filename*=UTF-8''"
                + URLEncoder.encode(file.getFileName().toString(), StandardCharsets.UTF_8).replace("+", "%20");
        InputStream in = Files.newInputStream(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.parseMediaType(contentType))
                .body(new InputStreamResource(in));
    }

    @GetMapping("/smtp-config")
    public ResponseEntity<ApiResponse<SmtpConfigDto>> getSmtpConfig(Authentication authentication) {
        String email = getEmail(authentication);
        SmtpConfigDto config = emailService.getSmtpConfig(email);
        return ResponseEntity.ok(ApiResponse.success("SMTP configuration retrieved successfully", config));
    }

    @PutMapping("/smtp-config")
    public ResponseEntity<ApiResponse<SmtpConfigDto>> saveSmtpConfig(
            @Valid @RequestBody SmtpConfigDto dto,
            Authentication authentication) {
        String email = getEmail(authentication);
        SmtpConfigDto saved = emailService.saveSmtpConfig(dto, email);
        return ResponseEntity.ok(ApiResponse.success("SMTP configuration saved successfully", saved));
    }

    @PostMapping("/smtp-config/test")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> testSmtpConfig(
            @Valid @RequestBody TestSmtpRequest request,
            Authentication authentication) {
        String email = getEmail(authentication);
        boolean success = emailService.testSmtpConnection(request, email);
        return ResponseEntity.ok(ApiResponse.success("SMTP test result", Map.of("success", success)));
    }

    private String getEmail(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return "admin@ticketboard.com";
    }
}
