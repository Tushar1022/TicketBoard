package com.aurionpro.ticketboard.devtools.repository;

import com.aurionpro.ticketboard.devtools.entity.DeveloperSnippet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeveloperSnippetRepository extends JpaRepository<DeveloperSnippet, Long> {
    List<DeveloperSnippet> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
