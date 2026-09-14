package com.aurionpro.ticketboard.common.lookup.service;

import com.aurionpro.ticketboard.common.exception.BadRequestException;
import com.aurionpro.ticketboard.common.exception.ResourceNotFoundException;
import com.aurionpro.ticketboard.common.lookup.dto.LookupDataCreateDto;
import com.aurionpro.ticketboard.common.lookup.dto.LookupDataDto;
import com.aurionpro.ticketboard.common.lookup.entity.LookupData;
import com.aurionpro.ticketboard.common.lookup.repository.LookupDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LookupDataService {

    private final LookupDataRepository lookupDataRepository;

    @Transactional(readOnly = true)
    public List<LookupDataDto> getAll() {
        return lookupDataRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LookupDataDto> getByCategory(String category) {
        return lookupDataRepository.findByCategoryOrderByDisplayOrderAsc(category.toUpperCase()).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LookupDataDto> getActiveByCategory(String category) {
        return lookupDataRepository.findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(category.toUpperCase()).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LookupDataDto getById(Long id) {
        LookupData lookup = lookupDataRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LookupData", "id", id));
        return mapToDto(lookup);
    }

    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return lookupDataRepository.findDistinctCategories();
    }

    @Transactional
    public LookupDataDto create(LookupDataCreateDto dto) {
        String category = dto.getCategory().toUpperCase().trim();
        String value = dto.getValue().trim();

        if (lookupDataRepository.existsByCategoryAndValue(category, value)) {
            throw new BadRequestException("Lookup value already exists for category: " + category + " with value: " + value);
        }

        LookupData lookup = LookupData.builder()
                .category(category)
                .value(value)
                .label(dto.getLabel().trim())
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .colorCode(dto.getColorCode())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .isDefault(dto.getIsDefault() != null ? dto.getIsDefault() : false)
                .build();

        return mapToDto(lookupDataRepository.save(lookup));
    }

    @Transactional
    public LookupDataDto update(Long id, LookupDataCreateDto dto) {
        LookupData lookup = lookupDataRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LookupData", "id", id));

        String category = dto.getCategory().toUpperCase().trim();
        String value = dto.getValue().trim();

        if (!lookup.getCategory().equalsIgnoreCase(category) || !lookup.getValue().equals(value)) {
            if (lookupDataRepository.existsByCategoryAndValue(category, value)) {
                throw new BadRequestException("Lookup value already exists for category: " + category + " with value: " + value);
            }
        }

        lookup.setCategory(category);
        lookup.setValue(value);
        lookup.setLabel(dto.getLabel().trim());
        if (dto.getDisplayOrder() != null) {
            lookup.setDisplayOrder(dto.getDisplayOrder());
        }
        if (dto.getColorCode() != null) {
            lookup.setColorCode(dto.getColorCode());
        }
        if (dto.getIsActive() != null) {
            lookup.setIsActive(dto.getIsActive());
        }
        lookup.setIsDefault(dto.getIsDefault() != null ? dto.getIsDefault() : lookup.getIsDefault());

        return mapToDto(lookupDataRepository.save(lookup));
    }

    @Transactional
    public void delete(Long id) {
        LookupData lookup = lookupDataRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LookupData", "id", id));
        if (Boolean.TRUE.equals(lookup.getIsDefault())) {
            throw new BadRequestException("Cannot delete system-default lookup value: " + lookup.getValue());
        }
        lookup.setIsActive(false);
        lookupDataRepository.save(lookup);
    }

    public LookupDataDto mapToDto(LookupData lookup) {
        if (lookup == null) return null;
        return LookupDataDto.builder()
                .id(lookup.getId())
                .category(lookup.getCategory())
                .value(lookup.getValue())
                .label(lookup.getLabel())
                .displayOrder(lookup.getDisplayOrder())
                .colorCode(lookup.getColorCode())
                .isActive(lookup.getIsActive())
                .isDefault(lookup.getIsDefault())
                .createdAt(lookup.getCreatedAt())
                .updatedAt(lookup.getUpdatedAt())
                .build();
    }
}