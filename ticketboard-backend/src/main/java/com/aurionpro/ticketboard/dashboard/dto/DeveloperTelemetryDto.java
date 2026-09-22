package com.aurionpro.ticketboard.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeveloperTelemetryDto {
    private String dbProduct;
    private String dbVersion;
    private double latencyMs;
    private String serverTime;
    private boolean wsEndpointAvailable;
}