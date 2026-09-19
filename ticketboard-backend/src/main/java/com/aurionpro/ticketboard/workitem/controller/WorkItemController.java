package com.aurionpro.ticketboard.workitem.controller;

import com.aurionpro.ticketboard.common.response.ApiResponse;
import com.aurionpro.ticketboard.document.storage.StoredDocumentInfo;
import com.aurionpro.ticketboard.workitem.dto.*;
import com.aurionpro.ticketboard.workitem.service.WorkItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/work-items")
@RequiredArgsConstructor
public class WorkItemController {

    private final WorkItemService workItemService;

    @PreAuthorize("hasAnyAuthority('workitem:view')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkItemDto>>> getAllWorkItems(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long requirementId,
            @RequestParam(required = false) Long assigneeId) {
        List<WorkItemDto> items;
        if (requirementId != null) {
            items = workItemService.getWorkItemsByRequirement(requirementId);
        } else if (projectId != null) {
            items = workItemService.getWorkItemsByProject(projectId);
        } else if (assigneeId != null) {
            items = workItemService.getWorkItemsByAssignee(assigneeId);
        } else {
            items = workItemService.getAllWorkItems();
        }
        return ResponseEntity.ok(ApiResponse.ok("Work items fetched successfully", items));
    }

    @PreAuthorize("hasAnyAuthority('workitem:view')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkItemDto>> getWorkItemById(@PathVariable Long id) {
        WorkItemDto item = workItemService.getWorkItemById(id);
        return ResponseEntity.ok(ApiResponse.ok("Work item fetched successfully", item));
    }

    @PreAuthorize("hasAnyAuthority('workitem:create', 'workitem:edit')")
    @PostMapping
    public ResponseEntity<ApiResponse<WorkItemDto>> createWorkItem(@Valid @RequestBody WorkItemCreateDto dto) {
        WorkItemDto created = workItemService.createWorkItem(dto);
        return ResponseEntity.ok(ApiResponse.ok("Work item created successfully", created));
    }

    @PreAuthorize("hasAnyAuthority('workitem:create', 'workitem:edit')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkItemDto>> updateWorkItem(@PathVariable Long id, @Valid @RequestBody WorkItemCreateDto dto) {
        WorkItemDto updated = workItemService.updateWorkItem(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Work item updated successfully", updated));
    }

    @PreAuthorize("hasAnyAuthority('workitem:create', 'workitem:edit')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<WorkItemDto>> updateStatus(@PathVariable Long id, @Valid @RequestBody WorkItemStatusUpdateDto dto) {
        WorkItemDto updated = workItemService.updateStatus(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Work item status updated successfully", updated));
    }

    @PreAuthorize("hasAnyAuthority('workitem:create', 'workitem:edit')")
    @PostMapping("/{id}/jira-sync")
    public ResponseEntity<ApiResponse<WorkItemDto>> syncWithJira(@PathVariable Long id) {
        WorkItemDto synced = workItemService.syncWithJira(id);
        return ResponseEntity.ok(ApiResponse.ok("Task synced with Jira successfully", synced));
    }

    @PreAuthorize("hasAnyAuthority('workitem:view')")
    @GetMapping("/{id}/documents")
    public ResponseEntity<ApiResponse<List<TaskDocumentDto>>> getDocuments(@PathVariable Long id) {
        List<TaskDocumentDto> docs = workItemService.getDocuments(id);
        return ResponseEntity.ok(ApiResponse.ok("Documents fetched successfully", docs));
    }

    @PreAuthorize("hasAnyAuthority('workitem:create', 'workitem:edit')")
    @PostMapping("/{id}/documents")
    public ResponseEntity<ApiResponse<TaskDocumentDto>> addDocument(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        String fileName = (String) payload.getOrDefault("fileName", "Specification_Doc.pdf");
        String fileType = (String) payload.getOrDefault("fileType", "application/pdf");
        Long fileSize = payload.containsKey("fileSize") ? ((Number) payload.get("fileSize")).longValue() : 2048000L;
        String fileUrl = (String) payload.getOrDefault("fileUrl", "/uploads/" + fileName);

        TaskDocumentDto doc = workItemService.addDocument(id, fileName, fileType, fileSize, fileUrl);
        return ResponseEntity.ok(ApiResponse.ok("Document attached successfully", doc));
    }

    @PreAuthorize("hasAnyAuthority('workitem:create', 'workitem:edit')")
    @PostMapping(value = "/{id}/documents/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TaskDocumentDto>> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        TaskDocumentDto doc = workItemService.uploadDocument(id, file);
        return ResponseEntity.ok(ApiResponse.ok("Document uploaded successfully", doc));
    }

    @PreAuthorize("hasAnyAuthority('workitem:view')")
    @GetMapping("/documents/{docId}/download")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> downloadDocument(
            @PathVariable Long docId) {
        StoredDocumentInfo info = workItemService.loadDocument(docId);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + info.fileName() + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType(info.contentType()))
                .body(info.resource());
    }

    @PreAuthorize("hasAnyAuthority('workitem:delete')")
    @DeleteMapping("/documents/{docId}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long docId) {
        workItemService.deleteDocument(docId);
        return ResponseEntity.ok(ApiResponse.ok("Document deleted successfully", null));
    }

    @PreAuthorize("hasAnyAuthority('workitem:create', 'workitem:edit')")
    @PostMapping("/dependencies")
    public ResponseEntity<ApiResponse<DependencyDto>> addDependency(@Valid @RequestBody DependencyDto dto) {
        DependencyDto created = workItemService.addDependency(dto);
        return ResponseEntity.ok(ApiResponse.ok("Dependency added successfully", created));
    }

    @PreAuthorize("hasAnyAuthority('workitem:delete')")
    @DeleteMapping("/dependencies/{dependencyId}")
    public ResponseEntity<ApiResponse<Void>> deleteDependency(@PathVariable Long dependencyId) {
        workItemService.removeDependency(dependencyId);
        return ResponseEntity.ok(ApiResponse.ok("Dependency removed successfully", null));
    }

    @PreAuthorize("hasAnyAuthority('workitem:delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWorkItem(@PathVariable Long id) {
        workItemService.deleteWorkItem(id);
        return ResponseEntity.ok(ApiResponse.ok("Work item deleted successfully", null));
    }
}
