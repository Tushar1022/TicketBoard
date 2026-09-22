package com.aurionpro.ticketboard.focus.repository;

import com.aurionpro.ticketboard.focus.entity.FocusSession;
import com.aurionpro.ticketboard.focus.entity.FocusSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FocusSessionRepository extends JpaRepository<FocusSession, Long> {

    Optional<FocusSession> findFirstByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, FocusSessionStatus status);

    List<FocusSession> findByUserIdOrderByUpdatedAtDesc(Long userId);

    void deleteByUserId(Long userId);
}