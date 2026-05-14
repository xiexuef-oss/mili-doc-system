package com.military.doc.ai.controller;

import com.military.doc.ai.llm.OllamaClient;
import com.military.doc.ai.context.VectorIndexService;
import com.military.doc.ai.entity.EmbeddingIndexTask;
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
import com.military.doc.common.result.Result;
import com.military.doc.common.storage.FileStorageService;
import com.military.doc.config.LlmProperties;
import com.military.doc.modules.document.entity.DocCatalog;
import com.military.doc.modules.document.entity.DocFile;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.entity.DocVersion;
import com.military.doc.modules.document.service.DocFileService;
import com.military.doc.modules.document.service.DocLedgerService;
import com.military.doc.modules.document.service.DocVersionService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI 智能助手")
public class AiAssistantController {

    @Autowired
    private CatalogGenerationService catalogGenerationService;

    @Autowired
    private DraftGenerationService draftGenerationService;

    @Autowired
    private TrainingDataService trainingDataService;

    @Autowired
    private ProofreadingService proofreadingService;

    @Autowired
    private PreReviewService preReviewService;

    @Autowired
    private ComplianceCheckService complianceCheckService;

    @Autowired
    private OpinionSummaryService opinionSummaryService;

    @Autowired
    private StageReadinessService stageReadinessService;

    @Autowired
    private ArchiveAdvisorService archiveAdvisorService;

    @Autowired
    private ChangeImpactService changeImpactService;

    @Autowired
    private VectorIndexService vectorIndexService;

    @Autowired
    private DocFileService docFileService;

    @Autowired
    private DocVersionService docVersionService;

    @Autowired
    private DocLedgerService docLedgerService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private LlmProperties llmProperties;

    @Autowired
    private OkHttpClient httpClient;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/catalog/generate")
    @Operation(summary = "根据项目输入文件和适用标准自动生成文档目录")
    public Result<List<DocCatalog>> generateCatalog(@RequestBody Map<String, Object> body,
                                                     Authentication authentication) {
        Long projectId = toLong(body.get("projectId"));
        Long stageId = toLong(body.get("stageId"));
        boolean overwrite = Boolean.TRUE.equals(body.get("overwrite"));
        Long userId = (Long) authentication.getPrincipal();

        log.info("Catalog generation requested: projectId={}, stageId={}, overwrite={}, userId={}",
            projectId, stageId, overwrite, userId);

        if (projectId == null) {
            return Result.error("PARAM_ERROR", "projectId is required");
        }

        List<DocCatalog> catalogs = catalogGenerationService.generate(projectId, stageId, userId, overwrite);
        return Result.success(catalogs);
    }

    @GetMapping("/health")
    @Operation(summary = "检查本地大模型连接状态")
    public Result<Map<String, Object>> health() {
        Map<String, Object> status = new java.util.LinkedHashMap<>();
        status.put("provider", llmProperties.getProvider());
        status.put("model", llmProperties.getModel());
        status.put("baseUrl", llmProperties.getBaseUrl());
        status.put("connected", false);
        status.put("modelLoaded", false);

        try {
            Request request = new Request.Builder()
                .url(llmProperties.getBaseUrl() + "/api/tags")
                .get()
                .build();
            try (var response = httpClient.newCall(request).execute()) {
                status.put("connected", response.isSuccessful());
                status.put("httpStatus", response.code());
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    var node = objectMapper.readTree(body);
                    var models = node.get("models");
                    if (models != null && models.isArray()) {
                        var modelNames = new java.util.ArrayList<String>();
                        for (var m : models) {
                            String name = m.has("name") ? m.get("name").asText() : "";
                            modelNames.add(name);
                            if (name.equals(llmProperties.getModel()) ||
                                name.startsWith(llmProperties.getModel() + ":")) {
                                status.put("modelLoaded", true);
                            }
                        }
                        status.put("availableModels", modelNames);
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
    @Operation(summary = "流式生成文档初稿（SSE）")
    public SseEmitter streamDraft(@RequestParam Long projectId,
                                   @RequestParam Long catalogId,
                                   Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        SseEmitter emitter = new SseEmitter(300000L);
        log.info("Draft stream requested: projectId={}, catalogId={}, userId={}", projectId, catalogId, userId);

        CompletableFuture.runAsync(() -> {
            try {
                draftGenerationService.generateStream(projectId, catalogId, chunk -> {
                    try {
                        emitter.send(SseEmitter.event().name("chunk").data(chunk));
                    } catch (IOException e) {
                        log.warn("SSE send failed for chunk, aborting stream", e);
                        throw new RuntimeException("SSE send failed", e);
                    }
                });
                emitter.send(SseEmitter.event().name("done").data("complete"));
                emitter.complete();
            } catch (RuntimeException e) {
                log.warn("Draft stream interrupted: {}", e.getMessage());
                emitter.completeWithError(e);
            } catch (Exception e) {
                log.error("Draft stream failed", e);
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(() -> log.warn("SSE timeout for draft stream"));
        emitter.onError(ex -> log.warn("SSE error for draft stream: {}", ex.getMessage()));

        return emitter;
    }

    @PostMapping("/draft/save")
    @Operation(summary = "保存生成的文档初稿，内容存储为 Markdown 文件并创建版本记录")
    public Result<DocFile> saveDraft(@RequestBody Map<String, Object> body,
                                      Authentication authentication) {
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

        // 1. Create DocFile metadata record (backward compat)
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

        // 2. Save content as .md file
        String fileObjectId = null;
        if (content != null && !content.isBlank()) {
            String filename = (docName != null ? docName : "draft") + ".md";
            fileObjectId = fileStorageService.upload(
                content.getBytes(StandardCharsets.UTF_8), filename);
        }

        // 3. Create initial version record
        DocVersion version = new DocVersion();
        version.setDocFileId(docFile.getId());
        version.setVersionNo("V0.1-AI");
        version.setSourceType("AI_GENERATED");
        version.setFileObjectId(fileObjectId);
        version.setVersionStatus("DRAFT");
        version.setOptimisticVersion(1);
        version.setSubmitUserId(userId);
        version.setSubmitTime(LocalDateTime.now());
        version.setChangeSummary("AI 自动生成初稿");
        version.setCreatedBy(userId);
        version.setCreatedAt(LocalDateTime.now());
        docVersionService.save(version);

        // 4. Create/update DocLedger with DRAFTING status
        DocLedger ledger = new DocLedger();
        ledger.setProjectId(projectId);
        ledger.setStageId(stageId);
        ledger.setCatalogId(catalogId);
        ledger.setDocName(docName != null ? docName : "AI 生成文档");
        ledger.setDocType(docType != null ? docType : "MANAGEMENT_DOC");
        ledger.setSecurityLevel(securityLevel);
        ledger.setFileObjectId(fileObjectId);
        docLedgerService.createLedger(ledger, userId);
        docLedgerService.transitionStatus(ledger.getId(), "DRAFTING", userId, "AI 生成初稿后自动转入起草状态");

        log.info("Draft saved: docFileId={}, ledgerId={}, versionId={}, catalogId={}",
            docFile.getId(), ledger.getId(), version.getId(), catalogId);
        return Result.success(docFile);
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
}
