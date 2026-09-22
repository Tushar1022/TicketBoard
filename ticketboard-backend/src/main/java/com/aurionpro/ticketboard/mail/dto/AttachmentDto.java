package com.aurionpro.ticketboard.mail.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachmentDto {
    private Long id;
    private Long emailMessageId;
    private String fileName;
    private String contentType;
    private long sizeBytes;
    @JsonProperty("isDrive")
    private boolean isDrive;
    private String driveUrl;
}