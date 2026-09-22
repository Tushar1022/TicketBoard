package com.aurionpro.ticketboard.project.repository;

import com.aurionpro.ticketboard.project.entity.MilestoneNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MilestoneNoteRepository extends JpaRepository<MilestoneNote, Long> {

    List<MilestoneNote> findByMilestoneIdOrderByPinnedDescCreatedAtDesc(Long milestoneId);

    long countByMilestoneId(Long milestoneId);

    @Modifying
    @Query("DELETE FROM MilestoneNote n WHERE n.milestone.id = :milestoneId")
    void deleteByMilestoneId(@Param("milestoneId") Long milestoneId);
}