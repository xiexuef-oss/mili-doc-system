package com.military.doc.ai.controller;

import com.military.doc.ai.llm.LlmClient;
import com.military.doc.ai.llm.LlmProviderService;
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
import org.springframework.beans.factory.annotation.Autowired;
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
    private DocChapterService docChapterService;

    @Autowired
    private LlmProperties llmProperties;

    @Autowired
    private LlmProviderService llmProviderService;

    @Autowired
    private LlmClient llmClient;

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

    @GetMapping("/health")
    @Operation(summary = "检查大模型连接状态")
    public Result<Map<String, Object>> health() {
        Map<String, Object> status = new java.util.LinkedHashMap<>();
        status.put("provider", llmProperties.getProvider());
        status.put("model", llmProperties.getModel());
        status.put("baseUrl", llmProperties.getBaseUrl());
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
    @Operation(summary = "流式生成文档初稿（SSE）")
    public SseEmitter streamDraft(@RequestParam Long projectId,
                                   @RequestParam(required = false) Long catalogId,
                                   @RequestParam(required = false) Long docLedgerId,
                                   Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        SseEmitter emitter = new SseEmitter(300000L);
        log.info("Draft stream requested: projectId={}, catalogId={}, docLedgerId={}, userId={}", projectId, catalogId, docLedgerId, userId);

        // Capture security context for async thread
        var securityContext = SecurityContextHolder.getContext();

        CompletableFuture.runAsync(() -> {
            SecurityContextHolder.setContext(securityContext);
            try {
                draftGenerationService.generateStream(projectId, catalogId, docLedgerId, chunk -> {
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
            } finally {
                SecurityContextHolder.clearContext();
            }
        });

        emitter.onTimeout(() -> log.warn("SSE timeout for draft stream"));
        emitter.onError(ex -> log.warn("SSE error for draft stream: {}", ex.getMessage()));

        return emitter;
    }

    @PostMapping("/draft/save")
    @Operation(summary = "保存AI生成初稿，新建起草台账（策划列原条目不动），同目录多次生成则覆盖")
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

        // 6. Update ledger file reference to latest
        ledger.setFileObjectId(fileObjectId);
        ledger.setUpdatedBy(userId);
        docLedgerService.updateById(ledger);

        // 7. Parse markdown into hierarchical chapter tree
        if (content != null && !content.isBlank()) {
            String safeContent = content.length() > 100_000 ? content.substring(0, 100_000) : content;

            // Delete old chapters when overwriting
            if (!isNew) {
                docChapterService.remove(new LambdaQueryWrapper<DocChapter>()
                    .eq(DocChapter::getDocLedgerId, ledger.getId()));
            }

            List<Map<String, Object>> segments = parseMarkdownToSegments(safeContent);

            if (segments.isEmpty()) {
                // No headings found — create a single fallback chapter
                DocChapter chapter = new DocChapter();
                chapter.setDocLedgerId(ledger.getId());
                chapter.setChapterNumber("1");
                chapter.setChapterTitle("初稿内容");
                chapter.setChapterLevel(1);
                chapter.setOrderNum(1);
                chapter.setParentId(0L);
                chapter.setContent(safeContent);
                chapter.setFillStatus("DRAFT");
                chapter.setFillPercentage(100);
                chapter.setCreatedBy(userId);
                chapter.setUpdatedBy(userId);
                docChapterService.save(chapter);
                log.info("Created single fallback chapter for ledger {}", ledger.getId());
            } else {
                // Build chapters from parsed segments
                List<DocChapter> chapters = new ArrayList<>();
                for (int i = 0; i < segments.size(); i++) {
                    Map<String, Object> seg = segments.get(i);
                    DocChapter dc = new DocChapter();
                    dc.setDocLedgerId(ledger.getId());
                    dc.setChapterNumber(String.valueOf(i + 1)); // placeholder, recomputed below
                    dc.setChapterTitle((String) seg.get("title"));
                    dc.setChapterLevel((Integer) seg.get("level"));
                    dc.setOrderNum(i + 1);
                    dc.setParentId(0L); // placeholder, resolved below
                    String chapterBody = (String) seg.get("body");
                    dc.setContent(chapterBody);
                    dc.setFillStatus("DRAFT");
                    dc.setFillPercentage(chapterBody != null && !chapterBody.isBlank() ? 100 : 0);
                    dc.setCreatedBy(userId);
                    dc.setUpdatedBy(userId);
                    chapters.add(dc);
                }
                docChapterService.saveBatch(chapters);

                // Resolve parent IDs: each chapter's parent is the nearest prior chapter with a lower level
                for (int i = 0; i < chapters.size(); i++) {
                    DocChapter dc = chapters.get(i);
                    int level = dc.getChapterLevel();
                    for (int j = i - 1; j >= 0; j--) {
                        if (chapters.get(j).getChapterLevel() < level) {
                            dc.setParentId(chapters.get(j).getId());
                            break;
                        }
                    }
                }

                // Compute hierarchical chapter numbers (1, 1.1, 1.2, 2, 2.1, ...)
                java.util.Map<Long, Integer> counters = new java.util.LinkedHashMap<>();
                for (DocChapter dc : chapters) {
                    Long pid = dc.getParentId();
                    int counter = counters.getOrDefault(pid, 0) + 1;
                    counters.put(pid, counter);

                    StringBuilder num = new StringBuilder();
                    if (pid != 0L) {
                        String parentNum = "";
                        for (DocChapter c : chapters) {
                            if (c.getId().equals(pid)) { parentNum = c.getChapterNumber(); break; }
                        }
                        if (!parentNum.isEmpty()) num.append(parentNum).append(".");
                    }
                    num.append(counter);
                    dc.setChapterNumber(num.toString());
                }

                docChapterService.updateBatchById(chapters);
                log.info("Created {} chapters from markdown for ledger {}", chapters.size(), ledger.getId());
            }
        }

        log.info("Draft saved: docFileId={}, ledgerId={}, version={}, catalogId={}, isNew={}",
            docFile.getId(), ledger.getId(), versionNo, catalogId, isNew);
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
            try {
                emitter.send(SseEmitter.event().name("error").data("message is required"));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        // Build the full prompt: include history as context
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

    /**
     * Parse markdown content into heading-body segments.
     * Each returned map has keys: "level" (Integer), "title" (String), "body" (String).
     * Content before the first heading becomes a preamble segment with level 1.
     */
    private List<Map<String, Object>> parseMarkdownToSegments(String markdown) {
        List<Map<String, Object>> segments = new ArrayList<>();
        String[] lines = markdown.split("\n");

        int currentLevel = 0;
        String currentTitle = null;
        StringBuilder currentBody = new StringBuilder();

        for (String line : lines) {
            int headingLevel = getHeadingLevel(line);
            if (headingLevel > 0) {
                // Flush previous segment
                if (currentTitle != null || currentBody.length() > 0) {
                    Map<String, Object> seg = new java.util.LinkedHashMap<>();
                    seg.put("level", currentLevel > 0 ? currentLevel : 1);
                    seg.put("title", currentTitle != null ? currentTitle : "文档内容");
                    seg.put("body", currentBody.toString().trim());
                    segments.add(seg);
                }
                currentLevel = headingLevel;
                currentTitle = line.substring(headingLevel).trim().replaceAll("\\s*#+\\s*$", "");
                currentBody = new StringBuilder();
            } else if (currentTitle != null || currentBody.length() > 0) {
                if (currentBody.length() > 0) currentBody.append("\n");
                currentBody.append(line);
            }
        }

        // Flush last segment
        if (currentTitle != null || currentBody.length() > 0) {
            Map<String, Object> seg = new java.util.LinkedHashMap<>();
            seg.put("level", currentLevel > 0 ? currentLevel : 1);
            seg.put("title", currentTitle != null ? currentTitle : "文档内容");
            seg.put("body", currentBody.toString().trim());
            segments.add(seg);
        }

        return segments;
    }

    /** Returns 1-6 for ATX headings like "# Title", "## Subtitle", or "##Title" (without space), or 0 if not a heading. */
    private int getHeadingLevel(String line) {
        if (line == null || line.isBlank()) return 0;
        int level = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '#') {
                level++;
            } else if (c == ' ' || c == '\t') {
                return (level >= 1 && level <= 6) ? level : 0;
            } else {
                // Non-space/non-# character immediately after # markers → treat as heading text
                // e.g., "##Title" or "#1. Scope" should still be recognized
                return (level >= 1 && level <= 6) ? level : 0;
            }
        }
        return (level >= 1 && level <= 6) ? level : 0;
    }

}
