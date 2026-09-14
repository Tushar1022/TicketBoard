package com.aurionpro.ticketboard.document.storage;

import org.springframework.core.io.Resource;

public record StoredDocumentInfo(String fileName, String contentType, Resource resource) {
}