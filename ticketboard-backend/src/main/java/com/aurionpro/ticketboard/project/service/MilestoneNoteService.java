package com.aurionpro.ticketboard.project.service;

import com.aurionpro.ticketboard.common.exception.ResourceNotFoundException;
import com.aurionpro.ticketboard.project.dto.MilestoneNoteCreateDto;
import com.aurionpro.ticketboard.project.dto.MilestoneNoteDto;
import com.aurionpro.ticketboard.project.entity.Milestone;
import com.aurionpro.ticketboard.project.entity.MilestoneNote;
import com.aurionpro.ticketboard.project.repository.MilestoneNoteRepository;
import com.aurionpro.ticketboard.project.repository.MilestoneRepository;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MilestoneNoteService {

    private final MilestoneNoteRepository milestoneNoteRepository;
    private final MilestoneRepository milestoneRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<MilestoneNoteDto> getNotesByMilestone(Long milestoneId) {
        return milestoneNoteRepository.findByMilestoneIdOrderByPinnedDescCreatedAtDesc(milestoneId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public MilestoneNoteDto addNote(Long milestoneId, MilestoneNoteCreateDto dto) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", "id", milestoneId));

        User author = getCurrentUser();

        MilestoneNote note = MilestoneNote.builder()
                .milestone(milestone)
                .author(author)
                .content(dto.getContent().trim())
                .pinned(dto.getPinned() != null ? dto.getPinned() : false)
                .build();
        if (author != null) {
            note.setCreatedBy(author.getEmail());
        }

        return mapToDto(milestoneNoteRepository.save(note));
    }

    @Transactional
    public MilestoneNoteDto updateNote(Long noteId, MilestoneNoteCreateDto dto) {
        MilestoneNote note = milestoneNoteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("MilestoneNote", "id", noteId));

        if (dto.getContent() != null && !dto.getContent().isBlank()) {
            note.setContent(dto.getContent().trim());
        }
        if (dto.getPinned() != null) {
            note.setPinned(dto.getPinned());
        }
        User editor = getCurrentUser();
        if (editor != null) {
            note.setUpdatedBy(editor.getEmail());
        }
        note.setUpdatedAt(LocalDateTime.now());

        return mapToDto(milestoneNoteRepository.save(note));
    }

    @Transactional
    public void deleteNote(Long noteId) {
        if (!milestoneNoteRepository.existsById(noteId)) {
            throw new ResourceNotFoundException("MilestoneNote", "id", noteId);
        }
        milestoneNoteRepository.deleteById(noteId);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return userRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }

    private MilestoneNoteDto mapToDto(MilestoneNote n) {
        return MilestoneNoteDto.builder()
                .id(n.getId())
                .milestoneId(n.getMilestone() != null ? n.getMilestone().getId() : null)
                .authorId(n.getAuthor() != null ? n.getAuthor().getId() : null)
                .authorName(n.getAuthor() != null ? n.getAuthor().getFullName() : "Anonymous")
                .authorEmail(n.getAuthor() != null ? n.getAuthor().getEmail() : null)
                .content(n.getContent())
                .pinned(n.getPinned())
                .createdAt(n.getCreatedAt())
                .updatedAt(n.getUpdatedAt())
                .build();
    }
}