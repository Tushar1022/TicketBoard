package com.aurionpro.ticketboard.release.repository;

import com.aurionpro.ticketboard.release.entity.ReleaseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReleaseItemRepository extends JpaRepository<ReleaseItem, Long> {
    List<ReleaseItem> findByReleaseId(Long releaseId);
}
