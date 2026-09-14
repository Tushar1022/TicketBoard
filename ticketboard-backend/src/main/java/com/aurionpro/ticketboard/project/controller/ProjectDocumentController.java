package com.aurionpro.ticketboard.project.controller;

import com.aurionpro.ticketboard.common.response.ApiResponse;
import com.aurionpro.ticketboard.document.storage.StoredDocumentInfo;
import com.aurionpro.ticketboard.project.dto.ProjectDocumentDto;
import com.aurionpro.ticketboard.project.service.ProjectDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/documents")
@RequiredArgsConstructor
public class ProjectDocumentController {

    private final ProjectDocumentService projectDocumentService;

    @PreAuthorize("hasAuthority('project:view')")
    @GetMapping("/project/{projectId}")
    public ResponseEntity<ApiResponse<List<ProjectDocumentDto>>> getDocuments(@PathVariable Long projectId) {
        List<ProjectDocumentDto> docs = projectDocumentService.getDocuments(projectId);
        return ResponseEntity.ok(ApiResponse.ok("Project documents fetched successfully", docs));
    }

    @PreAuthorize("hasAuthority('project:edit')")
    @PostMapping(value = "/project/{projectId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProjectDocumentDto>> uploadDocument(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String description) {
        ProjectDocumentDto doc = projectDocumentService.uploadDocument(projectId, file, description);
        return ResponseEntity.ok(ApiResponse.ok("Project document uploaded successfully", doc));
    }

    @PreAuthorize("hasAuthority('project:view')")
    @GetMapping("/{docId}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long docId) {
        StoredDocumentInfo info = projectDocumentService.loadDocument(docId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + info.fileName() + "\"")
                .contentType(MediaType.parseMediaType(info.contentType()))
                .body(info.resource());
    }

    @PreAuthorize("hasAuthority('project:edit')")
    @DeleteMapping("/{docId}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long docId) {
        projectDocumentService.deleteDocument(docId);
        return ResponseEntity.ok(ApiResponse.ok("Project document deleted successfully", null));
    }
}