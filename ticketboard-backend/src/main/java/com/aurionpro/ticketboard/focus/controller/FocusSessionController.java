package com.aurionpro.ticketboard.focus.controller;

import com.aurionpro.ticketboard.common.response.ApiResponse;
import com.aurionpro.ticketboard.focus.dto.FocusSessionDto;
import com.aurionpro.ticketboard.focus.dto.StartFocusSessionRequest;
import com.aurionpro.ticketboard.focus.service.FocusSessionService;
import com.aurionpro.ticketboard.timetracking.dto.TimeEntryDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/focus-sessions")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FocusSessionController {

    private final FocusSessionService focusSessionService;

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<FocusSessionDto>> start(
            @Valid @RequestBody StartFocusSessionRequest request) {
        FocusSessionDto session = focusSessionService.start(request.getWorkItemId(), request.getDescription());
        return ResponseEntity.ok(ApiResponse.ok("Focus session started", session));
    }

    @PostMapping("/pause")
    public ResponseEntity<ApiResponse<FocusSessionDto>> pause() {
        FocusSessionDto session = focusSessionService.pause();
        return ResponseEntity.ok(ApiResponse.ok("Focus session paused", session));
    }

    @PostMapping("/resume")
    public ResponseEntity<ApiResponse<FocusSessionDto>> resume() {
        FocusSessionDto session = focusSessionService.resume();
        return ResponseEntity.ok(ApiResponse.ok("Focus session resumed", session));
    }

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<FocusSessionDto>> reset() {
        FocusSessionDto session = focusSessionService.reset();
        return ResponseEntity.ok(ApiResponse.ok("Focus session reset", session));
    }

    @PostMapping("/log")
    public ResponseEntity<ApiResponse<TimeEntryDto>> logTime(@RequestBody(required = false) LogTimeBody body) {
        String description = body != null ? body.getDescription() : null;
        TimeEntryDto entry = focusSessionService.logTime(description);
        return ResponseEntity.ok(ApiResponse.ok("Focus time logged as entry", entry));
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<FocusSessionDto>> getCurrent() {
        FocusSessionDto session = focusSessionService.getCurrent();
        return ResponseEntity.ok(ApiResponse.ok("Current focus session", session));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<FocusSessionDto>>> getHistory() {
        List<FocusSessionDto> history = focusSessionService.getHistory();
        return ResponseEntity.ok(ApiResponse.ok("Focus session history", history));
    }

    private record LogTimeBody(String description) {
        public String getDescription() {
            return description;
        }
    }
}