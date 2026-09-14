package com.aurionpro.ticketboard.capacity.controller;

import com.aurionpro.ticketboard.capacity.dto.EmployeeWorkloadDto;
import com.aurionpro.ticketboard.capacity.dto.ProjectForecastDto;
import com.aurionpro.ticketboard.capacity.dto.RequirementForecastDto;
import com.aurionpro.ticketboard.capacity.dto.TeamCapacityDto;
import com.aurionpro.ticketboard.capacity.service.CapacityPlanningService;
import com.aurionpro.ticketboard.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/capacity")
@PreAuthorize("hasAnyAuthority('capacity:view', 'admin:access')")
@RequiredArgsConstructor
public class CapacityController {

    private final CapacityPlanningService capacityService;

    @GetMapping("/workload")
    public ResponseEntity<ApiResponse<List<EmployeeWorkloadDto>>> getEmployeeWorkloads() {
        List<EmployeeWorkloadDto> workloads = capacityService.getAllEmployeeWorkloads();
        return ResponseEntity.ok(ApiResponse.ok("Employee workloads fetched successfully", workloads));
    }

    @GetMapping("/teams")
    public ResponseEntity<ApiResponse<List<TeamCapacityDto>>> getTeamCapacities() {
        List<TeamCapacityDto> teams = capacityService.getTeamCapacities();
        return ResponseEntity.ok(ApiResponse.ok("Team capacities fetched successfully", teams));
    }

    @GetMapping("/projections")
    public ResponseEntity<ApiResponse<List<ProjectForecastDto>>> getProjectProjections() {
        List<ProjectForecastDto> forecasts = capacityService.getProjectDeliveryForecasts();
        return ResponseEntity.ok(ApiResponse.ok("Project delivery forecasts fetched successfully", forecasts));
    }

    @GetMapping("/demand-forecast")
    public ResponseEntity<ApiResponse<List<RequirementForecastDto>>> getDemandForecast() {
        List<RequirementForecastDto> forecast = capacityService.getRequirementForecasts();
        return ResponseEntity.ok(ApiResponse.ok("Requirement demand forecast fetched successfully", forecast));
    }
}
