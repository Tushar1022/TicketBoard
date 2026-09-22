package com.aurionpro.ticketboard.workitem.repository;

import com.aurionpro.ticketboard.workitem.entity.TaskDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskDocumentRepository extends JpaRepository<TaskDocument, Long> {
    List<TaskDocument> findByWorkItemIdOrderByCreatedAtDesc(Long workItemId);

    @Modifying
    @Query("UPDATE TaskDocument d SET d.uploadedBy = null WHERE d.uploadedBy.id = :userId")
    void detachUploadedBy(@Param("userId") Long userId);
}
