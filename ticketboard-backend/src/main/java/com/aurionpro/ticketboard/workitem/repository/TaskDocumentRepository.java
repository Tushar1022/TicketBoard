package com.aurionpro.ticketboard.workitem.repository;

import com.aurionpro.ticketboard.workitem.entity.TaskDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskDocumentRepository extends JpaRepository<TaskDocument, Long> {
    List<TaskDocument> findByWorkItemIdOrderByCreatedAtDesc(Long workItemId);
}
