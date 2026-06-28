package com.military.doc.modules.document.controller;

import com.military.doc.common.result.Result;
import com.military.doc.modules.document.service.DocxGenerationService;
import com.military.doc.modules.document.service.DocxParsingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/docx")
public class DocxController {

    @Autowired private DocxGenerationService generationService;
    @Autowired private DocxParsingService parsingService;

    @PostMapping("/generate/{docLedgerId}")
    public ResponseEntity<byte[]> generate(@PathVariable Long docLedgerId,
                                            @RequestParam(defaultValue = "true") boolean includeCover,
                                            @RequestParam(defaultValue = "true") boolean showHighlights) {
        byte[] bytes = generationService.generate(docLedgerId, includeCover, showHighlights);
        String filename = URLEncoder.encode("document_" + docLedgerId + ".docx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(bytes);
    }

    @PostMapping("/generate/{docLedgerId}/upload")
    public Result<Map<String, String>> generateAndUpload(@PathVariable Long docLedgerId,
                                                          @RequestParam(defaultValue = "true") boolean includeCover,
                                                          @RequestParam(defaultValue = "true") boolean showHighlights) {
        String objectId = generationService.generateAndUpload(docLedgerId, includeCover, showHighlights);
        return Result.success(Map.of("fileObjectId", objectId));
    }

    @PostMapping("/parse/{fileObjectId}")
    public Result<List<DocxParsingService.ChapterExtract>> parse(@PathVariable String fileObjectId) {
        return Result.success(parsingService.parseDocx(fileObjectId));
    }

    @PostMapping("/parse/{fileObjectId}/update-chapters")
    public Result<Map<String, Object>> parseAndUpdate(@PathVariable String fileObjectId,
                                                       @RequestParam Long docLedgerId,
                                                       Authentication authentication) {
        Long operatorId = (Long) authentication.getPrincipal();
        int updated = parsingService.updateChapters(docLedgerId, fileObjectId, operatorId);
        return Result.success(Map.of("updatedChapters", updated));
    }
}
