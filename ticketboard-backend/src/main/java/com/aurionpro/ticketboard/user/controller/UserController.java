package com.aurionpro.ticketboard.user.controller;

import com.aurionpro.ticketboard.common.response.ApiResponse;
import com.aurionpro.ticketboard.user.dto.UserCreateDto;
import com.aurionpro.ticketboard.user.dto.UserDto;
import com.aurionpro.ticketboard.user.enums.RoleType;
import com.aurionpro.ticketboard.user.enums.UserStatus;
import com.aurionpro.ticketboard.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers(
            @RequestParam(required = false) RoleType role,
            @RequestParam(required = false) com.aurionpro.ticketboard.user.enums.UserStatus status) {
        List<UserDto> users = userService.getUsersByRoleAndStatus(role, status);
        return ResponseEntity.ok(ApiResponse.ok("Users fetched successfully", users));
    }

    @GetMapping("/developers")
    public ResponseEntity<ApiResponse<List<UserDto>>> getDevelopers() {
        List<UserDto> developers = userService.getDevelopers();
        return ResponseEntity.ok(ApiResponse.ok("Developers fetched successfully", developers));
    }

    @GetMapping("/project-owners")
    public ResponseEntity<ApiResponse<List<UserDto>>> getProjectOwners() {
        List<UserDto> owners = userService.getProjectOwners();
        return ResponseEntity.ok(ApiResponse.ok("Project owners fetched successfully", owners));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
        UserDto user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.ok("User fetched successfully", user));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody UserCreateDto createDto) {
        UserDto user = userService.createUser(createDto);
        return ResponseEntity.ok(ApiResponse.ok("User created successfully", user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:edit')")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(@PathVariable Long id, @Valid @RequestBody UserCreateDto createDto) {
        UserDto user = userService.updateUser(id, createDto);
        return ResponseEntity.ok(ApiResponse.ok("User updated successfully", user));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('user:manage-status')")
    public ResponseEntity<ApiResponse<Void>> toggleStatus(@PathVariable Long id, @RequestParam com.aurionpro.ticketboard.user.enums.UserStatus status) {
        userService.toggleUserStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("User status updated successfully", null));
    }

    @PatchMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('user:edit')")
    public ResponseEntity<ApiResponse<UserDto>> updateRoles(@PathVariable Long id, @RequestBody Set<RoleType> roles) {
        UserDto updated = userService.updateUserRoles(id, roles);
        return ResponseEntity.ok(ApiResponse.ok("User roles updated successfully", updated));
    }
}
