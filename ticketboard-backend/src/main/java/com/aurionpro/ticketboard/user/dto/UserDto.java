package com.aurionpro.ticketboard.user.dto;

import com.aurionpro.ticketboard.user.enums.RoleType;
import com.aurionpro.ticketboard.user.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String employeeId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String designation;
    private Long departmentId;
    private String departmentName;
    private Long teamId;
    private String teamName;
    private Long managerId;
    private String managerName;
    private Set<RoleType> roles;
    private Set<String> permissions;
    private String skills;
    private Double dailyCapacityHours;
    private Double hourlyCost;
    private UserStatus status;
    private LocalDate joiningDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
