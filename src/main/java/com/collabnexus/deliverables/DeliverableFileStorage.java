package com.collabnexus.deliverables;

import com.collabnexus.common.Exceptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DeliverableFileStorage {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "webp", "gif", "txt", "csv", "zip");

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "pdf", "application/pdf",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "webp", "image/webp",
            "gif", "image/gif",
            "txt", "text/plain",
            "csv", "text/csv",
            "zip", "application/zip");

    private final Path root;
    private final long maxBytes;

    public DeliverableFileStorage(@Value("${cn.uploads.dir:./uploads}") String directory,
                                  @Value("${cn.uploads.max-bytes:10485760}") long maxBytes) {
        this.root = Paths.get(directory).toAbsolutePath().normalize().resolve("deliverables");
        this.maxBytes = maxBytes;
    }

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > maxBytes) {
            throw Exceptions.badRequest("FILE_TOO_LARGE",
                    "Supporting files must be 10 MB or smaller.");
        }

        String originalName = sanitizeName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw Exceptions.badRequest("FILE_TYPE_NOT_ALLOWED",
                    "Supporting files must be PDF, PNG, JPG, WEBP, GIF, TXT, CSV, or ZIP.");
        }

        String declaredType = file.getContentType();
        if (declaredType != null) {
            String normalized = declaredType.toLowerCase(Locale.ROOT);
            if (normalized.contains("executable") || normalized.contains("javascript")
                    || normalized.contains("html") || normalized.contains("shell")) {
                throw Exceptions.badRequest("FILE_TYPE_NOT_ALLOWED",
                        "This file type is not allowed.");
            }
        }
        String contentType = StringUtils.hasText(declaredType)
                && !declaredType.equalsIgnoreCase("application/octet-stream")
                ? declaredType
                : CONTENT_TYPES.get(extension);

        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        try {
            Files.createDirectories(root);
            Path target = root.resolve(storedName).normalize();
            if (!target.startsWith(root)) {
                throw Exceptions.badRequest("INVALID_FILE", "The uploaded file name is not valid.");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw Exceptions.badRequest("FILE_STORAGE_FAILED",
                    "The supporting file could not be stored. Please try again.");
        }

        return new StoredFile(storedName, originalName, contentType, file.getSize());
    }

    public Resource load(String storedName) {
        if (!StringUtils.hasText(storedName)) {
            throw Exceptions.notFound("Supporting file not found.");
        }
        Path target = root.resolve(storedName).normalize();
        if (!target.startsWith(root) || !Files.isRegularFile(target)) {
            throw Exceptions.notFound("Supporting file not found.");
        }
        return new FileSystemResource(target);
    }

    private String sanitizeName(String name) {
        String candidate = (name == null || name.isBlank()) ? "supporting-file" : name.trim();
        candidate = Paths.get(candidate).getFileName().toString();
        candidate = candidate.replaceAll("[^A-Za-z0-9._ -]", "_");
        if (candidate.length() > 255) {
            candidate = candidate.substring(candidate.length() - 255);
        }
        return candidate;
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public record StoredFile(String storedName, String originalName, String contentType, long size) {
    }
}
