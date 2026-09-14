package com.aurionpro.ticketboard.security;

import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.enums.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String employeeId;
    private final String email;
    private final String password;
    private final String fullName;
    private final UserStatus status;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.employeeId = user.getEmployeeId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.fullName = user.getFullName();
        this.status = user.getStatus();

        Set<GrantedAuthority> authSet = new HashSet<>();
        user.getRoles().forEach(role -> {
            authSet.add(new SimpleGrantedAuthority(role.getName().name()));
            if (role.getPermissions() != null) {
                role.getPermissions().forEach(perm ->
                        authSet.add(new SimpleGrantedAuthority(perm.getCode().getCode())));
            }
        });
        this.authorities = authSet;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}