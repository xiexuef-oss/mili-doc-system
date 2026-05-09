package com.military.doc.common.storage;

import io.minio.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service("minioFileStorageService")
@ConditionalOnProperty(prefix = "minio", name = "enabled", havingValue = "true")
public class MinioFileStorageService implements FileStorageService {

    private final String endpoint;
    private final String accessKey;
    private final String secretKey;
    private final String bucket;
    private volatile MinioClient client;

    public MinioFileStorageService(
            @Value("${minio.endpoint}") String endpoint,
            @Value("${minio.accessKey}") String accessKey,
            @Value("${minio.secretKey}") String secretKey,
            @Value("${minio.bucket}") String bucket) {
        this.endpoint = endpoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.bucket = bucket;
    }

    private MinioClient getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = MinioClient.builder()
                            .endpoint(endpoint)
                            .credentials(accessKey, secretKey)
                            .build();
                    ensureBucket();
                }
            }
        }
        return client;
    }

    private void ensureBucket() {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize MinIO bucket: " + bucket, e);
        }
    }

    @Override
    public String upload(MultipartFile file) {
        try {
            return upload(file.getBytes(), file.getOriginalFilename());
        } catch (Exception e) {
            throw new RuntimeException("MinIO upload failed", e);
        }
    }

    @Override
    public String upload(byte[] bytes, String originalFilename) {
        String objectId = UUID.randomUUID().toString();
        String ext = getExtension(originalFilename);
        String objectName = objectId + ext;
        try (InputStream is = new java.io.ByteArrayInputStream(bytes)) {
            getClient().putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(is, bytes.length, -1)
                    .contentType("application/octet-stream")
                    .build());
            return objectName;
        } catch (Exception e) {
            throw new RuntimeException("MinIO upload failed", e);
        }
    }

    @Override
    public InputStream download(String objectId) {
        try {
            return getClient().getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectId)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO download failed: " + objectId, e);
        }
    }

    @Override
    public void delete(String objectId) {
        try {
            getClient().removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectId)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO delete failed: " + objectId, e);
        }
    }

    @Override
    public String getAccessUrl(String objectId) {
        try {
            return getClient().getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket)
                    .object(objectId)
                    .method(Method.GET)
                    .expiry(7, TimeUnit.DAYS)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO presigned URL failed: " + objectId, e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return i >= 0 ? filename.substring(i) : "";
    }
}
