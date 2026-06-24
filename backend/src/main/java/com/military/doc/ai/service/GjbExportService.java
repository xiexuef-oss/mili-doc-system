package com.military.doc.ai.service;

import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.mapper.DocChapterMapper;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * GJB 格式导出服务（Phase 6）。
 * 将文档导出为 GJB 规范的文本格式。
 */
@Slf4j
@Service
public class GjbExportService {

    private final DocLedgerMapper docLedgerMapper;
    private final DocChapterMapper chapterMapper;

    public GjbExportService(DocLedgerMapper docLedgerMapper, DocChapterMapper chapterMapper) {
        this.docLedgerMapper = docLedgerMapper;
        this.chapterMapper = chapterMapper;
    }

    /**
     * 导出文档为纯文本（GJB 格式）。
     */
    public String exportAsText(Long ledgerId) {
        DocLedger doc = docLedgerMapper.selectById(ledgerId);
        if (doc == null) return "";

        List<DocChapter> chapters = chapterMapper.selectList(
            new LambdaQueryWrapper<DocChapter>()
                .eq(DocChapter::getDocLedgerId, ledgerId)
                .orderByAsc(DocChapter::getOrderNum));

        StringBuilder sb = new StringBuilder();

        // Cover page
        sb.append("═══════════════════════════════════════\n");
        sb.append("  ").append(doc.getDocName() != null ? doc.getDocName() : "文档").append("\n");
        sb.append("═══════════════════════════════════════\n");
        if (doc.getDocCode() != null) sb.append("  文档编号: ").append(doc.getDocCode()).append("\n");
        if (doc.getSecurityLevel() != null) sb.append("  密级: ").append(doc.getSecurityLevel()).append("\n");
        sb.append("\n");

        // Approval page
        sb.append("───────────────────────────────────────\n");
        sb.append("  编制: ___________  日期: ___________\n");
        sb.append("  审核: ___________  日期: ___________\n");
        sb.append("  批准: ___________  日期: ___________\n");
        sb.append("───────────────────────────────────────\n\n");

        // Content
        for (DocChapter ch : chapters) {
            if (ch.getContent() != null) {
                sb.append(ch.getContent()).append("\n\n");
            }
        }

        log.info("Exported document {}: {} chars", ledgerId, sb.length());
        return sb.toString();
    }

    /**
     * 导出为 Markdown 格式。
     */
    public String exportAsMarkdown(Long ledgerId) {
        // Same as text but preserves markdown formatting
        return exportAsText(ledgerId);
    }
}
