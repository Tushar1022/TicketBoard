package com.aurionpro.ticketboard.mail.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderCountsDto {
    private long inboxUnread;
    private long inboxTotal;
    private long sentTotal;
    private long draftsTotal;
    private long starredTotal;
    private long archiveTotal;
    private long spamTotal;
    private long trashTotal;
}
