package com.military.doc.modules.document.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.common.storage.FileStorageService;
import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.mapper.DocChapterMapper;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import com.military.doc.modules.document.util.GjbStyleHelper;
import com.military.doc.modules.project.entity.ProjectMasterData;
import com.military.doc.modules.project.mapper.ProjectMasterDataMapper;
import com.military.doc.modules.template.entity.DocTemplateChapter;
import com.military.doc.modules.template.entity.DocTemplateV2;
import com.military.doc.modules.template.mapper.DocTemplateChapterMapper;
import com.military.doc.modules.template.mapper.DocTemplateV2Mapper;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.*;

@Service
public class DocxGenerationService {

    @Autowired private DocLedgerMapper docLedgerMapper;
    @Autowired private DocChapterMapper docChapterMapper;
    @Autowired private DocTemplateV2Mapper templateV2Mapper;
    @Autowired private DocTemplateChapterMapper templateChapterMapper;
    @Autowired private ProjectMasterDataMapper masterDataMapper;
    @Autowired private com.military.doc.modules.document.mapper.ProjectDocChecklistMapper checklistMapper;
    @Autowired private com.military.doc.modules.document.mapper.StageDocChecklistTemplateMapper stageChecklistTplMapper;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private ObjectMapper objectMapper;

    public byte[] generate(Long docLedgerId, boolean includeCover, boolean showHighlights) {
        DocLedger ledger = docLedgerMapper.selectById(docLedgerId);
        if (ledger == null) throw new RuntimeException("文档台账条目不存在: " + docLedgerId);

        // === Find template via checklist chain ===
        DocTemplateV2 template = findTemplate(ledger);
        List<DocTemplateChapter> templateChapters = template != null
            ? templateChapterMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocTemplateChapter>()
                .eq(DocTemplateChapter::getTemplateId, template.getId())
                .orderByAsc(DocTemplateChapter::getOrderNum))
            : List.of();

        // === Load AI-generated content ===
        Map<String, String> contentByTitle = new java.util.LinkedHashMap<>();
        List<DocChapter> aiChapters = docChapterMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocChapter>()
                .eq(DocChapter::getDocLedgerId, docLedgerId)
                .eq(DocChapter::getDeleted, 0)
                .orderByAsc(DocChapter::getOrderNum));
        for (DocChapter ac : aiChapters) {
            if (ac.getChapterTitle() != null && ac.getContent() != null && !ac.getContent().isBlank()) {
                contentByTitle.put(ac.getChapterTitle(), ac.getContent());
            }
        }

        // === Project info for cover ===
        ProjectMasterData pmd = null;
        if (ledger.getProjectId() != null) {
            pmd = masterDataMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProjectMasterData>()
                    .eq(ProjectMasterData::getProjectId, ledger.getProjectId()));
        }

        try (XWPFDocument doc = new XWPFDocument()) {
            if (includeCover) {
                String docTitle = template != null ? template.getTemplateName() : ledger.getDocName();
                GjbStyleHelper.addCoverPage(doc, docTitle, ledger.getDocName(),
                    ledger.getSecurityLevel(), null);
            }

            // Body section: GJB page layout
            GjbStyleHelper.setupBodySection(doc);

            // If AI chapters exist with content, render them directly (avoids duplicate headings)
            boolean hasAiContent = aiChapters.stream().anyMatch(c -> c.getContent() != null && !c.getContent().isBlank());
            if (!hasAiContent && !templateChapters.isEmpty()) {
                writeTemplateChapters(doc, templateChapters, 0, contentByTitle,
                    new int[]{0}, showHighlights);
            } else {
                // Fallback: render AI chapters directly (legacy mode)
                Map<Long, List<DocChapter>> childrenMap = new HashMap<>();
                for (DocChapter dc : aiChapters) {
                    childrenMap.computeIfAbsent(dc.getParentId(), k -> new ArrayList<>()).add(dc);
                }
                List<DocChapter> rootChapters = childrenMap.getOrDefault(0L, new ArrayList<>());
                rootChapters.sort(Comparator.comparing(DocChapter::getOrderNum));
                int[] chapterCount = {0};
                for (DocChapter rootCh : rootChapters) {
                    chapterCount[0]++;
                    writeChapter(doc, rootCh, childrenMap, 1, chapterCount, new int[]{0}, new int[]{0}, showHighlights);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("生成Word文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * Find the document template via chain:
     * doc_ledger.checklistItemId → project_doc_checklist.template_id
     * → stage_doc_checklist_template.template_id → doc_template_v2
     */
    private DocTemplateV2 findTemplate(DocLedger ledger) {
        if (ledger.getChecklistItemId() == null) return null;
        try {
            // Step 1: project_doc_checklist
            com.military.doc.modules.document.entity.ProjectDocChecklist pdc = checklistMapper.selectById(ledger.getChecklistItemId());
            if (pdc == null || pdc.getTemplateId() == null) return null;
            // Step 2: stage_doc_checklist_template
            com.military.doc.modules.document.entity.StageDocChecklistTemplate sct = stageChecklistTplMapper.selectById(pdc.getTemplateId());
            if (sct == null || sct.getTemplateId() == null) return null;
            // Step 3: doc_template_v2
            return templateV2Mapper.selectById(sct.getTemplateId());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Render template-driven chapters, filling AI content where available.
     */
    private void writeTemplateChapters(XWPFDocument doc, List<DocTemplateChapter> tplChapters,
                                        int parentId, Map<String, String> contentByTitle,
                                        int[] counter, boolean showHighlights) {
        List<DocTemplateChapter> children = new ArrayList<>();
        for (DocTemplateChapter tc : tplChapters) {
            if ((tc.getParentId() == null ? 0L : tc.getParentId()) == parentId) {
                children.add(tc);
            }
        }
        children.sort(Comparator.comparing(c -> c.getOrderNum() != null ? c.getOrderNum() : 0));

        for (DocTemplateChapter tc : children) {
            counter[0]++;
            int level = Math.min(tc.getChapterLevel() != null ? tc.getChapterLevel() : 1, 5);

            // Build heading from template
            String heading = (tc.getChapterNumber() != null ? tc.getChapterNumber() : String.valueOf(counter[0]))
                + " " + (tc.getChapterTitle() != null ? tc.getChapterTitle() : "");
            GjbStyleHelper.addHeading(doc, heading, level);

            // Find matching AI content by title
            String matchedContent = null;
            if (tc.getChapterTitle() != null) {
                for (Map.Entry<String, String> e : contentByTitle.entrySet()) {
                    if (e.getKey().contains(tc.getChapterTitle())
                        || tc.getChapterTitle().contains(e.getKey())) {
                        matchedContent = e.getValue();
                        break;
                    }
                }
            }

            if (matchedContent != null && !matchedContent.isBlank()) {
                GjbStyleHelper.writeMarkdownContent(doc, matchedContent, level, true);
            } else if (showHighlights && Boolean.TRUE.equals(tc.getIsRequired())) {
                String title = tc.getChapterTitle() != null ? tc.getChapterTitle() : "";
                if (title.length() < 50) {
                    GjbStyleHelper.addHighlightedParagraph(doc,
                        "【必填项缺失】" + title + " - 此章节为GJB要求必填内容，请补充", "RED");
                }
            }

            // Render sub-chapters
            writeTemplateChapters(doc, tplChapters, tc.getId().intValue(), contentByTitle, counter, showHighlights);
        }
    }

    @Transactional
    public String generateAndUpload(Long docLedgerId, boolean includeCover, boolean showHighlights) {
        byte[] bytes = generate(docLedgerId, includeCover, showHighlights);
        String fileName = "doc_" + docLedgerId + "_" + System.currentTimeMillis() + ".docx";
        return fileStorageService.upload(new ByteArrayMultipartFile(fileName, bytes));
    }

    private void writeChapter(XWPFDocument doc, DocChapter chapter, Map<Long, List<DocChapter>> childrenMap,
                               int level, int[] chapterCount, int[] tableCount, int[] figureCount,
                               boolean showHighlights) {
        String chapterNum = chapter.getChapterNumber();
        if (chapterNum == null) chapterNum = String.valueOf(chapterCount[0]);

        // Use chapter_level from DB if available, otherwise use tree depth
        int headingLevel = chapter.getChapterLevel() != null ? Math.min(chapter.getChapterLevel(), 5) : level;
        GjbStyleHelper.addHeading(doc, chapterNum + " " + chapter.getChapterTitle(), headingLevel);

        if (chapter.getContent() != null && !chapter.getContent().isBlank()) {
            String content = chapter.getContent();
            String highlightType = null;
            if (showHighlights) {
                if ("PARTIAL".equals(chapter.getFillStatus())) highlightType = "YELLOW";
                if ("EMPTY".equals(chapter.getFillStatus())) highlightType = "RED";
            }
            if (highlightType != null) {
                GjbStyleHelper.addHighlightedParagraph(doc, content, highlightType);
            } else {
                // skipHeadings=true: titles already rendered by addHeading()
                GjbStyleHelper.writeMarkdownContent(doc, content, level, true);
            }
        } else if (showHighlights && chapter.getFillStatus() != null && !"FILLED".equals(chapter.getFillStatus())) {
            // Skip RED marker if title is long — AI may have put body content in the heading line
            String title = chapter.getChapterTitle();
            if (title == null || title.length() < 50) {
                String msg = "【必填项缺失】" + title + " - 此章节为GJB要求必填内容，请补充";
                GjbStyleHelper.addHighlightedParagraph(doc, msg, "RED");
            }
        }

        // Write sub-chapters
        List<DocChapter> children = childrenMap.getOrDefault(chapter.getId(), new ArrayList<>());
        children.sort(Comparator.comparing(DocChapter::getOrderNum));
        int[] subCount = {0};
        for (DocChapter child : children) {
            subCount[0]++;
            writeChapter(doc, child, childrenMap, Math.min(level + 1, 5),
                    subCount, tableCount, figureCount, showHighlights);
        }
    }

    private static class ByteArrayMultipartFile implements org.springframework.web.multipart.MultipartFile {
        private final String name;
        private final byte[] content;

        ByteArrayMultipartFile(String name, byte[] content) {
            this.name = name;
            this.content = content;
        }

        @Override public String getName() { return "file"; }
        @Override public String getOriginalFilename() { return name; }
        @Override public String getContentType() { return "application/vnd.openxmlformats-officedocument.wordprocessingml.document"; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public java.io.InputStream getInputStream() { return new java.io.ByteArrayInputStream(content); }
        @Override public void transferTo(java.io.File dest) throws java.io.IOException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }
}
