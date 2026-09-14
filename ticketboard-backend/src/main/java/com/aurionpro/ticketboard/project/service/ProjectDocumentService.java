package com.aurionpro.ticketboard.project.service;

import com.aurionpro.ticketboard.audit.enums.TimelineEventType;
import com.aurionpro.ticketboard.audit.service.ActivityLogService;
import com.aurionpro.ticketboard.common.exception.ResourceNotFoundException;
import com.aurionpro.ticketboard.document.storage.DocumentStorageService;
import com.aurionpro.ticketboard.document.storage.StoredDocumentInfo;
import com.aurionpro.ticketboard.project.dto.ProjectDocumentDto;
import com.aurionpro.ticketboard.project.entity.Project;
import com.aurionpro.ticketboard.project.entity.ProjectDocument;
import com.aurionpro.ticketboard.project.repository.ProjectDocumentRepository;
import com.aurionpro.ticketboard.project.repository.ProjectRepository;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectDocumentService {

    private final ProjectDocumentRepository projectDocumentRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final DocumentStorageService documentStorageService;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<ProjectDocumentDto> getDocuments(Long projectId) {
        return projectDocumentRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectDocumentDto uploadDocument(Long projectId, MultipartFile file, String description) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        String storedPath = documentStorageService.store(file, "projects");
        User currentUser = getCurrentUser();
        String fileName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "document");

        ProjectDocument doc = ProjectDocument.builder()
                .project(project)
                .fileName(fileName)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .description(description)
                .filePath(storedPath)
                .fileUrl("/api/v1/projects/documents/" + fileName)
                .uploadedBy(currentUser)
                .build();

        ProjectDocument saved = projectDocumentRepository.save(doc);

        activityLogService.logEvent(
                "PROJECT",
                project.getId(),
                project.getProjectCode(),
                TimelineEventType.DOCUMENT_ATTACHED,
                "Project document uploaded: " + fileName,
                "Uploaded by: " + (currentUser != null ? currentUser.getFullName() : "User"),
                null,
                "DOCUMENT_ATTACHED"
        );

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public StoredDocumentInfo loadDocument(Long docId) {
        ProjectDocument doc = projectDocumentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", docId));
        return new StoredDocumentInfo(
                doc.getFileName(),
                doc.getFileType() != null ? doc.getFileType() : "application/octet-stream",
                documentStorageService.loadAsResource(doc.getFilePath()));
    }

    @Transactional
    public void deleteDocument(Long docId) {
        ProjectDocument doc = projectDocumentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", docId));
        documentStorageService.delete(doc.getFilePath());
        projectDocumentRepository.delete(doc);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return userRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }

    private ProjectDocumentDto mapToDto(ProjectDocument d) {
        return ProjectDocumentDto.builder()
                .id(d.getId())
                .projectId(d.getProject().getId())
                .fileName(d.getFileName())
                .fileType(d.getFileType())
                .fileSize(d.getFileSize())
                .description(d.getDescription())
                .fileUrl(d.getFileUrl())
                .downloadUrl("/api/v1/projects/documents/" + d.getId() + "/download")
                .uploadedById(d.getUploadedBy() != null ? d.getUploadedBy().getId() : null)
                .uploadedByName(d.getUploadedBy() != null ? d.getUploadedBy().getFullName() : "User")
                .uploadedAt(d.getCreatedAt())
                .build();
    }
}