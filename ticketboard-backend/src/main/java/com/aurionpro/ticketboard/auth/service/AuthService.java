package com.aurionpro.ticketboard.auth.service;

import com.aurionpro.ticketboard.auth.dto.AuthResponse;
import com.aurionpro.ticketboard.auth.dto.ChangePasswordRequest;
import com.aurionpro.ticketboard.auth.dto.LoginRequest;
import com.aurionpro.ticketboard.auth.dto.RegisterRequest;
import com.aurionpro.ticketboard.common.exception.BadRequestException;
import com.aurionpro.ticketboard.common.exception.ResourceNotFoundException;
import com.aurionpro.ticketboard.security.CustomUserDetails;
import com.aurionpro.ticketboard.security.JwtTokenProvider;
import com.aurionpro.ticketboard.user.dto.UserDto;
import com.aurionpro.ticketboard.user.entity.Department;
import com.aurionpro.ticketboard.user.entity.Role;
import com.aurionpro.ticketboard.user.entity.Team;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.enums.RoleType;
import com.aurionpro.ticketboard.user.enums.UserStatus;
import com.aurionpro.ticketboard.user.repository.DepartmentRepository;
import com.aurionpro.ticketboard.user.repository.RoleRepository;
import com.aurionpro.ticketboard.user.repository.TeamRepository;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import com.aurionpro.ticketboard.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        return AuthResponse.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .user(userService.mapToDto(user))
                .build();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already taken!");
        }

        String employeeId = "EMP-" + (1000 + userRepository.count() + 1);

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId()).orElse(null);
        }

        Team team = null;
        if (request.getTeamId() != null) {
            team = teamRepository.findById(request.getTeamId()).orElse(null);
        }

        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(RoleType.ROLE_DEVELOPER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ROLE_DEVELOPER).description("Developer").build()));
        roles.add(userRole);

        User user = User.builder()
                .employeeId(employeeId)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .designation(request.getDesignation())
                .department(department)
                .team(team)
                .roles(roles)
                .status(UserStatus.ACTIVE)
                .dailyCapacityHours(8.0)
                .hourlyCost(50.0)
                .build();

        User savedUser = userRepository.save(user);

        CustomUserDetails userPrincipal = new CustomUserDetails(savedUser);
        String jwt = tokenProvider.generateTokenForUser(userPrincipal);

        return AuthResponse.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .user(userService.mapToDto(savedUser))
                .build();
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("No authenticated user found");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return userService.mapToDto(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password does not match");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
