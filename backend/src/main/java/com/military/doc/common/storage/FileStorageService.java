package com.military.doc.common.storage;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

public interface FileStorageService {
    String upload(MultipartFile file);
    String upload(byte[] bytes, String originalFilename);
    InputStream download(String objectId);
    void delete(String objectId);
    String getAccessUrl(String objectId);
}
