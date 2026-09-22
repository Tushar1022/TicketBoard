package com.aurionpro.ticketboard.audit.repository;

import com.aurionpro.ticketboard.audit.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, Long entityId);

    List<ActivityLog> findByEntityCodeOrderByTimestampDesc(String entityCode);

    List<ActivityLog> findTop20ByOrderByTimestampDesc();

    Page<ActivityLog> findAllByOrderByTimestampDesc(Pageable pageable);

    @Modifying
    @Query("UPDATE ActivityLog a SET a.user = null WHERE a.user.id = :userId")
    void detachUser(@Param("userId") Long userId);
}
