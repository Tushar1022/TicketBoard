package com.aurionpro.ticketboard.config;

import com.aurionpro.ticketboard.websocket.WorkspaceWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WorkspaceWebSocketConfig implements WebSocketConfigurer {

    private final WorkspaceWebSocketHandler workspaceWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(workspaceWebSocketHandler, "/ws-workspace")
                .setAllowedOriginPatterns("*");
    }
}