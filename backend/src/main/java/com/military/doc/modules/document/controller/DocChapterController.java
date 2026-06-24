package com.military.doc.modules.document.controller;

import com.military.doc.ai.context.ChapterWritingContext;
import com.military.doc.ai.context.ChapterWritingContextService;
import com.military.doc.ai.service.DraftGenerationService;
import com.military.doc.ai.service.MasterDataExtractionService;
import com.military.doc.ai.service.VariableMappingService;
import com.military.doc.common.result.Result;
import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.service.DocChapterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/v1/doc-chapters")
public class DocChapterController {

    @Autowired private DocChapterService docChapterService;
    @Autowired(required = false) private ChapterWritingContextService chapterContextService;
    @Autowired(required = false) private DraftGenerationService draftGenerationService;
    @Autowired(required = false) private VariableMappingService variableMappingService;
    @Autowired(required = false) private MasterDataExtractionService masterDataExtractionService;
    @Autowired(required = false) private com.military.doc.ai.service.ChapterEditService chapterEditService;

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

    @PostMapping("/ledger/summary/batch")
    public Result<Map<Long, Map<String, Object>>> getSummaryBatch(@RequestBody Map<String, List<Long>> body) {
        return Result.success(docChapterService.getCompletionSummaryBatch(body.get("docLedgerIds")));
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

    // ========== Three-library fusion endpoints ==========

    @GetMapping("/{id}/writing-context")
    public Result<ChapterWritingContext> getWritingContext(@PathVariable Long id,
                                                           @RequestParam Long projectId) {
        return Result.success(chapterContextService.assembleForChapter(id, projectId));
    }

    @PostMapping("/{id}/generate")
    public Result<String> generateChapter(@PathVariable Long id,
                                           @RequestParam Long projectId) {
        String content = draftGenerationService.generateChapter(id, projectId);
        if (content != null && !content.isBlank()) {
            docChapterService.updateContent(id, content, null, 0L);
        }
        return Result.success(content);
    }

    @GetMapping("/{id}/generate/stream")
    public SseEmitter generateChapterStream(@PathVariable Long id,
                                             @RequestParam Long projectId) {
        SseEmitter emitter = new SseEmitter(300000L); // 5 min timeout
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                draftGenerationService.generateChapterStream(id, projectId, chunk -> {
                    try {
                        emitter.send(SseEmitter.event().data(chunk));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                });
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    @PostMapping("/{id}/auto-fill")
    public Result<DocChapter> autoFillChapter(@PathVariable Long id,
                                               @RequestParam Long projectId) {
        return Result.success(variableMappingService.autoFillChapter(id, projectId));
    }

    @PostMapping("/auto-fill-all")
    public Result<Map<String, Object>> autoFillAll(@RequestParam Long docLedgerId,
                                                    @RequestParam Long projectId) {
        int count = variableMappingService.autoFillAll(docLedgerId, projectId);
        return Result.success(Map.of("filledCount", count));
    }

    @PostMapping("/{id}/ai-edit")
    public Result<Map<String, Object>> aiEditChapter(@PathVariable Long id,
                                                      @RequestBody Map<String, String> body) {
        String actionStr = body.get("action"); // rewrite / expand / shorten / polish
        String instruction = body.get("instruction");
        if (actionStr == null || actionStr.isBlank()) {
            return Result.error("PARAM_ERROR", "action is required (rewrite/expand/shorten/polish)");
        }
        com.military.doc.ai.service.ChapterEditService.EditAction action;
        try {
            action = com.military.doc.ai.service.ChapterEditService.EditAction.valueOf(actionStr);
        } catch (IllegalArgumentException e) {
            return Result.error("PARAM_ERROR", "invalid action: " + actionStr);
        }
        String result = chapterEditService.edit(id, action, instruction);
        if (result != null && !result.isBlank()) {
            docChapterService.updateContent(id, result, null, 0L);
        }
        return Result.success(Map.of("content", result != null ? result : ""));
    }

    @PostMapping("/extract-master-data")
    public Result<Map<String, Object>> extractMasterData(@RequestParam Long projectId) {
        return Result.success(masterDataExtractionService.extractFromInputFiles(projectId));
    }

    @GetMapping("/ledger/{docLedgerId}/validate")
    public Result<DocChapterService.ChapterStructureValidation> validateStructure(@PathVariable Long docLedgerId) {
        return Result.success(docChapterService.validateStructure(docLedgerId));
    }

    @PostMapping("/ledger/{docLedgerId}/fix")
    public Result<DocChapterService.ChapterStructureValidation> fixStructure(@PathVariable Long docLedgerId) {
        return Result.success(docChapterService.fixStructure(docLedgerId));
    }
}
