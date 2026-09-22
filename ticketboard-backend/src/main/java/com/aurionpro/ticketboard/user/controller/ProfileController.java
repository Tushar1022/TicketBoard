package com.aurionpro.ticketboard.user.controller;

import com.aurionpro.ticketboard.common.response.ApiResponse;
import com.aurionpro.ticketboard.user.entity.UserCredential;
import com.aurionpro.ticketboard.user.entity.UserSession;
import com.aurionpro.ticketboard.user.repository.UserCredentialRepository;
import com.aurionpro.ticketboard.user.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/user/profile")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ProfileController {

    private final UserSessionRepository sessionRepository;
    private final UserCredentialRepository credentialRepository;

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<UserSession>>> getActiveSessions() {
        List<UserSession> list = sessionRepository.findAll();
        if (list.isEmpty()) {
            list = List.of(
                UserSession.builder().sessionCode("SES-01").device("macOS Sonoma (Chrome 128)").ipAddress("192.168.1.42").location("Mumbai, India").lastActive(LocalDateTime.now()).isCurrent(true).build(),
                UserSession.builder().sessionCode("SES-02").device("iOS 17.5 (TicketBoard Mobile App)").ipAddress("49.36.120.14").location("Mumbai, India").lastActive(LocalDateTime.now().minusHours(2)).isCurrent(false).build()
            );
        }
        return ResponseEntity.ok(ApiResponse.success("Active sessions retrieved", list));
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<ApiResponse<Void>> revokeSession(@PathVariable Long id) {
        sessionRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Session revoked successfully", null));
    }

    @GetMapping("/credentials")
    public ResponseEntity<ApiResponse<List<UserCredential>>> getCredentials() {
        List<UserCredential> list = credentialRepository.findAll();
        if (list.isEmpty()) {
            list = List.of(
                UserCredential.builder().credentialType("SSH_KEY").fingerprint("SHA256:4928x91024L...m892147").status("ACTIVE").build(),
                UserCredential.builder().credentialType("GPG_KEY").fingerprint("3AA8B901C9024D99").status("ACTIVE").build()
            );
        }
        return ResponseEntity.ok(ApiResponse.success("User credentials retrieved", list));
    }
}
