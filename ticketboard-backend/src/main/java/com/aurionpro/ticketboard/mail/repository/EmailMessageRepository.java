package com.aurionpro.ticketboard.mail.repository;

import com.aurionpro.ticketboard.mail.entity.EmailMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailMessageRepository extends JpaRepository<EmailMessage, Long> {

    List<EmailMessage> findByUserEmailAndFolderOrderByCreatedAtDesc(String userEmail, String folder);

    List<EmailMessage> findByUserEmailAndIsStarredTrueOrderByCreatedAtDesc(String userEmail);

    long countByUserEmailAndFolderAndIsReadFalse(String userEmail, String folder);

    long countByUserEmailAndFolder(String userEmail, String folder);

    Optional<EmailMessage> findByIdAndUserEmail(Long id, String userEmail);

    @Query("SELECT e FROM EmailMessage e WHERE e.userEmail = :userEmail AND " +
           "(LOWER(e.subject) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(e.senderEmail) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(e.senderName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(e.body) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY e.createdAt DESC")
    List<EmailMessage> searchMessages(@Param("userEmail") String userEmail, @Param("query") String query);
}
