package com.aurionpro.ticketboard.project.repository;

import com.aurionpro.ticketboard.project.entity.ProjectDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, Long> {
    List<ProjectDocument> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    @Modifying
    @Query("UPDATE ProjectDocument d SET d.uploadedBy = null WHERE d.uploadedBy.id = :userId")
    void detachUploadedBy(@Param("userId") Long userId);
}