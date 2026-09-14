package com.aurionpro.ticketboard.user.dto;

import com.aurionpro.ticketboard.user.enums.RoleType;
import com.aurionpro.ticketboard.user.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateDto {

    private String employeeId;

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    private String password;

    private String phone;
    private String designation;
    private Long departmentId;
    private Long teamId;
    private Long managerId;
    private Set<RoleType> roles;
    private String skills;
    private Double dailyCapacityHours;
    private Double hourlyCost;
    private UserStatus status;
    private LocalDate joiningDate;
}
