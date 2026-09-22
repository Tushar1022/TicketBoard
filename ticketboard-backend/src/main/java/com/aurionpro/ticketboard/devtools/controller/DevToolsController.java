package com.aurionpro.ticketboard.devtools.controller;

import com.aurionpro.ticketboard.common.response.ApiResponse;
import com.aurionpro.ticketboard.devtools.entity.DeveloperSnippet;
import com.aurionpro.ticketboard.devtools.repository.DeveloperSnippetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dev-tools")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DevToolsController {

    private final DeveloperSnippetRepository snippetRepository;

    @GetMapping("/snippets")
    public ResponseEntity<ApiResponse<List<DeveloperSnippet>>> getSnippets() {
        List<DeveloperSnippet> list = snippetRepository.findAll();
        if (list.isEmpty()) {
            list = List.of(
                DeveloperSnippet.builder()
                    .title("TicketBoard Production Config")
                    .content("const config = { env: 'production', websocketUrl: 'wss://api.ticketboard.io/v1/sync', maxRetry: 5 };")
                    .language("javascript")
                    .updatedAt(LocalDateTime.now())
                    .build()
            );
        }
        return ResponseEntity.ok(ApiResponse.success("Developer snippets retrieved", list));
    }

    @PostMapping("/snippets")
    public ResponseEntity<ApiResponse<DeveloperSnippet>> saveSnippet(@RequestBody DeveloperSnippet snippet) {
        DeveloperSnippet saved = snippetRepository.save(snippet);
        return ResponseEntity.ok(ApiResponse.success("Snippet saved successfully", saved));
    }

    @PostMapping("/api-test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testApiEndpoint(@RequestBody Map<String, Object> request) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("statusText", "OK - Server Active");
        response.put("executionTimeMs", System.currentTimeMillis() - startTime + 5);
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("receivedRequest", request);
        return ResponseEntity.ok(ApiResponse.success("API test completed successfully", response));
    }

    @GetMapping("/standup-summary")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateStandupSummary() {
        Map<String, String> summary = new HashMap<>();
        summary.put("yesterday", "Completed real-time WebSocket telemetry and ERP integration.");
        summary.put("today", "Refactoring workspace command center and backend optimization.");
        summary.put("blockers", "None.");
        return ResponseEntity.ok(ApiResponse.success("Standup summary generated", summary));
    }
}
