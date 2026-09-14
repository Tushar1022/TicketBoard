package com.aurionpro.ticketboard.common.lookup.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LookupDataCreateDto {

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Value is required")
    private String value;

    @NotBlank(message = "Label is required")
    private String label;

    private Integer displayOrder;

    private String colorCode;

    private Boolean isActive;

    private Boolean isDefault;
}