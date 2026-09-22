package com.aurionpro.ticketboard.user.repository;

import com.aurionpro.ticketboard.user.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    List<UserSession> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
