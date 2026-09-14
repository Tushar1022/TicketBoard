package com.aurionpro.ticketboard.comment.service;

import com.aurionpro.ticketboard.audit.enums.TimelineEventType;
import com.aurionpro.ticketboard.audit.service.ActivityLogService;
import com.aurionpro.ticketboard.comment.dto.CommentDto;
import com.aurionpro.ticketboard.comment.entity.Comment;
import com.aurionpro.ticketboard.comment.repository.CommentRepository;
import com.aurionpro.ticketboard.common.exception.BadRequestException;
import com.aurionpro.ticketboard.common.exception.ResourceNotFoundException;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<CommentDto> getComments(String entityType, Long entityId) {
        return commentRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(entityType, entityId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentDto addComment(CommentDto dto) {
        User author = getCurrentUser();
        if (dto.getAuthorId() != null) {
            author = userRepository.findById(dto.getAuthorId()).orElse(author);
        }

        if (author == null) {
            throw new BadRequestException("Author must be authenticated or provided");
        }

        Comment comment = Comment.builder()
                .entityType(dto.getEntityType().toUpperCase())
                .entityId(dto.getEntityId())
                .author(author)
                .content(dto.getContent())
                .build();

        Comment saved = commentRepository.save(comment);

        activityLogService.logEvent(
                saved.getEntityType(),
                saved.getEntityId(),
                null,
                TimelineEventType.COMMENT_ADDED,
                String.format("%s commented: \"%s\"", author.getFullName(),
                        saved.getContent().length() > 50 ? saved.getContent().substring(0, 47) + "..." : saved.getContent()),
                saved.getContent(),
                null,
                null
        );

        return mapToDto(saved);
    }

    @Transactional
    public void deleteComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));

        User user = getCurrentUser();
        activityLogService.logEvent(
                comment.getEntityType(),
                comment.getEntityId(),
                null,
                TimelineEventType.COMMENT_DELETED,
                String.format("Comment deleted by %s", user != null ? user.getFullName() : "User"),
                comment.getContent(),
                comment.getContent(),
                null
        );
        commentRepository.delete(comment);
    }

    @Transactional
    public CommentDto updateComment(Long id, String content) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));

        User editor = getCurrentUser();
        String oldContent = comment.getContent();
        comment.setContent(content);
        comment.setUpdatedBy(editor != null ? editor.getFullName() : null);
        Comment saved = commentRepository.save(comment);

        activityLogService.logEvent(
                saved.getEntityType(),
                saved.getEntityId(),
                null,
                TimelineEventType.COMMENT_EDITED,
                String.format("%s edited a comment", editor != null ? editor.getFullName() : "User"),
                saved.getContent(),
                oldContent,
                saved.getContent()
        );

        return mapToDto(saved);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return userRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }

    public CommentDto mapToDto(Comment c) {
        return CommentDto.builder()
                .id(c.getId())
                .entityType(c.getEntityType())
                .entityId(c.getEntityId())
                .authorId(c.getAuthor() != null ? c.getAuthor().getId() : null)
                .authorName(c.getAuthor() != null ? c.getAuthor().getFullName() : "Anonymous")
                .authorEmail(c.getAuthor() != null ? c.getAuthor().getEmail() : null)
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .updatedBy(c.getUpdatedBy())
                .build();
    }
}
