package com.military.doc.modules.document.util;

import org.apache.poi.xwpf.usermodel.*;

/**
 * GJB 0.2 formatting utilities for Apache POI XWPFDocument.
 */
public class GjbStyleHelper {

    public static void addCoverPage(XWPFDocument doc, String docTitle, String projectName,
                                     String securityLevel, String orgName) {
        for (int i = 0; i < 6; i++) {
            doc.createParagraph();
        }

        XWPFParagraph secPara = doc.createParagraph();
        secPara.setAlignment(ParagraphAlignment.RIGHT);
        XWPFRun secRun = secPara.createRun();
        secRun.setText("密级：" + (securityLevel != null ? securityLevel : "内部"));
        secRun.setFontSize(12);
        secRun.setBold(true);

        for (int i = 0; i < 4; i++) {
            doc.createParagraph();
        }

        XWPFParagraph titlePara = doc.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setText(docTitle);
        titleRun.setFontSize(22);
        titleRun.setBold(true);
        titleRun.setFontFamily("黑体");

        doc.createParagraph();
        doc.createParagraph();

        XWPFParagraph projPara = doc.createParagraph();
        projPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun projRun = projPara.createRun();
        projRun.setText("项目名称：" + (projectName != null ? projectName : ""));
        projRun.setFontSize(14);
        projRun.setFontFamily("宋体");

        XWPFParagraph orgPara = doc.createParagraph();
        orgPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun orgRun = orgPara.createRun();
        orgRun.setText(orgName != null ? orgName : "");
        orgRun.setFontSize(14);
        orgRun.setFontFamily("宋体");

        XWPFParagraph breakPara = doc.createParagraph();
        breakPara.setPageBreak(true);
    }

    public static XWPFParagraph addHeading(XWPFDocument doc, String text, int level) {
        XWPFParagraph para = doc.createParagraph();
        para.setStyle("Heading" + level);
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontFamily("黑体");
        switch (level) {
            case 1: run.setFontSize(16); break;
            case 2: run.setFontSize(14); break;
            case 3: run.setFontSize(13); break;
            default: run.setFontSize(12); break;
        }
        return para;
    }

    public static XWPFParagraph addBodyText(XWPFDocument doc, String text) {
        XWPFParagraph para = doc.createParagraph();
        para.setFirstLineIndent(480);
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setFontSize(12);
        run.setFontFamily("宋体");
        return para;
    }

    public static void addHighlightedParagraph(XWPFDocument doc, String text, String highlightType) {
        XWPFParagraph para = doc.createParagraph();
        para.setFirstLineIndent(480);
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setFontSize(12);
        run.setFontFamily("宋体");

        if ("YELLOW".equals(highlightType)) {
            run.setColor("996600");
        } else if ("RED".equals(highlightType)) {
            run.setColor("CC0000");
            run.setBold(true);
        }
    }

    public static XWPFTable addTable(XWPFDocument doc, String[] headers,
                                      java.util.List<String[]> rows, String caption) {
        if (caption != null) {
            XWPFParagraph capPara = doc.createParagraph();
            capPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun capRun = capPara.createRun();
            capRun.setText(caption);
            capRun.setBold(true);
            capRun.setFontSize(10);
            capRun.setFontFamily("黑体");
        }

        int rowCount = Math.max(rows.size() + 1, 2);
        int colCount = headers.length;
        XWPFTable table = doc.createTable(rowCount, colCount);
        table.setWidth("100%");

        XWPFTableRow headerRow = table.getRow(0);
        for (int i = 0; i < colCount; i++) {
            XWPFTableCell cell = headerRow.getCell(i);
            cell.setText(headers[i]);
            cell.setColor("D9E2F3");
            cell.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
            cell.getParagraphs().get(0).getRuns().get(0).setBold(true);
            cell.getParagraphs().get(0).getRuns().get(0).setFontSize(10);
        }

        for (int r = 0; r < rows.size(); r++) {
            XWPFTableRow row = table.getRow(r + 1);
            String[] cols = rows.get(r);
            for (int c = 0; c < cols.length && c < colCount; c++) {
                row.getCell(c).setText(cols[c] != null ? cols[c] : "");
                row.getCell(c).getParagraphs().get(0).getRuns().get(0).setFontSize(10);
            }
        }

        return table;
    }
}
