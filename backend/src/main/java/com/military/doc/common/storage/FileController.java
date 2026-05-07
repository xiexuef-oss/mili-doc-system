package com.military.doc.common.storage;

import com.military.doc.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "文件管理")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        String objectId = fileStorageService.upload(file);
        return Result.success(objectId);
    }

    @GetMapping("/download/{objectId}")
    @Operation(summary = "下载文件")
    public ResponseEntity<byte[]> download(@PathVariable String objectId) {
        InputStream is = fileStorageService.download(objectId);
        try {
            byte[] bytes = is.readAllBytes();
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + objectId + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Download failed", e);
        }
    }

    @DeleteMapping("/{objectId}")
    @Operation(summary = "删除文件")
    public Result<Void> delete(@PathVariable String objectId) {
        fileStorageService.delete(objectId);
        return Result.success();
    }
}
