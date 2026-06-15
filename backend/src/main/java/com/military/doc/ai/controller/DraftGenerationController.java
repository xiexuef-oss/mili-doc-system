package com.military.doc.ai.controller;

import com.military.doc.ai.service.DraftGenerationService;
import com.military.doc.ai.util.MarkdownChapterParser;
import com.military.doc.common.result.Result;
import com.military.doc.common.storage.FileStorageService;
import com.military.doc.common.util.Str;
import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.entity.DocFile;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.entity.DocCatalog;
import com.military.doc.modules.document.entity.DocVersion;
import com.military.doc.modules.document.service.DocChapterService;
import com.military.doc.modules.document.service.DocFileService;
import com.military.doc.modules.document.service.DocLedgerService;
import com.military.doc.modules.document.service.DocVersionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI 文档初稿生成与保存。
 * 拆分自 AiAssistantController (P1-①).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/draft/v2")
@Tag(name = "AI 文档初稿生成")
public class DraftGenerationController {

    private final DraftGenerationService draftGenerationService;
    private final DocLedgerService docLedgerService;
    private final DocFileService docFileService;
    private final DocVersionService docVersionService;
    private final DocChapterService docChapterService;
    private final FileStorageService fileStorageService;

    public DraftGenerationController(DraftGenerationService draftGenerationService,
                                      DocLedgerService docLedgerService,
                                      DocFileService docFileService,
                                      DocVersionService docVersionService,
                                      DocChapterService docChapterService,
                                      FileStorageService fileStorageService) {
        this.draftGenerationService = draftGenerationService;
        this.docLedgerService = docLedgerService;
        this.docFileService = docFileService;
        this.docVersionService = docVersionService;
        this.docChapterService = docChapterService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/draft/stream")
    @Operation(summary = "流式生成文档初稿（SSE）")
    public SseEmitter streamDraft(@RequestParam Long projectId,
                                   @RequestParam(required = false) Long catalogId,
                                   @RequestParam(required = false) Long docLedgerId,
                                   Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        SseEmitter emitter = new SseEmitter(300000L);
        var securityContext = SecurityContextHolder.getContext();

        CompletableFuture.runAsync(() -> {
            SecurityContextHolder.setContext(securityContext);
            try {
                draftGenerationService.generateStream(projectId, catalogId, docLedgerId, chunk -> {
                    try {
                        emitter.send(SseEmitter.event().name("chunk").data(chunk));
                    } catch (Exception e) {
                        log.debug("SSE send aborted");
                        throw new RuntimeException("SSE_ABORT");
                    }
                });
                emitter.send(SseEmitter.event().name("done").data("complete"));
                emitter.complete();
            } catch (RuntimeException e) {
                if (!"SSE_ABORT".equals(e.getMessage())) log.warn("Draft stream error: {}", e.getMessage());
                try { emitter.complete(); } catch (Exception ignored) {}
            } catch (Exception e) {
                log.error("Draft stream failed", e);
                try { emitter.completeWithError(e); } catch (Exception ignored) {}
            } finally {
                SecurityContextHolder.clearContext();
            }
        });

        emitter.onTimeout(() -> log.warn("SSE timeout for draft stream"));
        emitter.onError(ex -> log.warn("SSE error for draft stream: {}", ex.getMessage()));
        return emitter;
    }

    @PostMapping("/draft/save")
    @Operation(summary = "保存AI生成初稿")
    public Result<DocFile> saveDraft(@RequestBody Map<String, Object> body,
                                      Authentication authentication) {
        try {
        Long projectId = toLong(body.get("projectId"));
        Long catalogId = toLong(body.get("catalogId"));
        Long stageId = toLong(body.get("stageId"));
        String docName = (String) body.get("docName");
        String docType = (String) body.get("docType");
        String securityLevel = (String) body.getOrDefault("securityLevel", "内部");
        String content = (String) body.get("content");
        Long userId = (Long) authentication.getPrincipal();
        if (projectId == null) return Result.error("PARAM_ERROR", "projectId is required");

        DocLedger ledger = docLedgerService.findOrCreateDraftLedger(projectId, stageId, catalogId, docName, docType, userId);
        if (securityLevel != null) ledger.setSecurityLevel(securityLevel);
        boolean isNew = ledger.getId() == null || ledger.getId() <= 0;

        String fileObjectId = null;
        if (content != null && !content.isBlank()) {
            String filename = (docName != null ? docName : "draft") + ".md";
            fileObjectId = fileStorageService.upload(content.getBytes(StandardCharsets.UTF_8), filename);
        }

        DocFile docFile = new DocFile();
        docFile.setProjectId(projectId); docFile.setCatalogId(catalogId); docFile.setStageId(stageId);
        docFile.setDocName(docName != null ? docName : "AI 生成文档");
        docFile.setDocType(docType != null ? docType : "MANAGEMENT_DOC");
        docFile.setSecurityLevel(securityLevel); docFile.setStatus("DRAFT");
        docFile.setCreatedBy(userId); docFile.setCreatedAt(LocalDateTime.now());
        docFileService.save(docFile);

        long genCount = docFileService.count(new LambdaQueryWrapper<DocFile>()
            .eq(DocFile::getProjectId, projectId)
            .eq(catalogId != null, DocFile::getCatalogId, catalogId));
        String versionNo = "V0." + genCount + "-AI";

        DocVersion version = new DocVersion();
        version.setDocFileId(docFile.getId()); version.setVersionNo(versionNo);
        version.setSourceType("AI_GENERATED"); version.setFileObjectId(fileObjectId);
        version.setVersionStatus("DRAFT"); version.setOptimisticVersion(1);
        version.setSubmitUserId(userId); version.setSubmitTime(LocalDateTime.now());
        version.setChangeSummary("AI 自动生成初稿 (" + versionNo + ")");
        version.setCreatedBy(userId); version.setCreatedAt(LocalDateTime.now());
        docVersionService.save(version);

        ledger.setFileObjectId(fileObjectId);
        if (content != null) ledger.setContentSize((long) content.length());
        ledger.setUpdatedBy(userId);
        docLedgerService.updateById(ledger);

        if (content != null && !content.isBlank()) {
            if (!isNew) docChapterService.remove(new LambdaQueryWrapper<DocChapter>().eq(DocChapter::getDocLedgerId, ledger.getId()));
            var roots = MarkdownChapterParser.parse(content);
            if (roots.isEmpty()) {
                DocChapter ch = new DocChapter();
                ch.setDocLedgerId(ledger.getId()); ch.setChapterNumber("1"); ch.setChapterTitle("初稿内容");
                ch.setChapterLevel(1); ch.setOrderNum(1); ch.setParentId(0L);
                ch.setContent(content); ch.setFillStatus("DRAFT"); ch.setFillPercentage(100);
                ch.setCreatedBy(userId); ch.setUpdatedBy(userId);
                docChapterService.save(ch);
            } else {
                var flat = MarkdownChapterParser.flatten(roots);
                List<DocChapter> chapters = new ArrayList<>();
                for (int i = 0; i < flat.size(); i++) {
                    var fs = flat.get(i);
                    DocChapter dc = new DocChapter();
                    dc.setDocLedgerId(ledger.getId());
                    dc.setChapterNumber(fs.section().number() != null ? fs.section().number() : String.valueOf(i + 1));
                    dc.setChapterTitle(Str.truncate(fs.section().title(), 250));
                    dc.setChapterLevel(Math.min(fs.section().level(), 5));
                    dc.setOrderNum(fs.orderNum()); dc.setParentId(0L);
                    dc.setContent(Str.truncate(fs.section().content(), 50000));
                    dc.setFillStatus("DRAFT");
                    dc.setFillPercentage(fs.section().content() != null && !fs.section().content().isBlank() ? 100 : 0);
                    dc.setCreatedBy(userId); dc.setUpdatedBy(userId);
                    chapters.add(dc);
                }
                docChapterService.saveBatch(chapters);
                log.info("Created {} chapters from markdown for ledger {}", chapters.size(), ledger.getId());
            }
        }

        log.info("Draft saved: docFileId={}, ledgerId={}, version={}", docFile.getId(), ledger.getId(), versionNo);
        return Result.success(docFile);
        } catch (Exception e) {
            log.error("Draft save failed: {}", e.getMessage(), e);
            return Result.error("SAVE_ERROR", "保存失败: " + e.getMessage());
        }
    }

    private Long toLong(Object v) {
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) { try { return Long.parseLong(s); } catch (NumberFormatException ignored) {} }
        return null;
    }
}
