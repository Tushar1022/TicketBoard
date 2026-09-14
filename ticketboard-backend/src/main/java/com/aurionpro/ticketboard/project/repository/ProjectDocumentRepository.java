package com.aurionpro.ticketboard.project.repository;

import com.aurionpro.ticketboard.project.entity.ProjectDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, Long> {
    List<ProjectDocument> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}