package com.aurionpro.ticketboard.user.repository;

import com.aurionpro.ticketboard.user.entity.Role;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmployeeId(String employeeId);

    boolean existsByEmail(String email);

    boolean existsByEmployeeId(String employeeId);

    List<User> findByStatus(UserStatus status);

    List<User> findByDepartmentId(Long departmentId);

    List<User> findByTeamId(Long teamId);

    long countByRolesContaining(Role role);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = com.aurionpro.ticketboard.user.enums.RoleType.ROLE_DEVELOPER AND u.status = 'ACTIVE'")
    List<User> findAllActiveDevelopers();

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.status = 'ACTIVE'")
    List<User> findByRoleName(@Param("roleName") com.aurionpro.ticketboard.user.enums.RoleType roleName);

    List<User> findByRolesNameAndStatus(com.aurionpro.ticketboard.user.enums.RoleType roleName, UserStatus status);

    @Modifying
    @Query("UPDATE User u SET u.reportingManager = null WHERE u.reportingManager.id = :userId")
    void detachReportingManager(@Param("userId") Long userId);
}
