package com.aurionpro.ticketboard.document.storage;

import com.aurionpro.ticketboard.common.exception.BadRequestException;
import com.aurionpro.ticketboard.common.exception.ResourceNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private final Path rootLocation;

    public DocumentStorageService() {
        this.rootLocation = Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
            Files.createDirectories(rootLocation.resolve("work-items"));
            Files.createDirectories(rootLocation.resolve("projects"));
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize upload storage directory", e);
        }
    }

    public String store(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Failed to store empty or missing file");
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String storedName = UUID.randomUUID() + "_" + original;
        try {
            Path targetDir = rootLocation.resolve(subDir);
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(storedName).normalize();
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return subDir + "/" + storedName;
        } catch (IOException e) {
            throw new BadRequestException("Could not store file " + original);
        }
    }

    public Resource loadAsResource(String storedPath) {
        if (storedPath == null || storedPath.isBlank() || storedPath.startsWith("http")) {
            throw new ResourceNotFoundException("File", "path", storedPath);
        }
        try {
            Path file = rootLocation.resolve(storedPath).normalize();
            if (!file.toAbsolutePath().startsWith(rootLocation.toAbsolutePath())) {
                throw new RuntimeException("Invalid path: directory traversal attempt detected");
            }
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new ResourceNotFoundException("File", "path", storedPath);
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File", "path", storedPath);
        }
    }

    public void delete(String storedPath) {
        if (storedPath == null || storedPath.isBlank() || storedPath.startsWith("http")) {
            return;
        }
        try {
            Path file = rootLocation.resolve(storedPath).normalize();
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // best effort delete
        }
    }
}