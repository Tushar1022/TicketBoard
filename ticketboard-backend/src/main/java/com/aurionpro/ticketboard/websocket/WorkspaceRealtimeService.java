package com.aurionpro.ticketboard.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WorkspaceRealtimeService {

    private final WorkspaceWebSocketHandler handler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void notifyFocusSession(Long userId, String type, Object session) {
        broadcast(userId, Map.of(
                "type", type,
                "topic", WorkspaceWebSocketHandler.TOPIC,
                "data", session,
                "at", LocalDateTime.now().toString()
        ));
    }

    public void notifyTelemetry(Long userId, Object telemetry) {
        broadcast(userId, Map.of(
                "type", "TELEMETRY",
                "topic", WorkspaceWebSocketHandler.TOPIC,
                "data", telemetry,
                "at", LocalDateTime.now().toString()
        ));
    }

    public void notifyDashboardSync(Long userId) {
        broadcast(userId, Map.of(
                "type", "DASHBOARD_SYNC",
                "topic", WorkspaceWebSocketHandler.TOPIC,
                "at", LocalDateTime.now().toString()
        ));
    }

    private void broadcast(Long userId, Map<String, Object> payload) {
        if (userId == null) return;
        try {
            String json = objectMapper.writeValueAsString(payload);
            handler.broadcastRaw(userId, json);
        } catch (Exception ignored) {
        }
    }
}