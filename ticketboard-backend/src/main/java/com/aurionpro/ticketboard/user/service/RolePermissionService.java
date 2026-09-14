package com.aurionpro.ticketboard.user.service;

import com.aurionpro.ticketboard.common.exception.ResourceNotFoundException;
import com.aurionpro.ticketboard.user.dto.PermissionDto;
import com.aurionpro.ticketboard.user.dto.RoleDto;
import com.aurionpro.ticketboard.user.dto.RolePermissionUpdateDto;
import com.aurionpro.ticketboard.user.entity.Permission;
import com.aurionpro.ticketboard.user.entity.Role;
import com.aurionpro.ticketboard.user.enums.PermissionCode;
import com.aurionpro.ticketboard.user.enums.RoleType;
import com.aurionpro.ticketboard.user.repository.PermissionRepository;
import com.aurionpro.ticketboard.user.repository.RoleRepository;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<RoleDto> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoleDto getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));
        return mapToDto(role);
    }

    @Transactional(readOnly = true)
    public List<PermissionDto> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .sorted(Comparator.comparing(Permission::getModule).thenComparing(Permission::getCode))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public RoleDto updateRolePermissions(Long roleId, RolePermissionUpdateDto dto) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        if (role.getName() == RoleType.ROLE_SUPER_ADMIN) {
            throw new com.aurionpro.ticketboard.common.exception.BadRequestException("Super Admin permissions are fixed and cannot be modified");
        }

        Set<Permission> permissions = new HashSet<>();
        if (dto.getPermissions() != null) {
            for (String codeStr : dto.getPermissions()) {
                PermissionCode code;
                try {
                    code = PermissionCode.fromCode(codeStr);
                } catch (IllegalArgumentException ex) {
                    throw new com.aurionpro.ticketboard.common.exception.BadRequestException("Unknown permission code: " + codeStr);
                }
                Permission permission = permissionRepository.findByCode(code)
                        .orElseThrow(() -> new ResourceNotFoundException("Permission", "code", codeStr));
                permissions.add(permission);
            }
        }
        role.setPermissions(permissions);
        return mapToDto(roleRepository.save(role));
    }

    private RoleDto mapToDto(Role role) {
        Set<String> permissionCodes = role.getPermissions().stream()
                .map(p -> p.getCode().getCode())
                .collect(Collectors.toSet());

        return RoleDto.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissions(permissionCodes)
                .userCount(Math.toIntExact(userRepository.countByRolesContaining(role)))
                .build();
    }

    private PermissionDto mapToDto(Permission permission) {
        return PermissionDto.builder()
                .id(permission.getId())
                .code(permission.getCode().getCode())
                .name(permission.getName())
                .module(permission.getModule())
                .description(permission.getDescription())
                .build();
    }
}