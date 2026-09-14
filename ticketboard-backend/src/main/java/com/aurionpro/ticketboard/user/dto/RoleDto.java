package com.aurionpro.ticketboard.user.dto;

import com.aurionpro.ticketboard.user.enums.RoleType;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class RoleDto {
    private Long id;
    private RoleType name;
    private String description;
    private Set<String> permissions;
    private Integer userCount;
}