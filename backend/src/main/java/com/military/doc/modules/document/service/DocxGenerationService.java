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
    @Autowired private FileStorageService fileStorageService;
    @Autowired private ObjectMapper objectMapper;

    public byte[] generate(Long docLedgerId, boolean includeCover, boolean showHighlights) {
        DocLedger ledger = docLedgerMapper.selectById(docLedgerId);
        if (ledger == null) throw new RuntimeException("文档台账条目不存在: " + docLedgerId);

        List<DocChapter> chapters = docChapterMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocChapter>()
                        .eq(DocChapter::getDocLedgerId, docLedgerId)
                        .orderByAsc(DocChapter::getOrderNum));

        ProjectMasterData pmd = null;
        if (ledger.getProjectId() != null) {
            pmd = masterDataMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProjectMasterData>()
                            .eq(ProjectMasterData::getProjectId, ledger.getProjectId()));
        }

        DocTemplateV2 template = null;
        if (!chapters.isEmpty() && chapters.get(0).getTemplateChapterId() != null) {
            DocTemplateChapter tc = templateChapterMapper.selectById(chapters.get(0).getTemplateChapterId());
            if (tc != null) {
                template = templateV2Mapper.selectById(tc.getTemplateId());
            }
        }

        try (XWPFDocument doc = new XWPFDocument()) {
            // A4 size is default in XWPFDocument

            if (includeCover) {
                String docTitle = template != null ? template.getTemplateName() : ledger.getDocName();
                String projectName = ledger.getDocName();
                GjbStyleHelper.addCoverPage(doc, docTitle, projectName,
                        ledger.getSecurityLevel(), null);
            }

            // Build chapter tree
            Map<Long, List<DocChapter>> childrenMap = new HashMap<>();
            for (DocChapter dc : chapters) {
                childrenMap.computeIfAbsent(dc.getParentId(), k -> new ArrayList<>()).add(dc);
            }
            List<DocChapter> rootChapters = childrenMap.getOrDefault(0L, new ArrayList<>());
            rootChapters.sort(Comparator.comparing(DocChapter::getOrderNum));

            int[] chapterCount = {0};
            int[] tableCount = {0};
            int[] figureCount = {0};

            for (DocChapter rootCh : rootChapters) {
                chapterCount[0]++;
                writeChapter(doc, rootCh, childrenMap, 1, chapterCount, tableCount, figureCount, showHighlights);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("生成Word文档失败: " + e.getMessage(), e);
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

        GjbStyleHelper.addHeading(doc, chapterNum + " " + chapter.getChapterTitle(), level);

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
                GjbStyleHelper.writeMarkdownContent(doc, content, level);
            }
        } else if (showHighlights && chapter.getFillStatus() != null && !"FILLED".equals(chapter.getFillStatus())) {
            String msg = "【缺项】此章节内容未填写";
            if ("REQUIRED".equals(chapter.getFillStatus()) || true) {
                msg = "【必填项缺失】" + chapter.getChapterTitle() + " - 此章节为GJB要求必填内容，请补充";
            }
            GjbStyleHelper.addHighlightedParagraph(doc, msg, "RED");
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
