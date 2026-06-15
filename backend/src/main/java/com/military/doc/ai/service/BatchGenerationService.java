package com.military.doc.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.military.doc.ai.util.MarkdownChapterParser;
import com.military.doc.ai.util.MarkdownChapterParser.FlatSection;
import com.military.doc.common.storage.FileStorageService;
import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.entity.DocInputReference;
import com.military.doc.modules.document.entity.ProjectDocChecklist;
import com.military.doc.modules.document.entity.StageDocChecklistTemplate;
import com.military.doc.modules.document.mapper.DocChapterMapper;
import com.military.doc.modules.document.mapper.DocInputReferenceMapper;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import com.military.doc.modules.document.mapper.ProjectDocChecklistMapper;
import com.military.doc.modules.document.mapper.StageDocChecklistTemplateMapper;
import com.military.doc.modules.project.entity.Project;
import com.military.doc.modules.project.mapper.ProjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 批量文档生成服务。
 * 给定项目+阶段，按上游依赖拓扑排序后依次生成文档初稿。
 */
@Slf4j
@Service
public class BatchGenerationService {

    private final ProjectDocChecklistMapper checklistMapper;
    private final StageDocChecklistTemplateMapper checklistTemplateMapper;
    private final DocInputReferenceMapper inputRefMapper;
    private final DocLedgerMapper docLedgerMapper;
    private final DocChapterMapper docChapterMapper;
    private final ProjectMapper projectMapper;
    private final DraftGenerationService draftGenerationService;
    private final FileStorageService fileStorageService;
    private final com.military.doc.ai.context.ContextAssemblyService contextAssemblyService;
    private final com.military.doc.modules.template.mapper.DocTemplateChapterMapper templateChapterMapper;

    public BatchGenerationService(ProjectDocChecklistMapper checklistMapper,
                                  StageDocChecklistTemplateMapper checklistTemplateMapper,
                                  DocInputReferenceMapper inputRefMapper,
                                  DocLedgerMapper docLedgerMapper,
                                  DocChapterMapper docChapterMapper,
                                  ProjectMapper projectMapper,
                                  DraftGenerationService draftGenerationService,
                                  FileStorageService fileStorageService,
                                  com.military.doc.ai.context.ContextAssemblyService contextAssemblyService,
                                  com.military.doc.modules.template.mapper.DocTemplateChapterMapper templateChapterMapper) {
        this.checklistMapper = checklistMapper;
        this.checklistTemplateMapper = checklistTemplateMapper;
        this.inputRefMapper = inputRefMapper;
        this.docLedgerMapper = docLedgerMapper;
        this.docChapterMapper = docChapterMapper;
        this.projectMapper = projectMapper;
        this.draftGenerationService = draftGenerationService;
        this.fileStorageService = fileStorageService;
        this.contextAssemblyService = contextAssemblyService;
        this.templateChapterMapper = templateChapterMapper;
    }


    public enum Phase {
        ANALYZING("分析阶段信息"),
        BUILDING_CONTEXT("构建项目上下文"),
        TOPOLOGICAL_SORTING("拓扑排序依赖"),
        GENERATING("生成文档"),
        POST_PROCESSING("后处理");

        private final String label;
        Phase(String label) { this.label = label; }
        public String label() { return label; }
    }

    private record IndexedItem(int index, ProjectDocChecklist item, String code) {}

    public record BatchProgressEvent(
        String type,        // "phase_start" | "phase_end" | "doc_start" | "doc_done" | "doc_error" | "batch_complete" | "batch_cancelled"
        int current,        // current document index (1-based)
        int total,          // total document count
        String docName,     // current document name
        String docCode,     // current document code
        Long docLedgerId,   // ledger id of generated doc
        String status,      // "generating" | "done" | "error"
        String message,     // error message or summary
        String phase,       // current phase name (for phase_start/phase_end)
        int estimatedTotalSeconds  // ETA in seconds
    ) {
        /** Factory for doc events */
        public static BatchProgressEvent docEvent(String type, int current, int total,
                                                   String docName, String docCode,
                                                   Long docLedgerId, String status, String message) {
            return new BatchProgressEvent(type, current, total, docName, docCode, docLedgerId, status, message, null, 0);
        }
        /** Factory for phase events */
        public static BatchProgressEvent phaseEvent(String type, Phase phase, int current, int total, int eta) {
            return new BatchProgressEvent(type, current, total, null, null, null, null, null, phase.name(), eta);
        }
    }

    // 活跃的取消标志，由控制器管理
    private final Map<String, java.util.concurrent.atomic.AtomicBoolean> cancelFlags = new java.util.concurrent.ConcurrentHashMap<>();

    /** 取消指定会话的批量生成 */
    public void cancelBatch(String sessionId) {
        var flag = cancelFlags.get(sessionId);
        if (flag != null) {
            flag.set(true);
            log.info("Batch generation cancelled: session={}", sessionId);
        }
    }

    /** 清理取消标志 */
    public void cleanup(String sessionId) {
        cancelFlags.remove(sessionId);
    }

    /**
     * Generate all documents for a project stage, respecting upstream dependencies.
     * Documents are topologically sorted so that upstream docs are generated first.
     *
     * @param projectId  project ID
     * @param stageId    stage ID
     * @param sessionId  unique session ID for cancel tracking
     * @param onEvent    callback for each progress event (for SSE streaming)
     */
    public void generateStageDocs(Long projectId, Long stageId, String sessionId,
                                   Consumer<BatchProgressEvent> onEvent) {
        var cancelFlag = new java.util.concurrent.atomic.AtomicBoolean(false);
        cancelFlags.put(sessionId, cancelFlag);
        long startTime = System.currentTimeMillis();

        try {
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                log.warn("Project not found: {}", projectId);
                return;
            }

            // Phase 1: ANALYZING
            emitPhase(onEvent, Phase.ANALYZING, 0, 1, 0);

            // 1. Get all checklist items for this stage
        List<ProjectDocChecklist> checklistItems = checklistMapper.selectList(
            new LambdaQueryWrapper<ProjectDocChecklist>()
                .eq(ProjectDocChecklist::getProjectId, projectId)
                .eq(ProjectDocChecklist::getStageId, stageId)
                .orderByAsc(ProjectDocChecklist::getSortOrder));

        if (checklistItems.isEmpty()) {
            log.warn("No checklist items found for project={} stage={}", projectId, stageId);
            emitPhase(onEvent, Phase.ANALYZING, 1, 1, 0);
            onEvent.accept(BatchProgressEvent.docEvent("batch_complete", 0, 0, null, null, null, "done", "无文档需生成"));
            return;
        }
        int total = checklistItems.size();
        emitPhase(onEvent, Phase.BUILDING_CONTEXT, 0, total, estimateEta(startTime, 0, total));

        // 2. Build doc_code → checklist_template_id mapping
        Map<Long, String> templateDocCodes = new HashMap<>(); // templateId → docCode
        for (ProjectDocChecklist item : checklistItems) {
            if (item.getTemplateId() != null) {
                StageDocChecklistTemplate tmpl = checklistTemplateMapper.selectById(item.getTemplateId());
                if (tmpl != null && tmpl.getDocCode() != null) {
                    templateDocCodes.put(item.getTemplateId(), tmpl.getDocCode());
                }
            }
        }

        // 3. Get all UPSTREAM_DOC references for the templates in this stage
        List<Long> templateIds = checklistItems.stream()
            .map(ProjectDocChecklist::getTemplateId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        Map<String, List<String>> upstreamMap = new HashMap<>(); // docCode → [upstream docCodes]
        for (Long tmplId : templateIds) {
            List<DocInputReference> refs = inputRefMapper.selectList(
                new LambdaQueryWrapper<DocInputReference>()
                    .eq(DocInputReference::getChecklistTemplateId, tmplId)
                    .eq(DocInputReference::getRefType, "UPSTREAM_DOC"));
            if (!refs.isEmpty()) {
                String docCode = templateDocCodes.get(tmplId);
                if (docCode != null) {
                    List<String> upstream = refs.stream()
                        .map(DocInputReference::getRefCode)
                        .filter(Objects::nonNull)
                        .toList();
                    upstreamMap.put(docCode, upstream);
                }
            }
        }

        // 4. Topological sort the checklist items
        emitPhase(onEvent, Phase.TOPOLOGICAL_SORTING, 0, total, estimateEta(startTime, 0, total));
        List<ProjectDocChecklist> sorted = topologicalSort(checklistItems, templateDocCodes, upstreamMap);
        log.info("Batch generation: {} docs, sorted order: {}",
            sorted.size(), sorted.stream().map(c -> {
                Long tid = c.getTemplateId();
                return tid != null ? templateDocCodes.getOrDefault(tid, "?") : "custom";
            }).toList());

        int[] completed = {0};
        ExecutorService pool = Executors.newFixedThreadPool(
            Math.min(Runtime.getRuntime().availableProcessors(), 4));

        // Group by dependency depth
        Map<Integer, List<IndexedItem>> byDepth = new LinkedHashMap<>();
        Map<String, Integer> docDepth = new HashMap<>();
        for (int i = 0; i < sorted.size(); i++) {
            ProjectDocChecklist item = sorted.get(i);
            String code = item.getTemplateId() != null ? templateDocCodes.get(item.getTemplateId()) : null;
            String key = code != null ? code : item.getDocName();
            int depth = 0;
            List<String> upstream = upstreamMap.getOrDefault(code, List.of());
            for (String u : upstream) {
                Integer ud = docDepth.get(u);
                if (ud != null) depth = Math.max(depth, ud + 1);
            }
            docDepth.put(key, depth);
            byDepth.computeIfAbsent(depth, k -> new ArrayList<>())
                .add(new IndexedItem(i, item, code));
        }

        for (Map.Entry<Integer, List<IndexedItem>> entry : byDepth.entrySet()) {
            // 检查取消
            if (cancelFlag.get()) {
                emitCancelled(onEvent, completed[0], total);
                return;
            }
            List<IndexedItem> level = entry.getValue();
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (IndexedItem ii : level) {
                // 每个文档前检查取消
                if (cancelFlag.get()) {
                    emitCancelled(onEvent, completed[0], total);
                    return;
                }
                futures.add(CompletableFuture.runAsync(() -> {
                    DocLedger ledger = findOrCreateLedger(projectId, stageId, ii.item);
                    int idx = ii.index;
                    onEvent.accept(BatchProgressEvent.docEvent("doc_start", idx + 1, total,
                        ii.item.getDocName(), ii.code, ledger.getId(), "generating", null));
                    try {
                        // Chapter-by-chapter generation: find template chapters, generate each individually
                        String projectCtx = contextAssemblyService.assembleContext(projectId);
                        String content = generateChaptersFromTemplate(projectId, projectCtx, ledger, ii.item);
                        if (content != null && !content.isBlank()) {
                            String filename = (ii.item.getDocName() != null ? ii.item.getDocName() : "draft") + ".md";
                            String fileObjectId = fileStorageService.upload(
                                content.getBytes(StandardCharsets.UTF_8), filename);
                            ledger.setFileObjectId(fileObjectId);
                            ledger.setContentSize((long) content.length());
                            ledger.setLifecycleStatus("DRAFTING");
                            ledger.setUpdatedAt(LocalDateTime.now());
                            docLedgerMapper.updateById(ledger);
                            int chapterCount = splitContentIntoChapters(ledger.getId(), content, ii.item);
                            synchronized (completed) { completed[0]++; }
                            onEvent.accept(BatchProgressEvent.docEvent("doc_done", idx + 1, total,
                                ii.item.getDocName(), ii.code, ledger.getId(), "done",
                                "生成了 " + content.length() + " 字符, " + chapterCount + " 章节"));
                        } else {
                            synchronized (completed) { completed[0]++; }
                            onEvent.accept(BatchProgressEvent.docEvent("doc_error", idx + 1, total,
                                ii.item.getDocName(), ii.code, ledger.getId(), "error",
                                "生成为空，请检查项目主数据和输入文件"));
                        }
                    } catch (Exception e) {
                        synchronized (completed) { completed[0]++; }
                        log.error("Failed to generate doc {}: {}", ii.item.getDocName(), e.getMessage());
                        onEvent.accept(BatchProgressEvent.docEvent("doc_error", idx + 1, total,
                            ii.item.getDocName(), ii.code, ledger != null ? ledger.getId() : null,
                            "error", e.getMessage()));
                    }
                }, pool));
            }

            // Wait for all docs at this depth before proceeding to next depth
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
        pool.shutdown();
        try { pool.awaitTermination(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}

        // 6. POST_PROCESSING phase + completion
        emitPhase(onEvent, Phase.POST_PROCESSING, total, total, 0);
        onEvent.accept(BatchProgressEvent.docEvent(
            "batch_complete", total, total, null, null, null, "done",
            "批量生成完成，共 " + completed[0] + " 份文档"));
        } finally {
            cancelFlags.remove(sessionId);
        }
    }

    private void emitPhase(Consumer<BatchProgressEvent> onEvent, Phase phase, int current, int total, int eta) {
        onEvent.accept(BatchProgressEvent.phaseEvent("phase_start", phase, current, total, eta));
    }

    private void emitCancelled(Consumer<BatchProgressEvent> onEvent, int completed, int total) {
        onEvent.accept(BatchProgressEvent.docEvent("batch_cancelled", completed, total, null, null, null,
            "cancelled", "批量生成已被取消，已完成 " + completed + " 份文档"));
        log.info("Batch generation cancelled: {}/{} docs completed", completed, total);
    }

    private int estimateEta(long startTime, int completed, int total) {
        if (completed == 0) return 0;
        long elapsed = System.currentTimeMillis() - startTime;
        double perDoc = (double) elapsed / completed;
        long remaining = (long) (perDoc * (total - completed));
        return (int) (remaining / 1000);
    }

    /**
     * Topological sort: documents with no upstream dependencies come first.
     */
    private List<ProjectDocChecklist> topologicalSort(
            List<ProjectDocChecklist> items,
            Map<Long, String> templateDocCodes,
            Map<String, List<String>> upstreamMap) {

        // Build item index by docCode
        Map<String, ProjectDocChecklist> byDocCode = new HashMap<>();
        for (ProjectDocChecklist item : items) {
            if (item.getTemplateId() != null) {
                String code = templateDocCodes.get(item.getTemplateId());
                if (code != null) {
                    byDocCode.put(code, item);
                }
            }
        }

        // Build indegree map
        Map<String, Integer> indegree = new LinkedHashMap<>();
        Map<String, List<String>> adjacency = new LinkedHashMap<>();

        for (ProjectDocChecklist item : items) {
            String code = item.getTemplateId() != null ? templateDocCodes.get(item.getTemplateId()) : null;
            String key = code != null ? code : item.getDocName();
            indegree.putIfAbsent(key, 0);
            adjacency.putIfAbsent(key, new ArrayList<>());
        }

        // For each doc, its upstream docs must be generated first
        for (Map.Entry<String, List<String>> entry : upstreamMap.entrySet()) {
            String docCode = entry.getKey();
            for (String upstream : entry.getValue()) {
                // Only add edge if upstream is in this stage
                if (indegree.containsKey(upstream)) {
                    adjacency.computeIfAbsent(upstream, k -> new ArrayList<>()).add(docCode);
                    indegree.merge(docCode, 1, Integer::sum);
                }
            }
        }

        // Kahn's algorithm
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> e : indegree.entrySet()) {
            if (e.getValue() == 0) {
                queue.add(e.getKey());
            }
        }

        List<ProjectDocChecklist> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            String key = queue.poll();
            ProjectDocChecklist item = byDocCode.get(key);
            if (item != null) {
                sorted.add(item);
            } else {
                // Custom item — find by name
                for (ProjectDocChecklist i : items) {
                    if (key.equals(i.getDocName()) && !sorted.contains(i)) {
                        sorted.add(i);
                        break;
                    }
                }
            }

            for (String neighbor : adjacency.getOrDefault(key, List.of())) {
                indegree.merge(neighbor, -1, (old, diff) -> old + diff);
                if (indegree.get(neighbor) == 0) {
                    queue.add(neighbor);
                }
            }
        }

        // Append any remaining items (no dependencies)
        for (ProjectDocChecklist item : items) {
            if (!sorted.contains(item)) {
                sorted.add(item);
            }
        }

        return sorted;
    }

    /**
     * Generate document content chapter-by-chapter using the document template.
     * Each template chapter gets a focused AI call with its description, writing tips,
     * standard references, and project context.
     */
    private String generateChaptersFromTemplate(Long projectId, String projectCtx,
                                                  DocLedger ledger, ProjectDocChecklist item) {
        // Find template via checklist item
        Long templateId = null;
        if (item.getTemplateId() != null) {
            var sct = checklistTemplateMapper.selectById(item.getTemplateId());
            if (sct != null) templateId = sct.getTemplateId();
        }
        if (templateId == null) {
            return draftGenerationService.generate(projectId, null, ledger.getId());
        }

        var chapters = templateChapterMapper.selectList(
            new LambdaQueryWrapper<com.military.doc.modules.template.entity.DocTemplateChapter>()
                .eq(com.military.doc.modules.template.entity.DocTemplateChapter::getTemplateId, templateId)
                .orderByAsc(com.military.doc.modules.template.entity.DocTemplateChapter::getOrderNum));

        if (chapters == null || chapters.isEmpty()) {
            return draftGenerationService.generate(projectId, null, ledger.getId());
        }

        StringBuilder doc = new StringBuilder();
        doc.append("# ").append(item.getDocName() != null ? item.getDocName() : "文档").append("\n\n");

        int generated = 0;
        for (var tplCh : chapters) {
            if (!Boolean.TRUE.equals(tplCh.getIsRequired())) continue;
            try {
                String body = draftGenerationService.generateChapterByTemplate(projectId, tplCh, projectCtx);
                if (body != null && !body.isBlank()) {
                    int level = tplCh.getChapterLevel() != null ? tplCh.getChapterLevel() : 1;
                    doc.append("#".repeat(Math.min(level, 5))).append(" ")
                       .append(tplCh.getChapterNumber()).append(" ")
                       .append(tplCh.getChapterTitle()).append("\n\n");
                    doc.append(body).append("\n\n");
                    generated++;
                }
            } catch (Exception e) {
                log.warn("Chapter {} failed: {}", tplCh.getChapterTitle(), e.getMessage());
            }
        }

        log.info("Per-chapter generation: {}/{} chapters for doc {}",
            generated, chapters.size(), item.getDocName());
        return generated > 0 ? doc.toString()
            : draftGenerationService.generate(projectId, null, ledger.getId());
    }

    private DocLedger findOrCreateLedger(Long projectId, Long stageId, ProjectDocChecklist item) {
        List<DocLedger> existing = docLedgerMapper.selectList(
            new LambdaQueryWrapper<DocLedger>()
                .eq(DocLedger::getProjectId, projectId)
                .eq(DocLedger::getStageId, stageId)
                .eq(DocLedger::getDocName, item.getDocName())
                .orderByDesc(DocLedger::getId)
                .last("LIMIT 1"));
        if (!existing.isEmpty()) return existing.get(0);

        DocLedger ledger = new DocLedger();
        ledger.setProjectId(projectId); ledger.setStageId(stageId);
        ledger.setDocName(item.getDocName()); ledger.setDocType(item.getCategory());
        ledger.setDocCategory(item.getCategoryCode()); ledger.setLifecycleStatus("DRAFTING");
        ledger.setRequiredFlag(true); ledger.setChecklistItemId(item.getId());
        ledger.setCreatedAt(LocalDateTime.now()); ledger.setUpdatedAt(LocalDateTime.now());
        docLedgerMapper.insert(ledger);
        return ledger;
    }

    private int splitContentIntoChapters(Long docLedgerId, String content, ProjectDocChecklist item) {
        try {
            List<DocChapter> oldChapters = docChapterMapper.selectList(
                new LambdaQueryWrapper<DocChapter>()
                    .eq(DocChapter::getDocLedgerId, docLedgerId).eq(DocChapter::getDeleted, 0));
            for (DocChapter old : oldChapters) { old.setDeleted(1); docChapterMapper.updateById(old); }

            List<MarkdownChapterParser.ParsedSection> roots = MarkdownChapterParser.parse(content);
            if (roots.isEmpty()) return 0;

            List<FlatSection> flatSections = MarkdownChapterParser.flatten(roots);
            List<DocChapter> chapters = new ArrayList<>();
            for (FlatSection fs : flatSections) {
                DocChapter ch = new DocChapter();
                ch.setDocLedgerId(docLedgerId); ch.setParentId(0L);
                ch.setChapterNumber(fs.section().number() != null ? fs.section().number() : String.valueOf(fs.orderNum()));
                ch.setChapterTitle(fs.section().title().length() > 250 ? fs.section().title().substring(0, 250) : fs.section().title());
                ch.setChapterLevel(Math.min(fs.section().level(), 5)); ch.setOrderNum(fs.orderNum());
                ch.setContent(fs.section().content()); ch.setFillStatus("FILLED");
                ch.setCreatedAt(LocalDateTime.now()); ch.setUpdatedAt(LocalDateTime.now());
                docChapterMapper.insert(ch); chapters.add(ch);
            }
            for (int i = 0; i < flatSections.size(); i++) {
                int pi = flatSections.get(i).parentFlatIndex();
                if (pi >= 0 && pi < chapters.size()) {
                    chapters.get(i).setParentId(chapters.get(pi).getId());
                    docChapterMapper.updateById(chapters.get(i));
                }
            }
            log.info("Split {} chapters for ledger {}", chapters.size(), docLedgerId);
            return chapters.size();
        } catch (Exception e) { log.warn("Split chapters failed: {}", e.getMessage()); return 0; }
    }


}
