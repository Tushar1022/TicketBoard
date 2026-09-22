package com.aurionpro.ticketboard.user.repository;

import com.aurionpro.ticketboard.user.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
    List<UserCredential> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
