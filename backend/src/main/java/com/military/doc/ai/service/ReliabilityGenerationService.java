package com.military.doc.ai.service;

import com.military.doc.ai.context.ContextAssemblyService;
import com.military.doc.ai.llm.LlmClient;
import com.military.doc.ai.prompt.PromptTemplateService;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.entity.ProjectDocChecklist;
import com.military.doc.modules.document.entity.StageDocChecklistTemplate;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import com.military.doc.modules.document.entity.DocVersion;
import com.military.doc.modules.document.mapper.DocVersionMapper;
import com.military.doc.modules.document.mapper.DocChapterMapper;
import com.military.doc.common.storage.FileStorageService;
import com.military.doc.modules.document.mapper.ProjectDocChecklistMapper;
import com.military.doc.modules.document.mapper.StageDocChecklistTemplateMapper;
import com.military.doc.modules.project.entity.Project;
import com.military.doc.modules.project.mapper.ProjectMapper;
import com.military.doc.modules.reliability.entity.RelRequirement;
import com.military.doc.modules.reliability.mapper.RelRequirementMapper;
import com.military.doc.modules.template.entity.DocTemplateChapter;
import com.military.doc.modules.template.mapper.DocTemplateChapterMapper;
import com.military.doc.common.util.Str;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import com.military.doc.modules.document.entity.DocChapter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 可靠性文档专用生成服务。
 * 复用现有 DraftGenerationService 的逐章生成能力，
 * 为 8 类可靠性文档提供 A档(纯文本)和 B档(计算+文本)的生成入口。
 */
@Slf4j
@Service
public class ReliabilityGenerationService {

    private final DraftGenerationService draftGenerationService;
    private final ContextAssemblyService contextAssemblyService;
    private final PromptTemplateService promptTemplateService;
    private final LlmClient llmClient;
    private final ProjectMapper projectMapper;
    private final DocLedgerMapper docLedgerMapper;
    private final StageDocChecklistTemplateMapper checklistTemplateMapper;
    private final ProjectDocChecklistMapper checklistItemMapper;
    private final DocTemplateChapterMapper tplChapterMapper;
    private final RelRequirementMapper relRequirementMapper;
    private final DocChapterMapper docChapterMapper;
    private final FileStorageService fileStorageService;
    private final DocVersionMapper docVersionMapper;

    private static final int MAX_CONTEXT_TOKENS = 50_000;

    public ReliabilityGenerationService(
            DraftGenerationService draftGenerationService,
            ContextAssemblyService contextAssemblyService,
            PromptTemplateService promptTemplateService,
            LlmClient llmClient,
            ProjectMapper projectMapper,
            DocLedgerMapper docLedgerMapper,
            StageDocChecklistTemplateMapper checklistTemplateMapper,
            ProjectDocChecklistMapper checklistItemMapper,
            DocTemplateChapterMapper tplChapterMapper,
            RelRequirementMapper relRequirementMapper,
            DocChapterMapper docChapterMapper,
            FileStorageService fileStorageService,
            DocVersionMapper docVersionMapper) {
        this.draftGenerationService = draftGenerationService;
        this.contextAssemblyService = contextAssemblyService;
        this.promptTemplateService = promptTemplateService;
        this.llmClient = llmClient;
        this.projectMapper = projectMapper;
        this.docLedgerMapper = docLedgerMapper;
        this.checklistTemplateMapper = checklistTemplateMapper;
        this.checklistItemMapper = checklistItemMapper;
        this.tplChapterMapper = tplChapterMapper;
        this.relRequirementMapper = relRequirementMapper;
        this.docChapterMapper = docChapterMapper;
        this.fileStorageService = fileStorageService;
        this.docVersionMapper = docVersionMapper;
    }

    // ==================== A 档：纯文本生成 ====================

    /**
     * 生成可靠性大纲。
     * 使用 reliability-outline prompt + 模板章节结构，逐章生成。
     */
    public String generateReliabilityOutline(Long projectId, Long docLedgerId) {
        return generateReliabilityDoc(projectId, docLedgerId, "reliability-outline");
    }

    /**
     * 生成降额设计报告。
     */
    public String generateDeratingReport(Long projectId, Long docLedgerId) {
        return generateReliabilityDoc(projectId, docLedgerId, "reliability-derating");
    }

    /**
     * 串流生成可靠性大纲。
     */
    public void generateReliabilityOutlineStream(Long projectId, Long docLedgerId, Consumer<String> onChunk) {
        generateReliabilityDocStream(projectId, docLedgerId, "reliability-outline", onChunk);
    }

    /**
     * 串流生成降额设计报告。
     */
    public void generateDeratingReportStream(Long projectId, Long docLedgerId, Consumer<String> onChunk) {
        generateReliabilityDocStream(projectId, docLedgerId, "reliability-derating", onChunk);
    }

    // ==================== B 档：计算 + 文本混合 ====================

    /**
     * 生成可靠性预计报告（B档）。
     * 调用 ReliabilityPredictor 计算 → 将结果注入上下文 → LLM 撰写报告。
     * 注意：ReliabilityPredictor 在 Phase 4 实现，当前方法预留接口。
     */
    public String generatePredictionReport(Long projectId, Long docLedgerId,
                                            String predictionResultJson) {
        String projectCtx = contextAssemblyService.assembleContext(projectId);
        StringBuilder ctx = new StringBuilder(projectCtx);
        ctx.append("\n\n## 预计计算结果\n```json\n")
           .append(predictionResultJson != null ? predictionResultJson : "{}")
           .append("\n```\n");

        String context = ctx.toString();
        if (Str.estimateTokens(context) > MAX_CONTEXT_TOKENS) {
            context = Str.truncateByTokens(context, MAX_CONTEXT_TOKENS);
        }

        String systemPrompt = promptTemplateService.getTemplate("reliability-prediction-report");
        if (systemPrompt.isEmpty()) {
            systemPrompt = "你是军工可靠性预计专家，请根据计算数据撰写预计报告。";
        }
        String userPrompt = promptTemplateService.render("reliability-prediction-report",
            Map.of("context", context));

        log.info("Prediction report generation: projectId={}, docLedgerId={}, prompt {} chars",
            projectId, docLedgerId, userPrompt.length());
        return llmClient.chat(systemPrompt, userPrompt);
    }

    /**
     * 生成可靠性分配报告（B档）。
     * 调用 ReliabilityAllocator 计算 → 将结果注入上下文 → LLM 撰写报告。
     */
    public String generateAllocationReport(Long projectId, Long docLedgerId,
                                            String allocationResultJson) {
        String projectCtx = contextAssemblyService.assembleContext(projectId);
        StringBuilder ctx = new StringBuilder(projectCtx);
        ctx.append("\n\n## 分配计算结果\n```json\n")
           .append(allocationResultJson != null ? allocationResultJson : "{}")
           .append("\n```\n");

        String context = ctx.toString();
        if (Str.estimateTokens(context) > MAX_CONTEXT_TOKENS) {
            context = Str.truncateByTokens(context, MAX_CONTEXT_TOKENS);
        }

        String systemPrompt = promptTemplateService.getTemplate("reliability-allocation-report");
        if (systemPrompt.isEmpty()) {
            systemPrompt = "你是军工可靠性分配专家，请根据分配数据撰写分配报告。";
        }
        String userPrompt = promptTemplateService.render("reliability-allocation-report",
            Map.of("context", context));

        log.info("Allocation report generation: projectId={}, docLedgerId={}, prompt {} chars",
            projectId, docLedgerId, userPrompt.length());
        return llmClient.chat(systemPrompt, userPrompt);
    }

    // ==================== 辅助方法 ====================

    /**
     * 通用可靠性文档生成：找模板 → 逐章生成 → 组装 Markdown。
     */
    private String generateReliabilityDoc(Long projectId, Long docLedgerId, String promptName) {
        Long templateId = findReliabilityTemplate(docLedgerId);
        if (templateId == null) {
            // 无模板时回退到 one-shot 生成
            return generateOneShot(projectId, docLedgerId, promptName);
        }
        return generateByRelTemplate(projectId, templateId, promptName);
    }

    private void generateReliabilityDocStream(Long projectId, Long docLedgerId,
                                               String promptName, Consumer<String> onChunk) {
        Long templateId = findReliabilityTemplate(docLedgerId);
        if (templateId == null) {
            generateOneShotStream(projectId, docLedgerId, promptName, onChunk);
            return;
        }
        generateByRelTemplateStream(projectId, templateId, promptName, onChunk);
    }

    /**
     * 从 docLedger 查找关联的可靠性模板 ID。
     */
    private Long findReliabilityTemplate(Long docLedgerId) {
        if (docLedgerId == null) return null;
        try {
            DocLedger ledger = docLedgerMapper.selectById(docLedgerId);
            if (ledger == null || ledger.getChecklistItemId() == null) return null;
            ProjectDocChecklist pdc = checklistItemMapper.selectById(ledger.getChecklistItemId());
            if (pdc == null || pdc.getTemplateId() == null) return null;
            StageDocChecklistTemplate sct = checklistTemplateMapper.selectById(pdc.getTemplateId());
            return sct != null ? sct.getTemplateId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 基于可靠性模板逐章生成。
     */
    private String generateByRelTemplate(Long projectId, Long templateId, String promptName) {
        var chapters = tplChapterMapper.selectList(
            new LambdaQueryWrapper<DocTemplateChapter>()
                .eq(DocTemplateChapter::getTemplateId, templateId)
                .orderByAsc(DocTemplateChapter::getOrderNum));

        if (chapters == null || chapters.isEmpty()) {
            return generateOneShot(projectId, null, promptName);
        }

        var required = chapters.stream()
            .filter(ch -> Boolean.TRUE.equals(ch.getIsRequired()))
            .toList();
        
        if (required.isEmpty()) return generateOneShot(projectId, null, promptName);

        String projectCtx = buildReliabilityContext(projectId, promptName);
        
        // Parallel generation with 4 threads
        StringBuilder doc = new StringBuilder();
        int generated = 0;
        var pool = java.util.concurrent.Executors.newFixedThreadPool(4);
        try {
        var futures = new java.util.ArrayList<java.util.concurrent.Future<String[]>>();

        for (var ch : required) {
            futures.add(pool.submit(() -> {
                try {
                    String body = draftGenerationService.generateChapterByTemplate(projectId, ch, projectCtx);
                    if (body != null && !body.isBlank()) {
                        int lv = Math.min(ch.getChapterLevel() != null ? ch.getChapterLevel() : 1, 5);
                        String heading = "#".repeat(lv) + " " + ch.getChapterNumber() + " " + ch.getChapterTitle();
                        return new String[]{heading, body};
                    }
                } catch (Exception e) {
                    log.warn("Chapter {} failed: {}", ch.getChapterTitle(), e.getMessage());
                }
                return null;
            }));
        }

        for (int i = 0; i < required.size(); i++) {
            try {
                String[] result = futures.get(i).get(120, java.util.concurrent.TimeUnit.SECONDS);
                if (result != null) {
                    doc.append(result[0]).append("\n\n").append(result[1]).append("\n\n");
                    generated++;
                }
            } catch (Exception e) {
                log.warn("Chapter {} error: {}", required.get(i).getChapterTitle(), e.getMessage());
            }
        }
        } finally {
            pool.shutdown();
        }
        log.info("Reliability parallel gen: {}/{} chapters", generated, required.size());
        return generated > 0 ? doc.toString() : generateOneShot(projectId, null, promptName);
    }

    private void generateByRelTemplateStream(Long projectId, Long templateId,
                                              String promptName, Consumer<String> onChunk) {
        generateReliabilityDocStream(projectId, null, promptName, onChunk);
    }


    /**
     * One-shot 生成（无模板时回退）。
     */
    private String generateOneShot(Long projectId, Long docLedgerId, String promptName) {
        String context = buildReliabilityContext(projectId, promptName);
        if (context.isEmpty()) return "";

        if (Str.estimateTokens(context) > MAX_CONTEXT_TOKENS) {
            context = Str.truncateByTokens(context, MAX_CONTEXT_TOKENS);
        }

        String userPrompt = promptTemplateService.render(promptName, Map.of("context", context));
        String systemPrompt = promptTemplateService.getTemplate(promptName);
        if (systemPrompt.isEmpty()) {
            systemPrompt = "你是军工可靠性文档撰写专家，请输出符合GJB规范的文档正文。";
        }

        log.info("Reliability one-shot: promptName={}, chars={}", promptName, userPrompt.length());
        String result = llmClient.chat(systemPrompt, userPrompt);
        if (result != null && !result.isEmpty() && result.startsWith("null")) {
            String cleaned = result.replaceFirst("^(null)+", "");
            log.info("Stripped {} leading null chars from reliability AI response", result.length() - cleaned.length());
            return cleaned;
        }
        return result;
    }

    private void generateOneShotStream(Long projectId, Long docLedgerId,
                                        String promptName, Consumer<String> onChunk) {
        String context = buildReliabilityContext(projectId, promptName);
        if (context.isEmpty()) return;

        if (Str.estimateTokens(context) > MAX_CONTEXT_TOKENS) {
            context = Str.truncateByTokens(context, MAX_CONTEXT_TOKENS);
        }

        String userPrompt = promptTemplateService.render(promptName, Map.of("context", context));
        String systemPrompt = promptTemplateService.getTemplate(promptName);
        if (systemPrompt.isEmpty()) {
            systemPrompt = "你是军工可靠性文档撰写专家，请输出符合GJB规范的文档正文。";
        }
        var stripRef = new Object() { boolean stripping = true; };
        llmClient.chatStream(systemPrompt, userPrompt, chunk -> {
            if (stripRef.stripping) {
                String stripped = chunk.replaceAll("^null+", "");
                if (stripped.isEmpty() && chunk.length() <= 200) return;
                if (!stripped.isEmpty()) { stripRef.stripping = false; chunk = stripped; }
            }
            onChunk.accept(chunk);
        });
    }

    /**
     * 组装包含可靠性指标的项目上下文。
     */
    private String buildReliabilityContext(Long projectId, String promptName) {
        StringBuilder ctx = new StringBuilder();
        ctx.append(contextAssemblyService.assembleContext(projectId));

        if (projectId != null) {
            try {
                var reqs = relRequirementMapper.selectList(
                    new LambdaQueryWrapper<RelRequirement>()
                        .eq(RelRequirement::getProjectId, projectId));
                if (reqs != null && !reqs.isEmpty()) {
                    ctx.append("\n\n## 可靠性指标要求\n");
                    for (var req : reqs) {
                        ctx.append("- MTBF: ");
                        ctx.append(req.getMtbfHours() != null ? req.getMtbfHours() + " h" : "待确定");
                        ctx.append("\n");
                        if (req.getReliabilityAtTime() != null) {
                            ctx.append("- 可靠度 R(")
                               .append(req.getReliabilityTimeHours() != null ? req.getReliabilityTimeHours() + "h" : "待确定时间")
                               .append(") = ").append(req.getReliabilityAtTime()).append("\n");
                        }
                        if (req.getVerificationMethod() != null) {
                            ctx.append("- 验证方法: ").append(req.getVerificationMethod()).append("\n");
                        }
                        if (req.getFailureCriteria() != null) {
                            ctx.append("- 故障判据: ").append(req.getFailureCriteria()).append("\n");
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to load requirements for project {}: {}", projectId, e.getMessage());
            }
        }

        return ctx.toString();
    }


    // ==================== 文档台账管理 ====================

    /**
     * 为可靠性文档查找或创建 DocLedger。
     * 去重逻辑：同一项目下同 docCode 只保留一条活跃记录。
     * @return DocLedger (never null)
     */
    public DocLedger findOrCreateLedger(Long projectId, Long stageId, String docCode, String docName) {
        // 1. 查找 stage_doc_checklist_template
        StageDocChecklistTemplate checklistTmpl = checklistTemplateMapper.selectOne(
            new LambdaQueryWrapper<StageDocChecklistTemplate>()
                .eq(StageDocChecklistTemplate::getDocCode, docCode));
        
        if (checklistTmpl == null) {
            log.warn("Checklist template not found for docCode={}, creating ad-hoc ledger", docCode);
            return createAdHocLedger(projectId, stageId, docName);
        }

        // 2. 查找或创建 ProjectDocChecklist（项目-阶段文档清单项）
        LambdaQueryWrapper<ProjectDocChecklist> checklistQuery = new LambdaQueryWrapper<ProjectDocChecklist>()
            .eq(ProjectDocChecklist::getProjectId, projectId)
            .eq(ProjectDocChecklist::getTemplateId, checklistTmpl.getId());
        if (stageId != null) {
            checklistQuery.eq(ProjectDocChecklist::getStageId, stageId);
        }
        checklistQuery.orderByDesc(ProjectDocChecklist::getId).last("LIMIT 1");
        ProjectDocChecklist checklistItem = checklistItemMapper.selectOne(checklistQuery);
        
        if (checklistItem == null) {
            checklistItem = new ProjectDocChecklist();
            checklistItem.setProjectId(projectId);
            checklistItem.setStageId(stageId);
            checklistItem.setTemplateId(checklistTmpl.getId());
            checklistItem.setDocName(checklistTmpl.getDocName());
            checklistItem.setCategory(checklistTmpl.getCategory());
            checklistItem.setCategoryCode(checklistTmpl.getCategoryCode());
            checklistItem.setSortOrder(checklistTmpl.getOrderNum());
            checklistItemMapper.insert(checklistItem);
        }

        // 3. 查找是否已有 DocLedger（去重：同项目+同 checklist_item）
        DocLedger existingLedger = docLedgerMapper.selectOne(
            new LambdaQueryWrapper<DocLedger>()
                .eq(DocLedger::getProjectId, projectId)
                .eq(DocLedger::getChecklistItemId, checklistItem.getId())
                .orderByDesc(DocLedger::getId).last("LIMIT 1"));
        
        if (existingLedger != null) {
            log.info("Reusing existing ledger id={} for docCode={}", existingLedger.getId(), docCode);
            return existingLedger;
        }

        // 4. 创建新的 DocLedger
        return createLedger(projectId, stageId, checklistItem);
    }

    private DocLedger createLedger(Long projectId, Long stageId, ProjectDocChecklist item) {
        DocLedger ledger = new DocLedger();
        ledger.setProjectId(projectId);
        ledger.setStageId(stageId);
        ledger.setChecklistItemId(item.getId());
        ledger.setDocName(item.getDocName());
        ledger.setDocType(item.getCategory());
        ledger.setDocCategory(item.getCategoryCode());
        ledger.setLifecycleStatus("DRAFTING");
        ledger.setRequiredFlag(true); // Reliability docs are always required
        ledger.setCreatedAt(java.time.LocalDateTime.now());
        ledger.setUpdatedAt(java.time.LocalDateTime.now());
        docLedgerMapper.insert(ledger);
        log.info("Created ledger id={} for docName={}", ledger.getId(), item.getDocName());
        return ledger;
    }

    private DocLedger createAdHocLedger(Long projectId, Long stageId, String docName) {
        DocLedger ledger = new DocLedger();
        ledger.setProjectId(projectId);
        ledger.setStageId(stageId);
        ledger.setDocName(docName != null ? docName : "可靠性文档");
        ledger.setDocType("可靠性设计文件");
        ledger.setDocCategory("REL");
        ledger.setLifecycleStatus("DRAFTING");
        ledger.setRequiredFlag(true);
        ledger.setCreatedAt(java.time.LocalDateTime.now());
        ledger.setUpdatedAt(java.time.LocalDateTime.now());
        docLedgerMapper.insert(ledger);
        return ledger;
    }

    /**
     * 保存生成内容到 DocLedger 并解析章节。
     */
        public java.util.Map<String, Object> saveContentToLedger(Long docLedgerId, String content) {
        if (docLedgerId == null || content == null || content.isBlank()) return java.util.Map.of("chapters", 0, "version", "");
        
        DocLedger ledger = docLedgerMapper.selectById(docLedgerId);
        if (ledger == null) return java.util.Map.of("chapters", 0, "version", "");

        // Update ledger
        ledger.setLifecycleStatus("DRAFTING");
        ledger.setContentSize((long) content.length());
        ledger.setUpdatedAt(java.time.LocalDateTime.now());
        
        // 军标版本号: V0.1 初稿, 每次重新生成递增 V0.2, V0.3...
        String version = computeNextVersion(docLedgerId);
        
        // Upload file with version in name
        try {
            String baseName = ledger.getDocName() != null ? ledger.getDocName() : "document";
            String filename = baseName + "_" + version + ".md";
            String fileId = fileStorageService.upload(
                content.getBytes(java.nio.charset.StandardCharsets.UTF_8), filename);
            ledger.setFileObjectId(fileId);
        } catch (Exception e) {
            log.warn("Failed to upload file for ledger {}: {}", docLedgerId, e.getMessage());
        }
        docLedgerMapper.updateById(ledger);

        // Parse markdown into chapters
        int chapterCount = splitChapters(docLedgerId, content);
        log.info("Saved {} to ledger {}: {} chars, {} chapters, version {}",
            ledger.getDocName(), docLedgerId, content.length(), chapterCount, version);
        return java.util.Map.of("chapters", chapterCount, "version", version);
    }

    private String computeNextVersion(Long docLedgerId) {
        try {
            var versions = docVersionMapper.selectList(
                new LambdaQueryWrapper<DocVersion>()
                    .eq(DocVersion::getDocFileId, docLedgerId)
                    .orderByDesc(DocVersion::getId)
                    .last("LIMIT 1"));
            if (versions != null && !versions.isEmpty()) {
                String lastVer = versions.get(0).getVersionNo();
                if (lastVer != null && lastVer.startsWith("V")) {
                    String[] parts = lastVer.substring(1).split("\\.");
                    if (parts.length >= 2) {
                        int minor = Integer.parseInt(parts[1]) + 1;
                        return "V" + parts[0] + "." + minor;
                    }
                }
            }
        } catch (Exception e) { /* ignore */ }
        return "V0.1";  // First draft
    }

    @Deprecated
    private int saveContentToLedgerOld() { return 0; }

    private int splitChapters(Long docLedgerId, String content) {
        try {
            // Mark existing chapters as deleted
            List<DocChapter> oldChapters = docChapterMapper.selectList(
                new LambdaQueryWrapper<DocChapter>()
                    .eq(DocChapter::getDocLedgerId, docLedgerId)
                    .eq(DocChapter::getDeleted, 0));
            for (DocChapter old : oldChapters) {
                old.setDeleted(1);
                docChapterMapper.updateById(old);
            }

            // Parse markdown sections
            List<com.military.doc.ai.util.MarkdownChapterParser.ParsedSection> roots =
                com.military.doc.ai.util.MarkdownChapterParser.parse(content);
            if (roots.isEmpty()) return 0;

            List<com.military.doc.ai.util.MarkdownChapterParser.FlatSection> flat =
                com.military.doc.ai.util.MarkdownChapterParser.flatten(roots);
            
            List<DocChapter> chapters = new java.util.ArrayList<>();
            for (var fs : flat) {
                DocChapter ch = new DocChapter();
                ch.setDocLedgerId(docLedgerId);
                ch.setParentId(0L);
                ch.setChapterNumber(fs.section().number() != null ? fs.section().number() : String.valueOf(fs.orderNum()));
                ch.setChapterTitle(fs.section().title().length() > 250 ? fs.section().title().substring(0, 250) : fs.section().title());
                ch.setChapterLevel(Math.min(fs.section().level(), 5));
                ch.setOrderNum(fs.orderNum());
                ch.setContent(fs.section().content());
                ch.setFillStatus("FILLED");
                ch.setCreatedAt(java.time.LocalDateTime.now());
                ch.setUpdatedAt(java.time.LocalDateTime.now());
                docChapterMapper.insert(ch);
                chapters.add(ch);
            }
            
            // Fix parent IDs
            for (int i = 0; i < flat.size(); i++) {
                int pi = flat.get(i).parentFlatIndex();
                if (pi >= 0 && pi < chapters.size()) {
                    chapters.get(i).setParentId(chapters.get(pi).getId());
                    docChapterMapper.updateById(chapters.get(i));
                }
            }
            
            return chapters.size();
        } catch (Exception e) {
            log.warn("Split chapters failed for ledger {}: {}", docLedgerId, e.getMessage());
            return 0;
        }
    }

}