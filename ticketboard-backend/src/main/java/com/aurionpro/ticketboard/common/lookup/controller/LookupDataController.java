package com.aurionpro.ticketboard.common.lookup.controller;

import com.aurionpro.ticketboard.common.lookup.dto.LookupDataCreateDto;
import com.aurionpro.ticketboard.common.lookup.dto.LookupDataDto;
import com.aurionpro.ticketboard.common.lookup.service.LookupDataService;
import com.aurionpro.ticketboard.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lookup-data")
@RequiredArgsConstructor
public class LookupDataController {

    private final LookupDataService lookupDataService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LookupDataDto>>> getAll(
            @RequestParam(required = false) String category) {
        List<LookupDataDto> data = (category != null && !category.isBlank())
                ? lookupDataService.getByCategory(category)
                : lookupDataService.getAll();
        return ResponseEntity.ok(ApiResponse.ok("Lookup data fetched successfully", data));
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<LookupDataDto>> getById(@PathVariable Long id) {
        LookupDataDto data = lookupDataService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok("Lookup data fetched successfully", data));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<LookupDataDto>>> getByCategory(@PathVariable String category) {
        List<LookupDataDto> data = lookupDataService.getActiveByCategory(category);
        return ResponseEntity.ok(ApiResponse.ok("Lookup data fetched successfully", data));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        List<String> categories = lookupDataService.getCategories();
        return ResponseEntity.ok(ApiResponse.ok("Lookup categories fetched successfully", categories));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('admin:master-data')")
    public ResponseEntity<ApiResponse<LookupDataDto>> create(@Valid @RequestBody LookupDataCreateDto dto) {
        LookupDataDto data = lookupDataService.create(dto);
        return ResponseEntity.ok(ApiResponse.ok("Lookup data created successfully", data));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:master-data')")
    public ResponseEntity<ApiResponse<LookupDataDto>> update(@PathVariable Long id, @Valid @RequestBody LookupDataCreateDto dto) {
        LookupDataDto data = lookupDataService.update(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Lookup data updated successfully", data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:master-data')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        lookupDataService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Lookup data deactivated successfully", null));
    }
}