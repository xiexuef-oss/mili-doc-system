package com.military.doc.common.storage;

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
            return new FileInputStream(uploadDir.resolve(objectId).toFile());
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File not found: " + objectId, e);
        }
    }

    @Override
    public void delete(String objectId) {
        try {
            Files.deleteIfExists(uploadDir.resolve(objectId));
        } catch (IOException e) {
            throw new RuntimeException("Delete failed", e);
        }
    }

    @Override
    public String getAccessUrl(String objectId) {
        return "/api/v1/files/download/" + objectId;
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return i >= 0 ? filename.substring(i) : "";
    }
}
