package com.aurionpro.ticketboard.notification.controller;

import com.aurionpro.ticketboard.common.response.ApiResponse;
import com.aurionpro.ticketboard.notification.dto.CreateNotificationRequest;
import com.aurionpro.ticketboard.notification.dto.NotificationDto;
import com.aurionpro.ticketboard.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getMyNotifications(Authentication authentication) {
        String email = getEmail(authentication);
        List<NotificationDto> list = notificationService.getNotificationsForUser(email);
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", list));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(Authentication authentication) {
        String email = getEmail(authentication);
        long count = notificationService.getUnreadCount(email);
        return ResponseEntity.ok(ApiResponse.success("Unread notification count retrieved", Map.of("unreadCount", count)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationDto>> markAsRead(@PathVariable Long id, Authentication authentication) {
        String email = getEmail(authentication);
        NotificationDto updated = notificationService.markAsRead(id, email);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", updated));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<String>> markAllAsRead(Authentication authentication) {
        String email = getEmail(authentication);
        notificationService.markAllAsRead(email);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", "SUCCESS"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationDto>> createNotification(
            @Valid @RequestBody CreateNotificationRequest request,
            Authentication authentication) {
        String email = getEmail(authentication);
        NotificationDto created = notificationService.createNotification(request, email);
        return ResponseEntity.ok(ApiResponse.success("Notification created successfully", created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteNotification(@PathVariable Long id, Authentication authentication) {
        String email = getEmail(authentication);
        notificationService.deleteNotification(id, email);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted successfully", "DELETED"));
    }

    private String getEmail(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return "admin@ticketboard.com";
    }
}
