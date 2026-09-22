package com.aurionpro.ticketboard.comment.repository;

import com.aurionpro.ticketboard.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByEntityTypeAndEntityIdOrderByCreatedAtAsc(String entityType, Long entityId);

    long countByEntityTypeAndEntityId(String entityType, Long entityId);

    void deleteByAuthorId(Long authorId);
}
