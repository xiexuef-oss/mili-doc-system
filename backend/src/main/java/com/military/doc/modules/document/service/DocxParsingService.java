package com.military.doc.modules.document.service;

import com.military.doc.common.storage.FileStorageService;
import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.mapper.DocChapterMapper;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

/**
 * Parses uploaded .docx files and extracts chapter content back into doc_chapter records.
 * Uses heading styles to identify chapter boundaries.
 */
@Service
public class DocxParsingService {

    @Autowired private DocChapterMapper docChapterMapper;
    @Autowired private FileStorageService fileStorageService;

    public List<ChapterExtract> parseDocx(String fileObjectId) {
        List<ChapterExtract> chapters = new ArrayList<>();
        try (InputStream is = fileStorageService.download(fileObjectId)) {
            XWPFDocument doc = new XWPFDocument(is);

            String currentHeading = null;
            StringBuilder currentContent = new StringBuilder();
            int headingLevel = 0;

            for (IBodyElement element : doc.getBodyElements()) {
                if (element instanceof XWPFParagraph para) {
                    String style = para.getStyle();
                    String text = para.getText().trim();

                    if (style != null && style.startsWith("Heading") && !text.isEmpty()) {
                        // Save previous chapter
                        if (currentHeading != null) {
                            chapters.add(new ChapterExtract(currentHeading, headingLevel, currentContent.toString().trim()));
                        }
                        currentHeading = text;
                        currentContent = new StringBuilder();
                        headingLevel = Integer.parseInt(style.replace("Heading", ""));
                    } else if (currentHeading != null && !text.isEmpty()) {
                        currentContent.append(text).append("\n");
                    }
                } else if (element instanceof XWPFTable table) {
                    if (currentContent.length() > 0) {
                        currentContent.append("\n[表格]\n");
                        for (XWPFTableRow row : table.getRows()) {
                            List<String> cells = new ArrayList<>();
                            for (XWPFTableCell cell : row.getTableCells()) {
                                cells.add(cell.getText().trim());
                            }
                            currentContent.append(String.join(" | ", cells)).append("\n");
                        }
                    }
                }
            }

            // Save last chapter
            if (currentHeading != null) {
                chapters.add(new ChapterExtract(currentHeading, headingLevel, currentContent.toString().trim()));
            }
        } catch (Exception e) {
            throw new RuntimeException("解析Word文档失败: " + e.getMessage(), e);
        }
        return chapters;
    }

    public int updateChapters(Long docLedgerId, String fileObjectId, Long operatorId) {
        List<ChapterExtract> extracts = parseDocx(fileObjectId);
        List<DocChapter> existing = docChapterMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocChapter>()
                        .eq(DocChapter::getDocLedgerId, docLedgerId)
                        .orderByAsc(DocChapter::getOrderNum));

        int updated = 0;
        for (ChapterExtract ext : extracts) {
            for (DocChapter dc : existing) {
                String title = (dc.getChapterNumber() != null ? dc.getChapterNumber() + " " : "") + dc.getChapterTitle();
                if (ext.heading.contains(dc.getChapterTitle()) || ext.heading.equals(title)) {
                    dc.setContent(ext.content);
                    dc.setFillStatus(ext.content != null && !ext.content.isBlank() ? "PARTIAL" : "EMPTY");
                    dc.setUpdatedBy(operatorId);
                    docChapterMapper.updateById(dc);
                    updated++;
                    break;
                }
            }
        }
        return updated;
    }

    public record ChapterExtract(String heading, int level, String content) {}
}
