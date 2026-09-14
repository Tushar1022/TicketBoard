package com.aurionpro.ticketboard.user.controller;

import com.aurionpro.ticketboard.common.response.ApiResponse;
import com.aurionpro.ticketboard.user.dto.PermissionDto;
import com.aurionpro.ticketboard.user.dto.RoleDto;
import com.aurionpro.ticketboard.user.dto.RolePermissionUpdateDto;
import com.aurionpro.ticketboard.user.service.RolePermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RolePermissionService rolePermissionService;

    @GetMapping
    @PreAuthorize("hasAuthority('admin:roles-permissions')")
    public ResponseEntity<ApiResponse<List<RoleDto>>> getAllRoles() {
        List<RoleDto> roles = rolePermissionService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.ok("Roles fetched successfully", roles));
    }

    @GetMapping("/{id:[0-9]+}")
    @PreAuthorize("hasAuthority('admin:roles-permissions')")
    public ResponseEntity<ApiResponse<RoleDto>> getRoleById(@PathVariable Long id) {
        RoleDto role = rolePermissionService.getRoleById(id);
        return ResponseEntity.ok(ApiResponse.ok("Role fetched successfully", role));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('admin:roles-permissions')")
    public ResponseEntity<ApiResponse<RoleDto>> updateRolePermissions(
            @PathVariable Long id,
            @Valid @RequestBody RolePermissionUpdateDto dto) {
        RoleDto role = rolePermissionService.updateRolePermissions(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Role permissions updated successfully", role));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('admin:roles-permissions')")
    public ResponseEntity<ApiResponse<List<PermissionDto>>> getAllPermissions() {
        List<PermissionDto> permissions = rolePermissionService.getAllPermissions();
        return ResponseEntity.ok(ApiResponse.ok("Permissions fetched successfully", permissions));
    }
}