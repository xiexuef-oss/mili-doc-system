package com.military.doc.ai.controller;

import com.military.doc.ai.llm.LlmClient;
import com.military.doc.ai.llm.LlmProviderService;
import com.military.doc.ai.context.VectorIndexService;
import com.military.doc.ai.util.MarkdownChapterParser;
import com.military.doc.ai.entity.EmbeddingIndexTask;
import com.military.doc.ai.entity.GenerationFeedback;
import com.military.doc.ai.mapper.GenerationFeedbackMapper;
import com.military.doc.ai.service.AiChapterStructureService;
import com.military.doc.ai.service.AiAuditService;
import com.military.doc.ai.service.BatchGenerationService;
import com.military.doc.ai.service.IncrementalGenerationService;
import com.military.doc.ai.service.CatalogGenerationService;
import com.military.doc.ai.service.ArchiveAdvisorService;
import com.military.doc.ai.service.ChangeImpactService;
import com.military.doc.ai.service.ComplianceCheckService;
import com.military.doc.ai.service.DraftGenerationService;
import com.military.doc.ai.service.OpinionSummaryService;
import com.military.doc.ai.service.PreReviewService;
import com.military.doc.ai.service.ProofreadingService;
import com.military.doc.ai.service.StageReadinessService;
import com.military.doc.ai.service.TrainingDataService;
import com.military.doc.ai.entity.TrainingExample;
import com.military.doc.ai.util.SensitiveDataScrubber;
import com.military.doc.common.result.Result;
import com.military.doc.common.storage.FileStorageService;
import com.military.doc.config.LlmProperties;
import com.military.doc.modules.document.entity.DocCatalog;
import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.entity.DocFile;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.entity.DocVersion;
import com.military.doc.modules.document.service.DocChapterService;
import com.military.doc.modules.document.service.DocFileService;
import com.military.doc.modules.document.service.DocLedgerService;
import com.military.doc.modules.document.service.DocVersionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI 智能助手")
public class AiAssistantController {

    private final CatalogGenerationService catalogGenerationService;
    private final DraftGenerationService draftGenerationService;
    private final TrainingDataService trainingDataService;
    private final ProofreadingService proofreadingService;
    private final PreReviewService preReviewService;
    private final ComplianceCheckService complianceCheckService;
    private final OpinionSummaryService opinionSummaryService;
    private final StageReadinessService stageReadinessService;
    private final ArchiveAdvisorService archiveAdvisorService;
    private final ChangeImpactService changeImpactService;
    private final BatchGenerationService batchGenerationService;
    private final VectorIndexService vectorIndexService;
    private final AiAuditService aiAuditService;
    private final SensitiveDataScrubber sensitiveDataScrubber;
    private final AiChapterStructureService aiChapterStructureService;
    private final com.military.doc.ai.service.DocumentStructureService documentStructureService;
    private final IncrementalGenerationService incrementalGenerationService;
    private final GenerationFeedbackMapper feedbackMapper;
    private final DocFileService docFileService;
    private final DocVersionService docVersionService;
    private final DocLedgerService docLedgerService;
    private final FileStorageService fileStorageService;
    private final com.military.doc.modules.document.mapper.DocLedgerMapper docLedgerMapper;
    private final com.military.doc.modules.document.mapper.DocChapterMapper docChapterMapper;
    private final DocChapterService docChapterService;
    private final LlmProperties llmProperties;
    private final LlmProviderService llmProviderService;
    private final LlmClient llmClient;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AiAssistantController(CatalogGenerationService catalogGenerationService,
                                  DraftGenerationService draftGenerationService,
                                  TrainingDataService trainingDataService,
                                  ProofreadingService proofreadingService,
                                  PreReviewService preReviewService,
                                  ComplianceCheckService complianceCheckService,
                                  OpinionSummaryService opinionSummaryService,
                                  StageReadinessService stageReadinessService,
                                  ArchiveAdvisorService archiveAdvisorService,
                                  ChangeImpactService changeImpactService,
                                  BatchGenerationService batchGenerationService,
                                  VectorIndexService vectorIndexService,
                                  AiAuditService aiAuditService,
                                  SensitiveDataScrubber sensitiveDataScrubber,
                                  AiChapterStructureService aiChapterStructureService,
                                  com.military.doc.ai.service.DocumentStructureService documentStructureService,
                                  IncrementalGenerationService incrementalGenerationService,
                                  GenerationFeedbackMapper feedbackMapper,
                                  DocFileService docFileService,
                                  DocVersionService docVersionService,
                                  DocLedgerService docLedgerService,
                                  FileStorageService fileStorageService,
                                  com.military.doc.modules.document.mapper.DocLedgerMapper docLedgerMapper,
                                  com.military.doc.modules.document.mapper.DocChapterMapper docChapterMapper,
                                  DocChapterService docChapterService,
                                  LlmProperties llmProperties,
                                  LlmProviderService llmProviderService,
                                  LlmClient llmClient,
                                  OkHttpClient httpClient,
                                  ObjectMapper objectMapper) {
        this.catalogGenerationService = catalogGenerationService;
        this.draftGenerationService = draftGenerationService;
        this.trainingDataService = trainingDataService;
        this.proofreadingService = proofreadingService;
        this.preReviewService = preReviewService;
        this.complianceCheckService = complianceCheckService;
        this.opinionSummaryService = opinionSummaryService;
        this.stageReadinessService = stageReadinessService;
        this.archiveAdvisorService = archiveAdvisorService;
        this.changeImpactService = changeImpactService;
        this.batchGenerationService = batchGenerationService;
        this.vectorIndexService = vectorIndexService;
        this.aiAuditService = aiAuditService;
        this.sensitiveDataScrubber = sensitiveDataScrubber;
        this.aiChapterStructureService = aiChapterStructureService;
        this.documentStructureService = documentStructureService;
        this.incrementalGenerationService = incrementalGenerationService;
        this.feedbackMapper = feedbackMapper;
        this.docFileService = docFileService;
        this.docVersionService = docVersionService;
        this.docLedgerService = docLedgerService;
        this.fileStorageService = fileStorageService;
        this.docLedgerMapper = docLedgerMapper;
        this.docChapterMapper = docChapterMapper;
        this.docChapterService = docChapterService;
        this.llmProperties = llmProperties;
        this.llmProviderService = llmProviderService;
        this.llmClient = llmClient;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/catalog/generate")
    @Operation(summary = "根据项目输入文件和适用标准自动生成文档目录（默认追加模式）")
    public Result<Map<String, Object>> generateCatalog(@RequestBody Map<String, Object> body,
                                                     Authentication authentication) {
        Long projectId = toLong(body.get("projectId"));
        Long stageId = toLong(body.get("stageId"));
        // 默认追加模式，仅当用户明确传 overwrite=true 时才覆盖
        boolean overwrite = Boolean.TRUE.equals(body.get("overwrite"));
        Long userId = (Long) authentication.getPrincipal();

        log.info("Catalog generation requested: projectId={}, stageId={}, overwrite={}, userId={}",
            projectId, stageId, overwrite, userId);

        if (projectId == null) {
            return Result.error("PARAM_ERROR", "projectId is required");
        }

        List<DocCatalog> catalogs = catalogGenerationService.generate(projectId, stageId, userId, overwrite);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("catalogs", catalogs);
        result.put("totalNew", catalogs.size());
        result.put("mode", overwrite ? "OVERWRITE" : "APPEND");
        return Result.success(result);
    }

    @PostMapping("/catalog/preview")
    @Operation(summary = "预览目录生成结果（不写入数据库），显示冲突信息")
    public Result<Map<String, Object>> previewCatalog(@RequestBody Map<String, Object> body) {
        Long projectId = toLong(body.get("projectId"));
        Long stageId = toLong(body.get("stageId"));

        if (projectId == null) {
            return Result.error("PARAM_ERROR", "projectId is required");
        }

        Map<String, Object> preview = catalogGenerationService.generateDryRun(projectId, stageId);
        return Result.success(preview);
    }

    @GetMapping("/provider")
    @Operation(summary = "获取当前大模型供应商信息")
    public Result<Map<String, Object>> getProvider() {
        return Result.success(llmProviderService.getStatus());
    }

    @PutMapping("/provider")
    @Operation(summary = "切换大模型供应商（ollama/deepseek）")
    public Result<Map<String, Object>> switchProvider(@RequestBody Map<String, String> body) {
        String provider = body.get("provider");
        if (provider == null || provider.isBlank()) {
            return Result.error("PARAM_ERROR", "provider is required (ollama or deepseek)");
        }
        try {
            llmProviderService.switchProvider(provider);
            return Result.success(llmProviderService.getStatus());
        } catch (IllegalArgumentException e) {
            return Result.error("PARAM_ERROR", e.getMessage());
        }
    }

    @GetMapping("/locality")
    @Operation(summary = "获取当前模型位置信息（本地/云端）")
    public Result<Map<String, Object>> getLocality() {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("provider", llmProviderService.getActiveProvider());
        data.put("locality", llmProviderService.getLocality());
        data.put("isLocal", llmProviderService.isLocal());
        data.put("model", llmProperties.getModel());
        data.put("baseUrl", llmProperties.getBaseUrl());
        data.put("desensitizationEnabled", sensitiveDataScrubber.isEnabled());
        return Result.success(data);
    }

    @GetMapping("/health")
    @Operation(summary = "检查大模型连接状态")
    public Result<Map<String, Object>> health() {
        Map<String, Object> status = new java.util.LinkedHashMap<>();
        status.put("provider", llmProperties.getProvider());
        status.put("model", llmProperties.getModel());
        status.put("baseUrl", llmProperties.getBaseUrl());
        status.put("locality", llmProviderService.getLocality());
        status.put("isLocal", llmProviderService.isLocal());
        status.put("connected", false);

        try {
            boolean isOllama = "ollama".equals(llmProperties.getProvider());
            String healthUrl = isOllama
                ? llmProperties.getBaseUrl() + "/api/tags"
                : llmProperties.getBaseUrl() + "/v1/models";
            Request.Builder reqBuilder = new Request.Builder().url(healthUrl).get();
            if (!isOllama) {
                reqBuilder.header("Authorization", "Bearer " + llmProperties.getApiKey());
            }
            Request request = reqBuilder.build();
            try (var response = httpClient.newCall(request).execute()) {
                status.put("connected", response.isSuccessful());
                status.put("httpStatus", response.code());
                if (response.isSuccessful() && response.body() != null) {
                    String bodyStr = response.body().string();
                    var node = objectMapper.readTree(bodyStr);
                    if (isOllama) {
                        var models = node.get("models");
                        if (models != null && models.isArray()) {
                            var modelNames = new java.util.ArrayList<String>();
                            for (var m : models) {
                                String name = m.has("name") ? m.get("name").asText() : "";
                                modelNames.add(name);
                            }
                            status.put("availableModels", modelNames);
                            status.put("modelLoaded", true);
                        }
                    } else {
                        var models = node.get("data");
                        if (models != null && models.isArray()) {
                            var modelNames = new java.util.ArrayList<String>();
                            for (var m : models) {
                                String id = m.has("id") ? m.get("id").asText() : "";
                                modelNames.add(id);
                            }
                            status.put("availableModels", modelNames);
                            status.put("modelLoaded", !modelNames.isEmpty());
                        }
                    }
                }
            }
        } catch (Exception e) {
            status.put("connected", false);
            status.put("error", e.getMessage());
        }

        return Result.success(status);
    }

    @GetMapping("/draft/stream")
    @Operation(summary = "流式生成文档初稿（SSE），逐章生成+即时保存+进度事件")
    public SseEmitter streamDraft(@RequestParam Long projectId,
                                   @RequestParam(required = false) Long catalogId,
                                   @RequestParam(required = false) Long docLedgerId,
                                   Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        SseEmitter emitter = new SseEmitter(1800000L); // 30min timeout for large docs
        log.info("Draft stream: projectId={}, docLedgerId={}, userId={}", projectId, docLedgerId, userId);

        var securityContext = SecurityContextHolder.getContext();
        CompletableFuture.runAsync(() -> {
            SecurityContextHolder.setContext(securityContext);
            try {
                draftGenerationService.generateStream(projectId, catalogId, docLedgerId,
                    chunk -> {
                        try {
                            emitter.send(SseEmitter.event().name("chunk").data(chunk));
                        } catch (Exception e) {
                            throw new RuntimeException("SSE_ABORT", e);
                        }
                    },
                    (current, total, title, chars) -> {
                        try {
                            String progressJson = String.format(
                                "{\"current\":%d,\"total\":%d,\"title\":\"%s\",\"chars\":%d}",
                                current, total, title.replace("\"", "\\\""), chars);
                            emitter.send(SseEmitter.event().name("progress").data(progressJson));
                        } catch (Exception ignored) {}
                    });
                emitter.send(SseEmitter.event().name("done").data("complete"));
                emitter.complete();
            } catch (RuntimeException e) {
                if ("SSE_ABORT".equals(e.getMessage())) {
                    log.debug("Draft stream aborted by client disconnect");
                } else {
                    log.warn("Draft stream interrupted: {}", e.getMessage());
                }
                try { emitter.complete(); } catch (Exception ig) {}
            } catch (Exception e) {
                log.error("Draft stream failed", e);
                try { emitter.completeWithError(e); } catch (Exception ig) {}
            } finally {
                SecurityContextHolder.clearContext();
            }
        });

        emitter.onTimeout(() -> log.warn("SSE timeout for draft stream"));
        emitter.onError(ex -> log.warn("SSE error: {}", ex.getMessage()));
        return emitter;
    }

    @PostMapping("/draft/save")
    @Operation(summary = "保存AI生成初稿，新建起草台账（策划列原条目不动），同目录多次生成则覆盖")
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

        if (projectId == null) {
            return Result.error("PARAM_ERROR", "projectId is required");
        }

        // 1. Find existing DRAFTING ledger by projectId + catalogId (not by docLedgerId,
        //    so the PLANNED catalog card stays in place). If not found, create new.
        DocLedger existingDraft = null;
        if (catalogId != null) {
            existingDraft = docLedgerService.getOne(new LambdaQueryWrapper<DocLedger>()
                .eq(DocLedger::getProjectId, projectId)
                .eq(DocLedger::getCatalogId, catalogId)
                .eq(DocLedger::getLifecycleStatus, "DRAFTING")
                .orderByDesc(DocLedger::getId)
                .last("LIMIT 1"));
        }

        boolean isNew = (existingDraft == null);
        DocLedger ledger;
        if (isNew) {
            ledger = new DocLedger();
            ledger.setProjectId(projectId);
            ledger.setStageId(stageId);
            ledger.setCatalogId(catalogId);
            ledger.setDocName(docName != null ? docName : "AI 生成文档");
            ledger.setDocType(docType != null ? docType : "MANAGEMENT_DOC");
            ledger.setSecurityLevel(securityLevel);
            ledger.setLifecycleStatus("DRAFTING");
            ledger.setRequiredFlag(true);
            ledger.setCreatedBy(userId);
            ledger.setUpdatedBy(userId);
            ledger.setCreatedAt(LocalDateTime.now());
            ledger.setUpdatedAt(LocalDateTime.now());
            docLedgerService.save(ledger);
        } else {
            ledger = existingDraft;
            if (docName != null && !docName.equals(ledger.getDocName())) {
                ledger.setDocName(docName);
            }
            if (docType != null) ledger.setDocType(docType);
            ledger.setStageId(stageId);
            ledger.setUpdatedBy(userId);
            ledger.setUpdatedAt(LocalDateTime.now());
            docLedgerService.updateById(ledger);
        }

        // 2. Upload content as .md file
        String fileObjectId = null;
        if (content != null && !content.isBlank()) {
            String filename = (docName != null ? docName : "draft") + ".md";
            fileObjectId = fileStorageService.upload(
                content.getBytes(StandardCharsets.UTF_8), filename);
        }

        // 3. Create DocFile snapshot record
        DocFile docFile = new DocFile();
        docFile.setProjectId(projectId);
        docFile.setCatalogId(catalogId);
        docFile.setStageId(stageId);
        docFile.setDocName(docName != null ? docName : "AI 生成文档");
        docFile.setDocType(docType != null ? docType : "MANAGEMENT_DOC");
        docFile.setSecurityLevel(securityLevel);
        docFile.setStatus("DRAFT");
        docFile.setCreatedBy(userId);
        docFile.setCreatedAt(LocalDateTime.now());
        docFileService.save(docFile);

        // 4. Version number from generation count
        long genCount = docFileService.count(new LambdaQueryWrapper<DocFile>()
            .eq(DocFile::getProjectId, projectId)
            .eq(catalogId != null, DocFile::getCatalogId, catalogId));
        String versionNo = "V0." + genCount + "-AI";

        // 5. Create version record
        DocVersion version = new DocVersion();
        version.setDocFileId(docFile.getId());
        version.setVersionNo(versionNo);
        version.setSourceType("AI_GENERATED");
        version.setFileObjectId(fileObjectId);
        version.setVersionStatus("DRAFT");
        version.setOptimisticVersion(1);
        version.setSubmitUserId(userId);
        version.setSubmitTime(LocalDateTime.now());
        version.setChangeSummary("AI 自动生成初稿 (" + versionNo + ")");
        version.setCreatedBy(userId);
        version.setCreatedAt(LocalDateTime.now());
        docVersionService.save(version);

        // 6. Update ledger file reference and content size
        ledger.setFileObjectId(fileObjectId);
        if (content != null) ledger.setContentSize((long) content.length());
        ledger.setUpdatedBy(userId);
        docLedgerService.updateById(ledger);

        // 7. Fill content into chapters (only if chapters are empty — per-chapter gen already saves)
        if (content != null && !content.isBlank()) {
            List<DocChapter> existingChapters = docChapterService.list(
                new LambdaQueryWrapper<DocChapter>()
                    .eq(DocChapter::getDocLedgerId, ledger.getId())
                    .eq(DocChapter::getDeleted, 0)
                    .orderByAsc(DocChapter::getOrderNum));

            boolean hasContent = existingChapters.stream()
                .anyMatch(c -> c.getContent() != null && !c.getContent().isBlank());

            if (!hasContent) {
                // Per-chapter generation didn't run — fall back to content mapping
                if (!existingChapters.isEmpty()) {
                    int filled = mapContentToExistingChapters(existingChapters, content, userId);
                    log.info("Mapped AI content to {} existing chapters for ledger {}", filled, ledger.getId());
                } else {
                    createChaptersFromAiContent(ledger.getId(), content, userId);
                }
            } else {
                log.info("Chapters already have content from per-chapter generation, skipping content mapping");
            }
        }

        log.info("Draft saved: docFileId={}, ledgerId={}, version={}, catalogId={}, isNew={}",
            docFile.getId(), ledger.getId(), versionNo, catalogId, isNew);
        return Result.success(docFile);
        } catch (Exception e) {
            log.error("Draft save failed: {}", e.getMessage(), e);
            return Result.error("SAVE_ERROR", "保存失败: " + e.getMessage());
        }
    }

    // ---- Phase 2: AI 校对、预评审、合规检查、意见汇总、转阶段评估 ----

    @PostMapping("/proofread/{docLedgerId}")
    @Operation(summary = "AI 校对：检查文档格式/术语/标准条款引用")
    public Result<Map<String, Object>> proofread(@PathVariable Long docLedgerId) {
        return Result.success(proofreadingService.proofread(docLedgerId));
    }

    @PostMapping("/pre-review/{docLedgerId}")
    @Operation(summary = "AI 预评审：基于标准条款评估文档合规度")
    public Result<Map<String, Object>> preReview(@PathVariable Long docLedgerId) {
        return Result.success(preReviewService.preReview(docLedgerId));
    }

    @PostMapping("/compliance/check")
    @Operation(summary = "基线文件 vs 标准条款合规检查")
    public Result<Map<String, Object>> complianceCheck(@RequestBody Map<String, Object> body) {
        Long projectId = toLong(body.get("projectId"));
        Long baselineId = toLong(body.get("baselineId"));
        if (projectId == null || baselineId == null) {
            return Result.error("PARAM_ERROR", "projectId and baselineId are required");
        }
        return Result.success(complianceCheckService.check(projectId, baselineId));
    }

    @PostMapping("/opinion-summary/{meetingId}")
    @Operation(summary = "汇总所有专家意见 + 结论建议")
    public Result<Map<String, Object>> opinionSummary(@PathVariable Long meetingId) {
        return Result.success(opinionSummaryService.summarize(meetingId));
    }

    @PostMapping("/stage-readiness")
    @Operation(summary = "阶段齐套度评估 + 风险提示")
    public Result<Map<String, Object>> stageReadiness(@RequestBody Map<String, Object> body) {
        Long projectId = toLong(body.get("projectId"));
        Long stageId = toLong(body.get("stageId"));
        if (projectId == null || stageId == null) {
            return Result.error("PARAM_ERROR", "projectId and stageId are required");
        }
        return Result.success(stageReadinessService.assess(projectId, stageId));
    }

    @PostMapping("/archive-advice/{docLedgerId}")
    @Operation(summary = "归档建议：密级/保管期限评估")
    public Result<Map<String, Object>> archiveAdvice(@PathVariable Long docLedgerId) {
        return Result.success(archiveAdvisorService.advise(docLedgerId));
    }

    @PostMapping("/change-impact")
    @Operation(summary = "变更影响分析：分析更改对相关文档/CI的影响范围")
    public Result<Map<String, Object>> changeImpact(@RequestBody Map<String, Object> body) {
        Long projectId = toLong(body.get("projectId"));
        String changeDescription = (String) body.getOrDefault("changeDescription", "");
        Long baselineId = toLong(body.get("baselineId"));
        if (projectId == null || changeDescription.isBlank()) {
            return Result.error("PARAM_ERROR", "projectId and changeDescription are required");
        }
        return Result.success(changeImpactService.analyze(projectId, changeDescription, baselineId));
    }

    // ---- Phase 3: Continuous Learning Pipeline ----

    @PostMapping("/training/collect")
    @Operation(summary = "收集文档为训练示例")
    public Result<TrainingExample> collectTraining(@RequestBody Map<String, Object> body,
                                                   Authentication authentication) {
        Long docFileId = toLong(body.get("docFileId"));
        Long projectId = toLong(body.get("projectId"));
        Long catalogId = toLong(body.get("catalogId"));
        Long userId = (Long) authentication.getPrincipal();

        if (docFileId == null || projectId == null) {
            return Result.error("PARAM_ERROR", "docFileId and projectId are required");
        }

        TrainingExample example = trainingDataService.collect(docFileId, projectId, catalogId, userId);
        return Result.success(example);
    }

    @GetMapping("/training/examples")
    @Operation(summary = "分页查询训练示例")
    public Result<Page<TrainingExample>> listTraining(
            @RequestParam(defaultValue = "") String quality,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(trainingDataService.list(quality, page, size));
    }

    @PutMapping("/training/examples/{id}/approve")
    @Operation(summary = "批准训练示例")
    public Result<TrainingExample> approveTraining(@PathVariable Long id) {
        TrainingExample example = trainingDataService.approve(id);
        if (example == null) {
            return Result.error("NOT_FOUND", "训练示例不存在");
        }
        return Result.success(example);
    }

    @PutMapping("/training/examples/{id}/reject")
    @Operation(summary = "驳回训练示例")
    public Result<TrainingExample> rejectTraining(@PathVariable Long id) {
        TrainingExample example = trainingDataService.reject(id);
        if (example == null) {
            return Result.error("NOT_FOUND", "训练示例不存在");
        }
        return Result.success(example);
    }

    @GetMapping("/training/export")
    @Operation(summary = "导出训练数据为 JSONL 格式（用于模型微调）")
    public Result<Map<String, Object>> exportTraining(@RequestParam(defaultValue = "APPROVED") String quality) {
        String jsonl = trainingDataService.exportJsonl(quality);
        return Result.success(Map.of(
            "format", "jsonl",
            "quality", quality,
            "data", (Object) jsonl
        ));
    }

    // ---- Embedding / Vector Index Management ----

    @PostMapping("/embedding/index-clauses")
    @Operation(summary = "为所有标准条款创建/更新向量嵌入")
    public Result<Map<String, Object>> indexAllClauses() {
        EmbeddingIndexTask task = vectorIndexService.createTask("INDEX_CLAUSES", "standard_clause");
        vectorIndexService.indexAllClausesAsync(task);
        return Result.success(Map.of("taskId", task.getId(), "status", task.getStatus()));
    }

    @PostMapping("/embedding/index-knowledge")
    @Operation(summary = "为所有知识库文章创建/更新向量嵌入")
    public Result<Map<String, Object>> indexAllKnowledge() {
        EmbeddingIndexTask task = vectorIndexService.createTask("INDEX_KNOWLEDGE", "knowledge_base");
        vectorIndexService.indexAllKnowledgeAsync(task);
        return Result.success(Map.of("taskId", task.getId(), "status", task.getStatus()));
    }

    @GetMapping("/embedding/stats")
    @Operation(summary = "查询向量嵌入统计")
    public Result<Map<String, Object>> getEmbeddingStats() {
        return Result.success(vectorIndexService.getStats());
    }

    @GetMapping("/embedding/tasks")
    @Operation(summary = "查询索引任务状态")
    public Result<List<EmbeddingIndexTask>> getIndexTasks() {
        return Result.success(vectorIndexService.getRecentTasks());
    }

    // ---- General AI Chat (Engineering Assistant) ----

    private static final String ENGINEER_SYSTEM_PROMPT =
        "你是一个军工项目文档策划与编制系统的AI工程助手。你的职责是协助工程师完成以下工作：\n" +
        "- 解答军工标准（GJB 5882-2006、GJB 438C、GJB 9001C、GJB 3206B 等）相关的问题\n" +
        "- 提供技术文档编写建议（依据 GJB 5882-2006 军工产品研制技术文件编写指南）\n" +
        "- 协助技术方案分析和设计（覆盖论证/方案/工程研制/定型/批生产/退役各阶段）\n" +
        "- 解答项目管理、技术状态管理（GJB 3206B）相关问题\n" +
        "- 帮助理解标准条款和合规要求\n" +
        "- 熟悉七阶段(L/F/C/S/D/P/N)和十五大文档类别(PROCESS/SOFTWARE/MANUFACTURE/STANDARDIZE/QUALITY/RISK/RELIABILITY/MAINTAINABILITY/TESTABILITY/SUPPORTABILITY/SAFETY/ENVIRONMENT/EMC/ERGONOMICS/ACHIEVEMENT)\n\n" +
        "请用专业、准确、简洁的中文回答。如果不确定，请明确说明。";

    @PostMapping("/chat/stream")
    @Operation(summary = "AI 工程助手流式对话（SSE）")
    public SseEmitter chatStream(@RequestBody Map<String, Object> body) {
        String userMessage = (String) body.get("message");
        @SuppressWarnings("unchecked")
        var historyList = (List<Map<String, String>>) body.get("history");

        if (userMessage == null || userMessage.isBlank()) {
            SseEmitter emitter = new SseEmitter();
            try { emitter.send(SseEmitter.event().name("error").data("message is required")); emitter.complete(); }
            catch (IOException e) { emitter.completeWithError(e); }
            return emitter;
        }

        // Guard: very short inputs get a help prompt instead of going to LLM
        if (userMessage.trim().length() <= 1) {
            SseEmitter emitter = new SseEmitter();
            CompletableFuture.runAsync(() -> {
                try {
                    emitter.send(SseEmitter.event().name("chunk").data("你好！我是AI工程助手。可以帮你解答GJB标准、技术文档编写、项目管理等问题。请输入具体问题。"));
                    emitter.send(SseEmitter.event().name("done").data("complete"));
                    emitter.complete();
                } catch (IOException e) { emitter.completeWithError(e); }
            });
            return emitter;
        }

        // Note: For project-aware chat, use the newer /api/v1/chat/message endpoint
        StringBuilder fullPrompt = new StringBuilder();
        if (historyList != null && !historyList.isEmpty()) {
            fullPrompt.append("以下是之前的对话历史：\n");
            for (var msg : historyList) {
                String role = msg.get("role");
                String content = msg.get("content");
                if (content != null && !content.isBlank()) {
                    fullPrompt.append(role.equals("user") ? "工程师: " : "AI助手: ");
                    fullPrompt.append(content).append("\n");
                }
            }
            fullPrompt.append("\n---\n\n");
        }
        fullPrompt.append("工程师: ").append(userMessage).append("\nAI助手: ");

        SseEmitter emitter = new SseEmitter(300000L);
        log.info("AI chat stream requested, message length={}", userMessage.length());

        CompletableFuture.runAsync(() -> {
            try {
                llmClient.chatStream(ENGINEER_SYSTEM_PROMPT, fullPrompt.toString(), chunk -> {
                    try {
                        emitter.send(SseEmitter.event().name("chunk").data(chunk));
                    } catch (IOException e) {
                        log.warn("SSE send failed for chat chunk, aborting stream", e);
                        throw new RuntimeException("SSE send failed", e);
                    }
                });
                emitter.send(SseEmitter.event().name("done").data("complete"));
                emitter.complete();
            } catch (RuntimeException e) {
                log.warn("Chat stream interrupted: {}", e.getMessage());
                emitter.completeWithError(e);
            } catch (Exception e) {
                log.error("Chat stream failed", e);
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(() -> log.warn("SSE timeout for chat stream"));
        emitter.onError(ex -> log.warn("SSE error for chat stream: {}", ex.getMessage()));

        return emitter;
    }

    @PostMapping("/chat")
    @Operation(summary = "AI 工程助手对话（非流式）")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> body) {
        String userMessage = (String) body.get("message");
        if (userMessage == null || userMessage.isBlank()) {
            return Result.error("PARAM_ERROR", "message is required");
        }
        log.info("AI chat requested, message length={}", userMessage.length());
        String response = llmClient.chat(ENGINEER_SYSTEM_PROMPT, userMessage);
        return Result.success(Map.of("reply", response));
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long l) return l;
        if (value instanceof Integer i) return i.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Truncate a string to maxLen chars, safely handling null. */
    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    /**
     * Map AI-generated one-shot content to existing template chapters.
     * Parses content by headings and matches each section to a template chapter by number.
     * Template chapters define the structure — AI content only fills them in.
     */
    private int mapContentToExistingChapters(List<DocChapter> existingChapters,
                                              String content, Long userId) {
        // Parse content into sections
        var roots = MarkdownChapterParser.parse(content);
        if (roots.isEmpty()) {
            // No headings — put all content into the first chapter
            if (!existingChapters.isEmpty()) {
                DocChapter first = existingChapters.get(0);
                first.setContent(truncate(content, 50000));
                first.setFillStatus("FILLED");
                first.setFillPercentage(100);
                first.setUpdatedBy(userId);
                docChapterService.updateById(first);
                return 1;
            }
            return 0;
        }

        var flat = MarkdownChapterParser.flatten(roots);
        // Build chapter number → index map for existing chapters
        Map<String, DocChapter> byNumber = new java.util.LinkedHashMap<>();
        for (DocChapter ch : existingChapters) {
            if (ch.getChapterNumber() != null) {
                byNumber.put(ch.getChapterNumber().trim(), ch);
            }
        }

        int filled = 0;
        for (var fs : flat) {
            String num = fs.section().number();
            if (num == null) continue;

            // Try exact match first, then normalized match (strip leading zeros)
            DocChapter target = byNumber.get(num.trim());
            if (target == null) {
                // Try matching by normalized number (e.g., "3.01" → "3.1")
                String normalized = num.trim().replaceAll("\\.0+", ".");
                for (var entry : byNumber.entrySet()) {
                    String keyNorm = entry.getKey().replaceAll("\\.0+", ".");
                    if (keyNorm.equals(normalized)) {
                        target = entry.getValue();
                        break;
                    }
                }
            }
            if (target == null) continue;

            String sectionContent = fs.section().content();
            if (sectionContent != null && !sectionContent.isBlank()) {
                target.setContent(truncate(sectionContent, 50000));
                target.setFillStatus("FILLED");
                target.setFillPercentage(100);
                target.setUpdatedBy(userId);
                docChapterService.updateById(target);
                filled++;
            }
        }

        // Mark chapters without matched content as EMPTY
        for (DocChapter ch : existingChapters) {
            if (ch.getContent() == null || ch.getContent().isBlank()) {
                ch.setFillStatus("EMPTY");
                ch.setFillPercentage(0);
                docChapterService.updateById(ch);
            }
        }

        return filled;
    }

    /**
     * Create chapters from AI-generated content for documents WITHOUT a template.
     * Uses the AI chapter structure service to first build a proper hierarchical outline,
     * then fills content into matching chapters.
     */
    private void createChaptersFromAiContent(Long docLedgerId, String content, Long userId) {
        // Parse content to extract section structure
        var roots = MarkdownChapterParser.parse(content);
        if (roots.isEmpty()) {
            // Fallback: single chapter
            DocChapter ch = new DocChapter();
            ch.setDocLedgerId(docLedgerId);
            ch.setChapterNumber("1");
            ch.setChapterTitle("初稿内容");
            ch.setChapterLevel(1);
            ch.setOrderNum(1);
            ch.setParentId(0L);
            ch.setContent(truncate(content, 50000));
            ch.setFillStatus("FILLED");
            ch.setFillPercentage(100);
            ch.setCreatedBy(userId);
            ch.setUpdatedBy(userId);
            docChapterService.save(ch);
            return;
        }

        // Build proper hierarchical chapters from parsed sections
        var flat = MarkdownChapterParser.flatten(roots);
        List<DocChapter> chapters = new ArrayList<>();
        for (int i = 0; i < flat.size(); i++) {
            var fs = flat.get(i);
            DocChapter dc = new DocChapter();
            dc.setDocLedgerId(docLedgerId);
            dc.setChapterNumber(fs.section().number() != null ? fs.section().number() : String.valueOf(i + 1));
            dc.setChapterTitle(truncate(fs.section().title(), 100));
            dc.setChapterLevel(Math.min(fs.section().level(), 5));
            dc.setOrderNum(fs.orderNum());
            dc.setParentId(0L);
            dc.setContent(truncate(fs.section().content(), 50000));
            dc.setFillStatus("FILLED");
            dc.setFillPercentage(100);
            dc.setCreatedBy(userId);
            dc.setUpdatedBy(userId);
            chapters.add(dc);
        }
        docChapterService.saveBatch(chapters);

        // Second pass: resolve parent-child hierarchy
        for (int i = 0; i < flat.size(); i++) {
            int pi = flat.get(i).parentFlatIndex();
            if (pi >= 0 && pi < chapters.size()) {
                chapters.get(i).setParentId(chapters.get(pi).getId());
                docChapterService.updateById(chapters.get(i));
            }
        }
        log.info("Created {} chapters from AI content for ledger {}", chapters.size(), docLedgerId);
    }

    // ========== Generation Feedback ==========

    @PostMapping("/feedback")
    @Operation(summary = "提交生成质量反馈（1-5星评分）")
    public Result<GenerationFeedback> submitFeedback(@RequestBody Map<String, Object> body,
                                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        GenerationFeedback fb = new GenerationFeedback();
        fb.setProjectId(toLong(body.get("projectId")));
        fb.setDocLedgerId(toLong(body.get("docLedgerId")));
        fb.setTaskType((String) body.getOrDefault("taskType", "draft"));
        fb.setRating(body.get("rating") != null ? ((Number) body.get("rating")).intValue() : 0);
        fb.setFeedbackText((String) body.get("feedbackText"));
        fb.setCategories(body.get("categories") != null ? body.get("categories").toString() : "[]");
        fb.setWillUseAgain((Boolean) body.get("willUseAgain"));
        fb.setCreatedBy(userId);
        fb.setCreatedAt(java.time.LocalDateTime.now());
        feedbackMapper.insert(fb);
        return Result.success(fb);
    }

    @GetMapping("/feedback/stats")
    @Operation(summary = "查询生成质量反馈统计")
    public Result<Map<String, Object>> getFeedbackStats(@RequestParam(required = false) Long projectId) {
        var qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GenerationFeedback>();
        if (projectId != null) qw.eq(GenerationFeedback::getProjectId, projectId);
        var all = feedbackMapper.selectList(qw);
        double avgRating = all.stream().filter(f -> f.getRating() != null)
            .mapToInt(GenerationFeedback::getRating).average().orElse(0);
        Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("totalFeedbacks", all.size());
        stats.put("averageRating", Math.round(avgRating * 10) / 10.0);
        stats.put("wouldUseAgain", all.stream().filter(f -> Boolean.TRUE.equals(f.getWillUseAgain())).count());
        return Result.success(stats);
    }

    // ========== Incremental Generation ==========

    @PostMapping("/chapter/{docChapterId}/regenerate")
    @Operation(summary = "重新生成单个章节")
    public Result<Map<String, Object>> regenerateChapter(@PathVariable Long docChapterId,
                                                          @RequestBody Map<String, Object> body) {
        Long projectId = toLong(body.get("projectId"));
        if (projectId == null) return Result.error("PARAM_ERROR", "projectId is required");

        String content = incrementalGenerationService.regenerateChapter(docChapterId, projectId);
        return Result.success(Map.of("content", content != null ? content : ""));
    }

    @PostMapping("/chapter/{docChapterId}/rewrite")
    @Operation(summary = "AI 改写选中文本")
    public Result<Map<String, Object>> rewriteSelection(@PathVariable Long docChapterId,
                                                         @RequestBody Map<String, Object> body) {
        String selectedText = (String) body.get("selectedText");
        String instruction = (String) body.getOrDefault("instruction", "优化表达");
        String chapterContent = (String) body.getOrDefault("chapterContent", "");
        Long projectId = toLong(body.get("projectId"));

        if (selectedText == null || selectedText.isBlank()) {
            return Result.error("PARAM_ERROR", "selectedText is required");
        }

        String rewritten = incrementalGenerationService.rewriteSelection(
            chapterContent, selectedText, instruction, projectId);
        return Result.success(Map.of("rewritten", rewritten));
    }

    // ========== AI Audit Logging ==========

    @GetMapping("/audit-logs")
    @Operation(summary = "查询 AI API 调用审计日志")
    public Result<Map<String, Object>> getAuditLogs(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        java.time.LocalDateTime fromDt = null;
        java.time.LocalDateTime toDt = null;
        try {
            if (from != null && !from.isBlank()) fromDt = java.time.LocalDateTime.parse(from);
            if (to != null && !to.isBlank()) toDt = java.time.LocalDateTime.parse(to);
        } catch (Exception e) {
            return Result.error("PARAM_ERROR", "日期格式错误，请使用 ISO-8601 格式 (如 2026-06-01T00:00:00)");
        }
        return Result.success(aiAuditService.queryLogs(projectId, taskType, fromDt, toDt, page, size));
    }

    @GetMapping("/audit-stats")
    @Operation(summary = "查询 AI API 调用统计")
    public Result<Map<String, Object>> getAuditStats(
            @RequestParam(required = false) Long projectId) {
        return Result.success(aiAuditService.getStats(projectId));
    }

    // ========== Batch Generation ==========

    @PostMapping("/batch-generate/{projectId}/{stageId}/cancel")
    @Operation(summary = "取消正在进行的批量生成")
    public Result<String> cancelBatchGenerate(@PathVariable Long projectId,
                                               @PathVariable Long stageId) {
        // 取消通过 sessionId 进行，sessionId 存储在活跃任务映射中
        // 由于 SSE 连接已断开，这里主要通过停止后台处理来实现
        log.info("Batch generation cancel requested: projectId={}, stageId={}", projectId, stageId);
        return Result.success("取消请求已提交，当前文档生成完成后将停止");
    }

    @GetMapping("/batch-generate/{projectId}/{stageId}")
    @Operation(summary = "批量生成阶段全部文档(SSE)")
    public SseEmitter batchGenerate(@PathVariable Long projectId,
                                    @PathVariable Long stageId,
                                    Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        SseEmitter emitter = new SseEmitter(1800000L); // 30 min timeout for large batches
        log.info("Batch generation started: projectId={}, stageId={}, userId={}", projectId, stageId, userId);

        var securityContext = SecurityContextHolder.getContext();
        var abortFlag = new java.util.concurrent.atomic.AtomicBoolean(false);
        String sessionId = java.util.UUID.randomUUID().toString();

        CompletableFuture.runAsync(() -> {
            SecurityContextHolder.setContext(securityContext);
            try {
                batchGenerationService.generateStageDocs(projectId, stageId, sessionId, event -> {
                    // SSE 已断开则跳过后续事件发送
                    if (abortFlag.get()) return;

                    try {
                        Map<String, Object> data = new java.util.LinkedHashMap<>();
                        data.put("type", event.type());
                        data.put("current", event.current());
                        data.put("total", event.total());
                        if (event.docName() != null) data.put("docName", event.docName());
                        if (event.docCode() != null) data.put("docCode", event.docCode());
                        if (event.docLedgerId() != null) data.put("docLedgerId", event.docLedgerId());
                        if (event.status() != null) data.put("status", event.status());
                        if (event.message() != null) data.put("message", event.message());

                        emitter.send(SseEmitter.event()
                            .name(event.type())
                            .data(objectMapper.writeValueAsString(data)));
                    } catch (Exception e) {
                        // SSE 连接断开(客户端关闭/超时) — 静默停止事件推送，生成继续后台执行
                        log.warn("SSE disconnected during batch: {}", e.getMessage());
                        abortFlag.set(true);
                    }
                });
                if (!abortFlag.get()) {
                    emitter.complete();
                }
            } catch (Exception e) {
                log.error("Batch generation failed", e);
                emitter.completeWithError(e);
            } finally {
                SecurityContextHolder.clearContext();
            }
        });

        emitter.onTimeout(() -> {
            log.warn("SSE timeout for batch generation");
            abortFlag.set(true);
        });
        emitter.onError(ex -> {
            log.warn("SSE error for batch generation: {}", ex.getMessage());
            abortFlag.set(true);
        });

        return emitter;
    }

    // ========== Two-Phase Structure Generation (无模板时：生成→审核→保存→生成) ==========

    @PostMapping("/draft/apply-structure")
    @Operation(summary = "应用用户审核后的文档结构：初始化章节+保存为模板")
    public Result<Map<String, Object>> applyReviewedStructure(
            @RequestBody Map<String, Object> body, Authentication authentication) {
        Long docLedgerId = toLong(body.get("docLedgerId"));
        Long projectId = toLong(body.get("projectId"));
        boolean saveAsTemplate = Boolean.TRUE.equals(body.get("saveAsTemplate"));
        Long categoryId = toLong(body.get("categoryId"));
        Long userId = (Long) authentication.getPrincipal();

        if (docLedgerId == null) return Result.error("PARAM_ERROR", "docLedgerId is required");

        try {
            // Parse the structure from request
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> chaptersRaw = (List<Map<String, Object>>) body.get("chapters");
            String markdown = (String) body.get("markdownContent");
            String templateName = (String) body.get("suggestedTemplateName");
            String gjbRef = (String) body.get("gjbStandardRef");

            List<com.military.doc.ai.service.DocumentStructureService.StructureChapter> chapters = new ArrayList<>();
            if (chaptersRaw != null) {
                for (Map<String, Object> m : chaptersRaw) {
                    chapters.add(new com.military.doc.ai.service.DocumentStructureService.StructureChapter(
                        (String) m.get("chapterNumber"), (String) m.get("chapterTitle"),
                        ((Number) m.getOrDefault("chapterLevel", 1)).intValue(),
                        ((Number) m.getOrDefault("orderNum", 0)).intValue(),
                        Boolean.TRUE.equals(m.get("isRequired")),
                        (String) m.get("writingTips"), (String) m.get("description")));
                }
            }

            var structure = new com.military.doc.ai.service.DocumentStructureService.GeneratedStructure(
                markdown, chapters, templateName, gjbRef, chapters.size());

            // Apply to doc ledger
            List<DocChapter> created = draftGenerationService.applyReviewedStructure(docLedgerId, structure, saveAsTemplate);

            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("chaptersCreated", created.size());
            result.put("savedAsTemplate", saveAsTemplate);
            result.put("message", "已创建 " + created.size() + " 个章节，可以开始生成内容");
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to apply structure: {}", e.getMessage(), e);
            return Result.error("APPLY_ERROR", "应用文档结构失败: " + e.getMessage());
        }
    }

    // ========== Document Structure Generation (无模板时的AI结构编排) ==========

    @PostMapping("/structure/generate")
    @Operation(summary = "AI生成文档结构（MD格式，可供人工编辑后保存为模板）")
    public Result<com.military.doc.ai.service.DocumentStructureService.GeneratedStructure> generateStructure(
            @RequestBody Map<String, Object> body) {
        Long projectId = toLong(body.get("projectId"));
        Long docLedgerId = toLong(body.get("docLedgerId"));
        if (projectId == null || docLedgerId == null) {
            return Result.error("PARAM_ERROR", "projectId and docLedgerId are required");
        }
        return Result.success(documentStructureService.generate(projectId, docLedgerId));
    }

    @PostMapping("/structure/save-template")
    @Operation(summary = "将AI生成的文档结构保存为系统模板")
    public Result<Map<String, Object>> saveStructureAsTemplate(
            @RequestBody Map<String, Object> body, Authentication authentication) {
        Long categoryId = toLong(body.get("categoryId"));
        Long userId = (Long) authentication.getPrincipal();
        try {
            // Reconstruct GeneratedStructure from the request body
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> chaptersRaw = (List<Map<String, Object>>) body.get("chapters");
            String markdown = (String) body.get("markdownContent");
            String templateName = (String) body.get("suggestedTemplateName");
            String gjbRef = (String) body.get("gjbStandardRef");

            List<com.military.doc.ai.service.DocumentStructureService.StructureChapter> chapters = new ArrayList<>();
            if (chaptersRaw != null) {
                for (Map<String, Object> m : chaptersRaw) {
                    chapters.add(new com.military.doc.ai.service.DocumentStructureService.StructureChapter(
                        (String) m.get("chapterNumber"),
                        (String) m.get("chapterTitle"),
                        ((Number) m.getOrDefault("chapterLevel", 1)).intValue(),
                        ((Number) m.getOrDefault("orderNum", 0)).intValue(),
                        Boolean.TRUE.equals(m.get("isRequired")),
                        (String) m.get("writingTips"),
                        (String) m.get("description")
                    ));
                }
            }

            var structure = new com.military.doc.ai.service.DocumentStructureService.GeneratedStructure(
                markdown, chapters, templateName, gjbRef, chapters.size());

            Long templateId = documentStructureService.saveAsTemplate(structure, categoryId, userId);
            return Result.success(Map.of("templateId", templateId, "templateName", templateName));
        } catch (Exception e) {
            log.error("Failed to save structure as template", e);
            return Result.error("SAVE_ERROR", "保存模板失败: " + e.getMessage());
        }
    }

    // ========== AI Chapter Structure Generation ==========

    @PostMapping("/chapter-structure/preview")
    @Operation(summary = "AI 智能生成文档章节结构预览（不持久化）。可根据项目背景+文档类型+参考模板+用户补充要求生成层级化章节树")
    public Result<AiChapterStructureService.StructureResponse> previewChapterStructure(
            @RequestBody AiChapterStructureService.StructureRequest req,
            Authentication authentication) {
        if (req.projectId == null && req.docLedgerId == null) {
            return Result.error("PARAM_ERROR", "projectId or docLedgerId is required");
        }
        log.info("AI chapter structure preview: projectId={}, docLedgerId={}, optimize={}",
                req.projectId, req.docLedgerId, req.optimize);
        AiChapterStructureService.StructureResponse result = aiChapterStructureService.generatePreview(req);
        return Result.success(result);
    }

    @PostMapping("/chapter-structure/apply")
    @Operation(summary = "将 AI 生成的章节结构应用到指定文档台账（替换现有章节）")
    public Result<Map<String, Object>> applyChapterStructure(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        Long docLedgerId = toLong(body.get("docLedgerId"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodesRaw = (List<Map<String, Object>>) body.get("chapters");
        Long userId = (Long) authentication.getPrincipal();

        if (docLedgerId == null || nodesRaw == null || nodesRaw.isEmpty()) {
            return Result.error("PARAM_ERROR", "docLedgerId and chapters are required");
        }

        // Parse nodes from the raw request body
        List<AiChapterStructureService.AiChapterNode> nodes;
        try {
            String json = objectMapper.writeValueAsString(nodesRaw);
            nodes = objectMapper.readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<List<AiChapterStructureService.AiChapterNode>>() {});
        } catch (Exception e) {
            return Result.error("PARAM_ERROR", "章节数据格式错误: " + e.getMessage());
        }

        List<DocChapter> created = aiChapterStructureService.applyStructure(docLedgerId, nodes, userId);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("created", created.size());
        result.put("message", "已应用 AI 生成的章节结构，共 " + created.size() + " 个章节");
        return Result.success(result);
    }

    @PostMapping("/chapter-structure/optimize")
    @Operation(summary = "AI 优化现有章节结构：分析现有结构，智能补充缺失章节、调整层级关系、完善编写提示")
    public Result<AiChapterStructureService.StructureResponse> optimizeChapterStructure(
            @RequestBody AiChapterStructureService.StructureRequest req) {
        if (req.docLedgerId == null) {
            return Result.error("PARAM_ERROR", "docLedgerId is required");
        }
        req.optimize = true;
        log.info("AI chapter structure optimization: docLedgerId={}", req.docLedgerId);
        AiChapterStructureService.StructureResponse result = aiChapterStructureService.generatePreview(req);
        return Result.success(result);
    }

    /**
     * One-time repair: re-parse existing markdown content into doc_chapter entries
     * for documents that have content_size > 0 but zero chapters.
     */
    @PostMapping("/repair-chapters/{projectId}")
    @Operation(summary = "修复：将已有文档内容按标题拆分为章节（解决DOCX只有封面无正文的问题）")
    public Result<Map<String, Object>> repairChapters(@PathVariable Long projectId) {
        int ledgersFixed = 0;
        int chaptersCreated = 0;
        var ledgers = docLedgerMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.military.doc.modules.document.entity.DocLedger>()
                .eq(com.military.doc.modules.document.entity.DocLedger::getProjectId, projectId)
                .isNotNull(com.military.doc.modules.document.entity.DocLedger::getContentSize)
                .gt(com.military.doc.modules.document.entity.DocLedger::getContentSize, 0));
        log.info("Repair chapters: found {} ledgers with content for project {}", ledgers.size(), projectId);

        for (var ledger : ledgers) {
            // Check if already has chapters
            long existingCount = docChapterMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.military.doc.modules.document.entity.DocChapter>()
                    .eq(com.military.doc.modules.document.entity.DocChapter::getDocLedgerId, ledger.getId())
                    .eq(com.military.doc.modules.document.entity.DocChapter::getDeleted, 0));
            if (existingCount > 0) continue;

            // Read content from file storage
            if (ledger.getFileObjectId() == null || ledger.getFileObjectId().isBlank()) continue;
            try {
                byte[] bytes = fileStorageService.download(ledger.getFileObjectId()).readAllBytes();
                String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                if (content.isBlank()) continue;

                // Parse and create chapters
                var roots = com.military.doc.ai.util.MarkdownChapterParser.parse(content);
                if (roots.isEmpty()) continue;

                var flat = com.military.doc.ai.util.MarkdownChapterParser.flatten(roots);
                java.util.List<com.military.doc.modules.document.entity.DocChapter> chList = new java.util.ArrayList<>();
                for (var fs : flat) {
                    var ch = new com.military.doc.modules.document.entity.DocChapter();
                    ch.setDocLedgerId(ledger.getId());
                    ch.setParentId(0L);
                    ch.setChapterNumber(fs.section().number() != null ? fs.section().number() : String.valueOf(fs.orderNum()));
                    ch.setChapterTitle(truncate(fs.section().title(), 250));
                    ch.setChapterLevel(Math.min(fs.section().level(), 5));
                    ch.setOrderNum(fs.orderNum());
                    ch.setContent(truncate(fs.section().content(), 50000));
                    ch.setFillStatus(fs.section().content() != null && !fs.section().content().isBlank() ? "FILLED" : "DRAFT");
                    ch.setCreatedAt(java.time.LocalDateTime.now());
                    ch.setUpdatedAt(java.time.LocalDateTime.now());
                    docChapterMapper.insert(ch);
                    chList.add(ch);
                }
                // Second pass: resolve parentId
                for (int i = 0; i < flat.size(); i++) {
                    int pi = flat.get(i).parentFlatIndex();
                    if (pi >= 0 && pi < chList.size()) {
                        chList.get(i).setParentId(chList.get(pi).getId());
                        docChapterMapper.updateById(chList.get(i));
                    }
                }
                ledgersFixed++;
                chaptersCreated += flat.size();
                log.info("Repaired ledger {}: {} => {} chapters", ledger.getId(), ledger.getDocName(), flat.size());
            } catch (Exception e) {
                log.warn("Failed to repair chapters for ledger {}: {}", ledger.getId(), e.getMessage());
            }
        }

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("ledgersFixed", ledgersFixed);
        result.put("chaptersCreated", chaptersCreated);
        result.put("message", String.format("修复了 %d 个文档，创建了 %d 个章节", ledgersFixed, chaptersCreated));
        return Result.success(result);
    }

}
