package com.aurionpro.ticketboard.user.repository;

import com.aurionpro.ticketboard.user.entity.Permission;
import com.aurionpro.ticketboard.user.enums.PermissionCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByCode(PermissionCode code);
    List<Permission> findByModule(String module);
    boolean existsByCode(PermissionCode code);
}
