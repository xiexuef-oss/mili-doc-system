package com.military.doc.modules.document.util;

import org.apache.poi.xwpf.usermodel.*;

import java.math.BigInteger;

/**
 * GJB 0.2 formatting utilities for Apache POI XWPFDocument.
 *
 * Font sizes per GJB 0.2:
 *   章标题(Heading1): 四号黑体 14pt
 *   条标题(Heading2): 小四号黑体 12pt
 *   正文: 小四号宋体 12pt, 首行缩进2字符, 行距21磅
 *   表头: 五号黑体 10.5pt
 */
public class GjbStyleHelper {

    // Page layout: A4, margins: top/bottom 25mm, left/right 30mm (GJB 0.1A)
    private static final BigInteger A4_W = BigInteger.valueOf(11906);
    private static final BigInteger A4_H = BigInteger.valueOf(16838);
    private static final BigInteger MARGIN_TB = BigInteger.valueOf(1418);  // 25mm
    private static final BigInteger MARGIN_LR = BigInteger.valueOf(1701);  // 30mm

    public static void setupBodySection(XWPFDocument doc) {
        var ctDoc = doc.getDocument();
        var body = ctDoc.getBody();
        var sectPr = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();

        var pageSz = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
        pageSz.setW(A4_W);
        pageSz.setH(A4_H);

        var pageMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        pageMar.setTop(MARGIN_TB);
        pageMar.setBottom(MARGIN_TB);
        pageMar.setLeft(MARGIN_LR);
        pageMar.setRight(MARGIN_LR);
    }

    public static void addCoverPage(XWPFDocument doc, String docTitle, String projectName,
                                     String securityLevel, String orgName) {
        for (int i = 0; i < 6; i++) doc.createParagraph();

        XWPFParagraph secPara = doc.createParagraph();
        secPara.setAlignment(ParagraphAlignment.RIGHT);
        XWPFRun secRun = secPara.createRun();
        secRun.setText("密级：" + (securityLevel != null ? securityLevel : "内部"));
        secRun.setFontSize(12); secRun.setBold(true);

        for (int i = 0; i < 4; i++) doc.createParagraph();

        XWPFParagraph titlePara = doc.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setText(docTitle);
        titleRun.setFontSize(22); titleRun.setBold(true); titleRun.setFontFamily("黑体");

        doc.createParagraph(); doc.createParagraph();

        XWPFParagraph projPara = doc.createParagraph();
        projPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun projRun = projPara.createRun();
        projRun.setText("项目名称：" + (projectName != null ? projectName : ""));
        projRun.setFontSize(14); projRun.setFontFamily("宋体");

        XWPFParagraph orgPara = doc.createParagraph();
        orgPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun orgRun = orgPara.createRun();
        orgRun.setText(orgName != null ? orgName : "编制单位：[请填写]");
        orgRun.setFontSize(14); orgRun.setFontFamily("宋体");

        doc.createParagraph(); doc.createParagraph();

        XWPFParagraph signPara = doc.createParagraph();
        signPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun signRun = signPara.createRun();
        signRun.setText("编    制：______________      审    核：______________");
        signRun.setFontSize(12); signRun.setFontFamily("宋体");

        doc.createParagraph();

        XWPFParagraph apprPara = doc.createParagraph();
        apprPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun apprRun = apprPara.createRun();
        apprRun.setText("批    准：______________      版 本 号：V1.0");
        apprRun.setFontSize(12); apprRun.setFontFamily("宋体");

        // Real Word section break: attach sectPr to PARAGRAPH property, not document body
        XWPFParagraph breakPara = doc.createParagraph();
        var ctp = breakPara.getCTP();
        var pPr = ctp.isSetPPr() ? ctp.getPPr() : ctp.addNewPPr();
        var coverSect = pPr.addNewSectPr(); // paragraph-level sectPr = real section break
        coverSect.addNewTitlePg();
        var pgSz = coverSect.addNewPgSz();
        pgSz.setW(A4_W); pgSz.setH(A4_H);
    }

    public static XWPFParagraph addHeading(XWPFDocument doc, String text, int level) {
        XWPFParagraph para = doc.createParagraph();
        para.setStyle("Heading" + level);
        XWPFRun run = para.createRun();
        run.setText(text); run.setBold(true); run.setFontFamily("黑体");
        switch (level) {
            case 1: run.setFontSize(14); break;  // 四号黑体
            case 2: run.setFontSize(12); break;  // 小四号黑体
            case 3: run.setFontSize(12); break;
            default: run.setFontSize(12); break;
        }
        return para;
    }

    public static XWPFParagraph addBodyText(XWPFDocument doc, String text) {
        XWPFParagraph para = doc.createParagraph();
        para.setFirstLineIndent(480);  // 2-char indent for 12pt text
        para.setSpacingBetween(1.5);  // ~21pt line spacing
        XWPFRun run = para.createRun();
        run.setText(text); run.setFontSize(12); run.setFontFamily("宋体");
        return para;
    }

    // === Markdown-to-DOCX rendering (skipHeadings mode) ===

    public static void writeMarkdownContent(XWPFDocument doc, String markdown, int baseLevel) {
        writeMarkdownContent(doc, markdown, baseLevel, false);
    }

    public static void writeMarkdownContent(XWPFDocument doc, String markdown, int baseLevel, boolean skipHeadings) {
        if (markdown == null || markdown.isBlank()) return;
        String[] lines = markdown.split("\\R");
        java.util.List<String> pendingTableLines = new java.util.ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                flushPendingTable(doc, pendingTableLines); pendingTableLines.clear();
                continue;
            }
            if (trimmed.startsWith("|") && trimmed.contains("|")) {
                pendingTableLines.add(trimmed);
                continue;
            }
            if (!pendingTableLines.isEmpty()) {
                flushPendingTable(doc, pendingTableLines); pendingTableLines.clear();
            }
            if (trimmed.startsWith("#")) {
                if (skipHeadings) continue; // title already rendered by addHeading(), skip entirely
                String headingText = trimmed.replaceAll("^#{1,5}\\s*", "");
                addBodyTextWithBold(doc, "**" + headingText + "**");
            } else if ((trimmed.startsWith("- ") || trimmed.startsWith("* ")) && !trimmed.startsWith("**")) {
                addBulletPoint(doc, trimmed.substring(2).trim());
            } else {
                addBodyTextWithBold(doc, trimmed);
            }
        }
        if (!pendingTableLines.isEmpty()) flushPendingTable(doc, pendingTableLines);
    }

    public static void addBulletPoint(XWPFDocument doc, String text) {
        XWPFParagraph para = doc.createParagraph();
        para.setIndentationLeft(480);
        XWPFRun run = para.createRun();
        run.setText("• " + text); run.setFontSize(12); run.setFontFamily("宋体");
    }

    public static XWPFParagraph addBodyTextWithBold(XWPFDocument doc, String text) {
        XWPFParagraph para = doc.createParagraph();
        para.setFirstLineIndent(480);
        para.setSpacingBetween(1.5);
        String[] parts = text.split("\\*\\*");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            XWPFRun run = para.createRun();
            run.setText(parts[i]); run.setFontSize(12); run.setFontFamily("宋体");
            if (i % 2 == 1) run.setBold(true);
        }
        if (para.getRuns().isEmpty()) {
            XWPFRun run = para.createRun(); run.setText("");
            run.setFontSize(12); run.setFontFamily("宋体");
        }
        return para;
    }

    public static void addHighlightedParagraph(XWPFDocument doc, String text, String highlightType) {
        XWPFParagraph para = doc.createParagraph();
        para.setFirstLineIndent(480);
        XWPFRun run = para.createRun();
        run.setText(text); run.setFontSize(12); run.setFontFamily("宋体");
        if ("YELLOW".equals(highlightType)) run.setColor("996600");
        else if ("RED".equals(highlightType)) { run.setColor("CC0000"); run.setBold(true); }
    }

    public static XWPFTable addTable(XWPFDocument doc, String[] headers,
                                      java.util.List<String[]> rows, String caption) {
        if (caption != null) {
            XWPFParagraph capPara = doc.createParagraph();
            capPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun capRun = capPara.createRun();
            capRun.setText(caption); capRun.setBold(true);
            capRun.setFontSize(10); capRun.setFontFamily("黑体");
        }
        int rowCount = Math.max(rows.size() + 1, 2);
        XWPFTable table = doc.createTable(rowCount, headers.length);
        table.setWidth("100%");
        XWPFTableRow headerRow = table.getRow(0);
        for (int i = 0; i < headers.length; i++) {
            XWPFTableCell cell = headerRow.getCell(i);
            cell.setText(headers[i]); cell.setColor("D9E2F3");
            cell.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
            var hRun = cell.getParagraphs().get(0).getRuns().get(0);
            hRun.setBold(true); hRun.setFontSize(10); hRun.setFontFamily("宋体");
        }
        for (int r = 0; r < rows.size(); r++) {
            XWPFTableRow row = table.getRow(r + 1);
            String[] cols = rows.get(r);
            for (int c = 0; c < cols.length && c < headers.length; c++) {
                row.getCell(c).setText(cols[c] != null ? cols[c] : "");
                var bRun = row.getCell(c).getParagraphs().get(0).getRuns().get(0);
                bRun.setFontSize(9); bRun.setFontFamily("宋体");
            }
        }
        return table;
    }

    // === Internal ===

    private static void flushPendingTable(XWPFDocument doc, java.util.List<String> rawLines) {
        if (rawLines.isEmpty()) return;
        java.util.List<String[]> rows = new java.util.ArrayList<>();
        for (String rawLine : rawLines) {
            if (!rawLine.startsWith("|")) continue;
            if (rawLine.matches("^\\|[\\s:\\-|]+\\|$")) continue;
            String[] cells = rawLine.split("\\|");
            java.util.List<String> cellList = new java.util.ArrayList<>();
            for (int i = 0; i < cells.length; i++) {
                String cell = cells[i].trim();
                if (cell.isEmpty() && (i == 0 || i == cells.length - 1)) continue;
                cellList.add(cell);
            }
            if (!cellList.isEmpty()) rows.add(cellList.toArray(new String[0]));
        }
        if (rows.isEmpty()) return;
        int maxCols = 0;
        for (String[] row : rows) maxCols = Math.max(maxCols, row.length);

        XWPFTable table = doc.createTable(rows.size(), maxCols);
        table.setWidth("100%");
        for (int r = 0; r < rows.size(); r++) {
            XWPFTableRow row = table.getRow(r);
            String[] rowData = rows.get(r);
            for (int c = 0; c < maxCols; c++) {
                XWPFTableCell cell = row.getCell(c);
                cell.removeParagraph(0);
                XWPFParagraph para = cell.addParagraph();
                XWPFRun run = para.createRun();
                run.setText(c < rowData.length ? rowData[c] : "");
                if (r == 0) { run.setFontSize(10); run.setFontFamily("宋体"); run.setBold(true); cell.setColor("D9E2F3"); }
                else { run.setFontSize(9); run.setFontFamily("宋体"); }
            }
        }
    }
}
