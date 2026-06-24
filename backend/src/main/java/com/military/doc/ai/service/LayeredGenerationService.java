package com.military.doc.ai.service;

import com.military.doc.ai.context.ContextAssemblyService;
import com.military.doc.ai.llm.LlmClient;
import com.military.doc.ai.prompt.PromptTemplateService;
import com.military.doc.ai.util.AiMeta;
import com.military.doc.ai.util.AiMetaParser;
import com.military.doc.common.util.Str;
import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.mapper.DocChapterMapper;
import com.military.doc.modules.document.service.DocChapterService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Layered 3-step document generation orchestrator.
 * Step 1: Generate outline (chapter titles + summaries) → user confirms
 * Step 2: Per-chapter content generation with AI_META uncertainty markers
 * Step 3: Full assembly + auto-review + deficiency checklist
 *
 * Implements Section 4.3 of requirements-v2.0.md.
 */
@Slf4j
@Service
public class LayeredGenerationService {

    private final ContextAssemblyService contextAssemblyService;
    private final PromptTemplateService promptTemplateService;
    private final LlmClient llmClient;
    private final DocChapterMapper chapterMapper;
    private final DocChapterService chapterService;
    private final DraftGenerationService draftGenerationService;
    private final AutoReviewService autoReviewService;

    public LayeredGenerationService(ContextAssemblyService contextAssemblyService,
                                     PromptTemplateService promptTemplateService,
                                     LlmClient llmClient,
                                     DocChapterMapper chapterMapper,
                                     DocChapterService chapterService,
                                     DraftGenerationService draftGenerationService,
                                     AutoReviewService autoReviewService) {
        this.contextAssemblyService = contextAssemblyService;
        this.promptTemplateService = promptTemplateService;
        this.llmClient = llmClient;
        this.chapterMapper = chapterMapper;
        this.chapterService = chapterService;
        this.draftGenerationService = draftGenerationService;
        this.autoReviewService = autoReviewService;
    }

    // ==================== API types ====================

    public enum Step { OUTLINE, CHAPTERS, ASSEMBLE }
    public enum StepStatus { PENDING, IN_PROGRESS, DONE, CONFIRMED }

    public record OutlineChapter(String chapterNumber, String chapterTitle, int chapterLevel,
                                  String summary, boolean isRequired, List<String> keyPoints) {}
    public record OutlineResult(List<OutlineChapter> chapters, int totalChapters, boolean readyForNext) {}
    public record ChapterResult(String chapterNumber, String chapterTitle, String content,
                                 AiMeta meta, boolean completed) {}
    public record AssembleResult(String fullDocument, AutoReviewService.ReviewReport review,
                                  List<String> deficiencyItems, int totalScore) {}
    public record GenerationState(Step currentStep, StepStatus outlineStatus,
                                   StepStatus chaptersStatus, StepStatus assembleStatus,
                                   int outlineChapterCount, int chaptersGenerated, int totalChapters) {}

    // ==================== Step 1: Generate Outline ====================

    public OutlineResult generateOutline(Long docLedgerId, Long projectId) {
        List<DocChapter> chapters = loadChapters(docLedgerId);
        if (chapters.isEmpty()) {
            return new OutlineResult(List.of(), 0, false);
        }

        String projectContext = contextAssemblyService.assembleContext(projectId);
        StringBuilder prompt = new StringBuilder();
        prompt.append("## 任务\n为以下文档的每个章节生成一句话摘要（不超过50字）和2-3个关键要点。\n\n");
        prompt.append(projectContext).append("\n\n");
        prompt.append("## 章节列表\n");
        for (DocChapter ch : chapters) {
            prompt.append("- ").append(ch.getChapterNumber()).append(" ").append(ch.getChapterTitle()).append("\n");
        }
        prompt.append("\n## 输出格式\n严格按照以下JSON格式输出：\n");
        prompt.append("[{\"chapterNumber\":\"1\",\"summary\":\"本章规定了...\",\"keyPoints\":[\"要点1\",\"要点2\"]}]\n");

        String systemPrompt = buildOutlineSystemPrompt();
        String response = llmClient.chat(systemPrompt, prompt.toString());

        List<OutlineChapter> outlines = parseOutlineResponse(response, chapters);
        return new OutlineResult(outlines, outlines.size(), !outlines.isEmpty());
    }

    public void generateOutlineStream(Long docLedgerId, Long projectId,
                                       Consumer<String> onChunk, Consumer<OutlineResult> onDone) {
        OutlineResult result = generateOutline(docLedgerId, projectId);
        if (result.chapters.isEmpty()) {
            onChunk.accept("（章节列表为空，请先初始化章节结构）");
        } else {
            StringBuilder sb = new StringBuilder("## 文档大纲\n\n");
            for (OutlineChapter oc : result.chapters) {
                sb.append("### ").append(oc.chapterNumber).append(" ").append(oc.chapterTitle)
                  .append(oc.isRequired ? "（必填）" : "").append("\n");
                sb.append("> ").append(oc.summary).append("\n\n");
                if (!oc.keyPoints.isEmpty()) {
                    for (String kp : oc.keyPoints) {
                        sb.append("- ").append(kp).append("\n");
                    }
                    sb.append("\n");
                }
            }
            onChunk.accept(sb.toString());
        }
        onDone.accept(result);
    }

    // ==================== Step 2: Generate Chapters ====================

    public void generateChaptersStream(Long docLedgerId, Long projectId,
                                        Consumer<String> onChunk,
                                        Consumer<ChapterResult> onChapterDone,
                                        Consumer<Integer> onProgress) {
        List<DocChapter> chapters = loadChapters(docLedgerId);
        if (chapters.isEmpty()) {
            onChunk.accept("（无章节可生成）");
            return;
        }

        int total = chapters.size();
        int threads = Math.min(6, total);
        var pool = Executors.newFixedThreadPool(threads);
        var futures = new ArrayList<Future<ChapterResult>>();

        for (DocChapter ch : chapters) {
            futures.add(pool.submit(() -> generateSingleChapter(ch, projectId)));
        }

        int completed = 0;
        for (int i = 0; i < total; i++) {
            try {
                ChapterResult cr = futures.get(i).get(180, TimeUnit.SECONDS);
                if (cr != null && cr.completed) {
                    // Save to DB
                    DocChapter ch = chapterMapper.selectById(
                        chapters.stream().filter(c -> c.getChapterNumber().equals(cr.chapterNumber))
                            .findFirst().map(DocChapter::getId).orElse(null));
                    if (ch != null) {
                        ch.setContent(truncate(cr.content, 50000));
                        ch.setFillStatus("FILLED");
                        ch.setFillPercentage(100);
                        chapterMapper.updateById(ch);
                    }
                    // Stream heading + content
                    String heading = "## " + cr.chapterNumber + " " + cr.chapterTitle + "\n\n";
                    onChunk.accept(heading);
                    onChunk.accept(cr.content + "\n\n");
                    if (cr.meta != null) {
                        onChunk.accept(AiMetaParser.buildMetaBlock(cr.meta) + "\n\n");
                    }
                    onChapterDone.accept(cr);
                    completed++;
                }
            } catch (Exception e) {
                log.warn("Chapter {} failed: {}", chapters.get(i).getChapterTitle(), e.getMessage());
                onChunk.accept("（章节「" + chapters.get(i).getChapterTitle() + "」生成超时）\n\n");
            }
            onProgress.accept(completed);
        }
        pool.shutdown();
        log.info("Layered generation chapter step: {}/{} completed", completed, total);
    }

    // ==================== Step 3: Assemble + Auto-Review ====================

    public AssembleResult assembleAndReview(Long docLedgerId, Long projectId) {
        List<DocChapter> chapters = loadChapters(docLedgerId);
        // Build full document from chapters
        StringBuilder doc = new StringBuilder();
        for (DocChapter ch : chapters) {
            int level = Math.min(ch.getChapterLevel() != null ? ch.getChapterLevel() : 1, 5);
            doc.append("#".repeat(level + 1)).append(" ")
               .append(ch.getChapterNumber()).append(" ").append(ch.getChapterTitle()).append("\n\n");
            if (ch.getContent() != null && !ch.getContent().isBlank()) {
                doc.append(ch.getContent()).append("\n\n");
            } else {
                doc.append("（本章内容待补充）\n\n");
            }
        }

        // Run auto-review
        AutoReviewService.ReviewReport review = autoReviewService.review(docLedgerId);

        // Build deficiency checklist
        List<String> deficiencies = new ArrayList<>();
        if (review != null) {
            if (review.getForbiddenWordIssues() != null) {
                for (var issue : review.getForbiddenWordIssues()) deficiencies.add(issue.getDescription());
            }
            if (review.getToVerifyItems() != null) {
                for (var issue : review.getToVerifyItems()) deficiencies.add(issue.getDescription());
            }
            if (review.getMissingItems() != null) {
                for (var issue : review.getMissingItems()) deficiencies.add(issue.getDescription());
            }
        }
        // Check for empty chapters
        for (DocChapter ch : chapters) {
            if (ch.getContent() == null || ch.getContent().isBlank()) {
                deficiencies.add("缺失内容：" + ch.getChapterNumber() + " " + ch.getChapterTitle());
            }
        }

        // Calculate score
        int totalScore = review != null && review.getQualityScore() != null ?
            review.getQualityScore().getTotalScore() : 0;

        return new AssembleResult(doc.toString(), review, deficiencies, totalScore);
    }

    // ==================== State Management ====================

    public GenerationState getState(Long docLedgerId) {
        List<DocChapter> chapters = loadChapters(docLedgerId);
        long filledCount = chapters.stream()
            .filter(c -> c.getContent() != null && !c.getContent().isBlank()).count();
        boolean hasOutline = chapters.stream()
            .anyMatch(c -> c.getContentJson() != null && c.getContentJson().contains("summary"));

        return new GenerationState(
            filledCount == 0 ? Step.OUTLINE :
            filledCount < chapters.size() ? Step.CHAPTERS : Step.ASSEMBLE,
            hasOutline ? StepStatus.DONE : StepStatus.PENDING,
            filledCount > 0 ? (filledCount >= chapters.size() ? StepStatus.DONE : StepStatus.IN_PROGRESS) : StepStatus.PENDING,
            StepStatus.PENDING,
            chapters.size(),
            (int) filledCount,
            chapters.size()
        );
    }

    // ==================== Internal ====================

    private ChapterResult generateSingleChapter(DocChapter ch, Long projectId) {
        String content = draftGenerationService.generateSingleChapterContent(
            contextAssemblyService.assembleContext(projectId), ch);
        if (content == null || content.isBlank()) {
            return new ChapterResult(ch.getChapterNumber(), ch.getChapterTitle(), "", null, false);
        }
        AiMeta meta = AiMetaParser.extract(content);
        String clean = AiMetaParser.stripMeta(content);
        return new ChapterResult(ch.getChapterNumber(), ch.getChapterTitle(), clean, meta, true);
    }

    private List<DocChapter> loadChapters(Long docLedgerId) {
        if (docLedgerId == null) return List.of();
        return chapterMapper.selectList(
            new LambdaQueryWrapper<DocChapter>()
                .eq(DocChapter::getDocLedgerId, docLedgerId)
                .eq(DocChapter::getDeleted, 0)
                .orderByAsc(DocChapter::getOrderNum));
    }

    private List<OutlineChapter> parseOutlineResponse(String response, List<DocChapter> chapters) {
        try {
            String json = response;
            int start = json.indexOf('[');
            int end = json.lastIndexOf(']');
            if (start >= 0 && end > start) json = json.substring(start, end + 1);

            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> raw = mapper.readValue(json, List.class);
            List<OutlineChapter> result = new ArrayList<>();
            Map<String, DocChapter> byNum = chapters.stream()
                .collect(Collectors.toMap(c -> c.getChapterNumber(), c -> c, (a, b) -> a));

            for (Map<String, Object> item : raw) {
                String num = (String) item.get("chapterNumber");
                DocChapter ch = byNum.get(num);
                if (ch == null) continue;
                String summary = (String) item.getOrDefault("summary", "");
                @SuppressWarnings("unchecked")
                List<String> keyPoints = (List<String>) item.getOrDefault("keyPoints", List.of());
                result.add(new OutlineChapter(num, ch.getChapterTitle(),
                    ch.getChapterLevel() != null ? ch.getChapterLevel() : 1,
                    summary, true, keyPoints));
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to parse outline response: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildOutlineSystemPrompt() {
        String tpl = promptTemplateService.getTemplate("system-chapter-writing");
        if (tpl != null && !tpl.isEmpty()) return tpl;
        return "你是一位军工文档策划专家。请为文档章节生成摘要和关键要点。只输出JSON格式。";
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
