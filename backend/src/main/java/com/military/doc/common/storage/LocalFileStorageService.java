package com.military.doc.common.storage;

import com.military.doc.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;

@Service
@ConditionalOnMissingBean(name = "minioFileStorageService")
public class LocalFileStorageService implements FileStorageService {

    private final Path uploadDir;

    public LocalFileStorageService(@Value("${storage.local.path:uploads}") String path) {
        this.uploadDir = Paths.get(path).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create upload directory", e);
        }
    }

    @Override
    public String upload(MultipartFile file) {
        try {
            return upload(file.getBytes(), file.getOriginalFilename());
        } catch (IOException e) {
            throw new RuntimeException("Upload failed", e);
        }
    }

    @Override
    public String upload(byte[] bytes, String originalFilename) {
        String objectId = UUID.randomUUID().toString();
        String ext = getExtension(originalFilename);
        String filename = objectId + ext;
        try {
            Path target = uploadDir.resolve(filename);
            Files.write(target, bytes);
            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Upload failed", e);
        }
    }

    @Override
    public InputStream download(String objectId) {
        try {
            return new FileInputStream(resolveSafe(objectId).toFile());
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File not found: " + objectId, e);
        }
    }

    @Override
    public void delete(String objectId) {
        try {
            Files.deleteIfExists(resolveSafe(objectId));
        } catch (IOException e) {
            throw new RuntimeException("Delete failed", e);
        }
    }

    @Override
    public String getAccessUrl(String objectId) {
        return "/api/v1/files/download/" + objectId;
    }

    /** Rejects path traversal / absolute-path injection: resolved path must stay inside uploadDir. */
    private Path resolveSafe(String objectId) {
        if (objectId == null || objectId.isBlank()) {
            throw BusinessException.validation("文件标识不能为空");
        }
        Path resolved = uploadDir.resolve(objectId).normalize();
        if (!resolved.startsWith(uploadDir)) {
            throw BusinessException.validation("非法的文件标识: " + objectId);
        }
        return resolved;
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return i >= 0 ? filename.substring(i) : "";
    }
}
