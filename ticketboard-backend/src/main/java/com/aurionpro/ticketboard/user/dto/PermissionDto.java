package com.aurionpro.ticketboard.user.dto;

import com.aurionpro.ticketboard.user.enums.PermissionCode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PermissionDto {
    private Long id;
    private String code;
    private String name;
    private String module;
    private String description;
}