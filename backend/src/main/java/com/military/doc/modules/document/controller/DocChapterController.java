package com.military.doc.modules.document.controller;

import com.military.doc.common.result.Result;
import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.service.DocChapterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/doc-chapters")
public class DocChapterController {

    @Autowired private DocChapterService docChapterService;

    @PostMapping("/init")
    public Result<List<DocChapter>> initFromTemplate(@RequestParam Long docLedgerId,
                                                      @RequestParam Long templateId,
                                                      @RequestParam Long operatorId) {
        return Result.success(docChapterService.initFromTemplate(docLedgerId, templateId, operatorId));
    }

    @GetMapping("/ledger/{docLedgerId}")
    public Result<List<DocChapter>> listByLedger(@PathVariable Long docLedgerId) {
        return Result.success(docChapterService.listByDocLedger(docLedgerId));
    }

    @GetMapping("/ledger/{docLedgerId}/tree")
    public Result<List<Map<String, Object>>> getTree(@PathVariable Long docLedgerId) {
        return Result.success(docChapterService.getChapterTree(docLedgerId));
    }

    @GetMapping("/ledger/{docLedgerId}/summary")
    public Result<Map<String, Object>> getSummary(@PathVariable Long docLedgerId) {
        return Result.success(docChapterService.getCompletionSummary(docLedgerId));
    }

    @GetMapping("/{id}")
    public Result<DocChapter> getById(@PathVariable Long id) {
        return Result.success(docChapterService.getById(id));
    }

    @PutMapping("/{id}/content")
    public Result<DocChapter> updateContent(@PathVariable Long id,
                                             @RequestBody Map<String, String> body) {
        return Result.success(docChapterService.updateContent(
                id, body.get("content"), body.get("contentJson"),
                body.containsKey("operatorId") ? Long.valueOf(body.get("operatorId")) : null));
    }

    @PutMapping("/{id}/fill-status")
    public Result<DocChapter> updateFillStatus(@PathVariable Long id,
                                                @RequestBody Map<String, Object> body) {
        return Result.success(docChapterService.updateFillStatus(
                id,
                (String) body.get("fillStatus"),
                body.get("fillPercentage") != null ? ((Number) body.get("fillPercentage")).intValue() : null));
    }
}
