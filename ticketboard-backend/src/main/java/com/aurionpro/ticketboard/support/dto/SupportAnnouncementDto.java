package com.aurionpro.ticketboard.support.dto;

import com.aurionpro.ticketboard.support.entity.AnnouncementType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportAnnouncementDto {
    private Long id;
    private String title;
    private String message;
    private AnnouncementType type;
    private Boolean active;
    private String postedByName;
    private String postedByEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
