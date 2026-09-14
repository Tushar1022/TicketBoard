package com.aurionpro.ticketboard.user.dto;

import lombok.Data;

import java.util.Set;

@Data
public class RolePermissionUpdateDto {
    private Set<String> permissions;
}