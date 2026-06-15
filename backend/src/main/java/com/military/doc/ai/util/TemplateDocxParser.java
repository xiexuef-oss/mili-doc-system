package com.military.doc.ai.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板 DOCX 结构解析器。
 * 将上传的 GJB 标准格式 DOCX 模板自动提取为结构化数据，
 * 供大模型直接理解（章节树、变量占位符、表格结构、封面特征）。
 */
@Slf4j
@Component
public class TemplateDocxParser {

    // ---- GJB-style heading patterns ----
    private static final Pattern GJB_CHAPTER = Pattern.compile(
        "^第[一二三四五六七八九十百零\\d]+章\\s*");
    private static final Pattern NUMBERED_SECTION = Pattern.compile(
        "^(\\d+(?:\\.\\d+)*)\\s+");
    private static final Pattern CHINESE_NUM = Pattern.compile(
        "^[一二三四五六七八九十]+[、，,]\\s*");

    // ---- Variable placeholder patterns ----
    private static final Pattern[] VAR_PATTERNS = {
        Pattern.compile("\\{[A-Za-z_\\u4e00-\\u9fa5][A-Za-z0-9_\\u4e00-\\u9fa5]*\\}"), // {变量名}
        Pattern.compile("【[^】]+】"),                                                    // 【填写内容】
        Pattern.compile("[×X]{2,}"),                                                     // XXX / ×××
        Pattern.compile("_{3,}"),                                                        // ___ 下划线占位
        Pattern.compile("XXX-XX-XXX"),                                                   // 文档编号模板
    };

    // ---- Cover page indicators ----
    private static final Set<String> COVER_INDICATORS = Set.of(
        "密级", "秘密", "机密", "绝密", "内部",
        "文档编号", "文件编号", "项目编号",
        "编制", "审核", "批准", "标准化",
        "版本", "阶段", "标识"
    );

    // ---- Noise patterns ----
    private static final Pattern PAGE_NUMBER = Pattern.compile("^\\s*\\d{1,4}\\s*$");
    private static final Pattern HEADER_FOOTER = Pattern.compile(
        "^(ICS|备案号|发布日期|实施日期|发布|中华人民共和国).*");

    /**
     * Parse a DOCX byte array into structured template representation.
     */
    public TemplateStructure parse(byte[] docxBytes, String fileName) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            TemplateStructure structure = new TemplateStructure();
            structure.setFileName(fileName);

            // Phase 1: Extract all paragraphs and tables in document order
            List<DocumentElement> elements = extractElements(doc);

            // Phase 2: Detect cover page section
            int coverEnd = detectCoverEnd(elements, 15); // first 15 paragraphs
            structure.setHasCover(coverEnd > 0);
            if (coverEnd > 0) {
                structure.setCoverElements(extractCoverInfo(elements.subList(0, coverEnd)));
            }

            // Phase 3: Build chapter tree from heading styles
            List<ChapterNode> chapterTree = buildChapterTree(elements, coverEnd);
            structure.setChapterTree(chapterTree);

            // Phase 4: Detect variable placeholders across all text
            List<VariablePlaceholder> allVars = new ArrayList<>();
            for (DocumentElement el : elements) {
                allVars.addAll(detectVariables(el.getText(), el.getChapterPath()));
            }
            structure.setVariables(allVars);

            // Phase 5: Extract table structures
            List<TableStructure> tables = extractTables(elements);
            structure.setTables(tables);

            // Phase 6: Extract full text
            StringBuilder fullText = new StringBuilder();
            for (DocumentElement el : elements) {
                if (el.isHeading()) {
                    fullText.append("\n").append("#".repeat(Math.min(el.getHeadingLevel(), 6)))
                        .append(" ").append(el.getText()).append("\n\n");
                } else if (el.isTable()) {
                    fullText.append(el.getTableMarkdown()).append("\n\n");
                } else {
                    fullText.append(el.getText()).append("\n\n");
                }
            }
            structure.setRawText(fullText.toString());

            // Phase 7: Detect title and security info from cover
            structure.setTitle(detectTitle(elements, fileName));
            structure.setSecurityLevel(detectSecurityLevel(fullText.toString()));
            structure.setDocCodeFormat(detectDocCodeFormat(fullText.toString()));

            log.info("TemplateDocxParser: parsed '{}': {} chapters, {} variables, {} tables",
                fileName, chapterTree.size(), allVars.size(), tables.size());
            return structure;
        }
    }

    // ---- Extraction ----

    private List<DocumentElement> extractElements(XWPFDocument doc) {
        List<DocumentElement> elements = new ArrayList<>();
        for (IBodyElement bodyEl : doc.getBodyElements()) {
            if (bodyEl instanceof XWPFParagraph para) {
                DocumentElement el = extractParagraph(para);
                if (el != null) elements.add(el);
            } else if (bodyEl instanceof XWPFTable table) {
                DocumentElement el = extractTable(table);
                if (el != null) elements.add(el);
            }
        }
        return elements;
    }

    private DocumentElement extractParagraph(XWPFParagraph para) {
        String text = para.getText();
        if (text == null) text = "";
        String trimmed = text.trim();

        // Skip empty paragraphs, page numbers, header/footer noise
        if (trimmed.isEmpty() || trimmed.length() < 1) return null;
        if (PAGE_NUMBER.matcher(trimmed).matches()) return null;
        if (HEADER_FOOTER.matcher(trimmed).matches()) return null;

        DocumentElement el = new DocumentElement();
        el.setText(trimmed);
        el.setElementType("PARAGRAPH");

        // Determine heading level
        String style = para.getStyle();
        int headingLevel = 0;

        if (style != null && style.startsWith("Heading")) {
            try {
                headingLevel = Integer.parseInt(style.replace("Heading", ""));
            } catch (NumberFormatException ignored) {}
        }

        // Fallback: detect heading by content pattern
        if (headingLevel == 0) {
            if (GJB_CHAPTER.matcher(trimmed).find()) {
                headingLevel = 1;
            } else if (NUMBERED_SECTION.matcher(trimmed).find()) {
                // Determine level by number of dots
                Matcher m = NUMBERED_SECTION.matcher(trimmed);
                if (m.find()) {
                    String num = m.group(1);
                    long dots = num.chars().filter(c -> c == '.').count();
                    headingLevel = (int) Math.min(dots + 2, 6);
                }
            } else if (CHINESE_NUM.matcher(trimmed).find()) {
                headingLevel = 2;
            }
        }

        el.setHeadingLevel(headingLevel);

        // Detect formatting (bold, underline, color)
        if (!para.getRuns().isEmpty()) {
            XWPFRun firstRun = para.getRuns().get(0);
            el.setBold(firstRun.isBold());
            el.setUnderline(firstRun.getUnderline() != UnderlinePatterns.NONE);
            String color = firstRun.getColor();
            if (color != null && !color.equals("000000") && !color.equals("auto")) {
                el.setFontColor(color);
            }
        }

        // Detect variable placeholders in this paragraph
        String chapterPath = headingLevel > 0 ? trimmed : null;
        List<VariablePlaceholder> vars = detectVariables(trimmed, chapterPath);
        if (!vars.isEmpty()) {
            el.setHasVariables(true);
        }

        return el;
    }

    private DocumentElement extractTable(XWPFTable table) {
        DocumentElement el = new DocumentElement();
        el.setElementType("TABLE");

        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();

        for (int i = 0; i < table.getRows().size(); i++) {
            XWPFTableRow row = table.getRows().get(i);
            List<String> cells = new ArrayList<>();
            for (XWPFTableCell cell : row.getTableCells()) {
                cells.add(cell.getText().trim());
            }
            if (i == 0) {
                headers.addAll(cells);
            } else {
                rows.add(cells);
            }
        }

        // Build markdown representation
        StringBuilder md = new StringBuilder();
        if (!headers.isEmpty()) {
            md.append("| ").append(String.join(" | ", headers)).append(" |\n");
            md.append("|").append("---|".repeat(headers.size())).append("\n");
        }
        for (List<String> row : rows) {
            // Pad row to match header count
            while (row.size() < headers.size()) row.add("");
            md.append("| ").append(String.join(" | ", row)).append(" |\n");
        }

        el.setTableMarkdown(md.toString());
        el.setTableHeaders(headers);
        el.setTableRows(rows);

        // Check for caption preceding this table (stored as text)
        String caption = detectTableCaption(table);
        if (caption != null) {
            el.setText(caption);
        }

        // Detect variables in table cells
        StringBuilder allCellText = new StringBuilder();
        for (String h : headers) allCellText.append(h).append(" ");
        for (List<String> row : rows) {
            for (String cell : row) allCellText.append(cell).append(" ");
        }
        el.setHasVariables(!detectVariables(allCellText.toString(), null).isEmpty());

        return el;
    }

    // ---- Cover detection ----

    private int detectCoverEnd(List<DocumentElement> elements, int maxCheck) {
        int limit = Math.min(elements.size(), maxCheck);
        int coverScore = 0;
        for (int i = 0; i < limit; i++) {
            String text = elements.get(i).getText();
            for (String indicator : COVER_INDICATORS) {
                if (text.contains(indicator)) coverScore++;
            }
            // First real heading signals end of cover
            if (elements.get(i).getHeadingLevel() == 1 && i > 1 && coverScore >= 2) {
                return i;
            }
        }
        return coverScore >= 3 ? limit : 0;
    }

    private Map<String, String> extractCoverInfo(List<DocumentElement> coverElements) {
        Map<String, String> info = new LinkedHashMap<>();
        for (DocumentElement el : coverElements) {
            String text = el.getText();
            if (text.contains("密级") || text.contains("秘密") || text.contains("机密") || text.contains("绝密")) {
                info.put("securityLevel", text);
            }
            if (text.contains("文档编号") || text.contains("文件编号") || text.contains("项目编号")) {
                info.put("docCode", text);
            }
            if (text.contains("阶段")) {
                info.put("stage", text);
            }
            if (text.contains("版本")) {
                info.put("version", text);
            }
        }
        return info;
    }

    // ---- Chapter tree ----

    private List<ChapterNode> buildChapterTree(List<DocumentElement> elements, int startFrom) {
        List<ChapterNode> roots = new ArrayList<>();
        Deque<ChapterNode> stack = new ArrayDeque<>();

        for (int i = startFrom; i < elements.size(); i++) {
            DocumentElement el = elements.get(i);
            if (el.getHeadingLevel() <= 0) continue;

            ChapterNode node = new ChapterNode();
            node.setTitle(el.getText());
            node.setLevel(el.getHeadingLevel());
            node.setHeadingStyle(el.isHeading() ? "Heading" + el.getHeadingLevel() : "PATTERN");
            node.setBold(el.isBold());
            node.setFontColor(el.getFontColor());

            // Detect numbering format
            Matcher m = NUMBERED_SECTION.matcher(el.getText());
            if (m.find()) {
                node.setNumberingFormat(m.group(1));
            } else if (GJB_CHAPTER.matcher(el.getText()).find()) {
                node.setNumberingFormat("章");
            } else if (CHINESE_NUM.matcher(el.getText()).find()) {
                node.setNumberingFormat("中文编号");
            }

            // Pop stack to find parent
            while (!stack.isEmpty() && stack.peek().getLevel() >= node.getLevel()) {
                stack.pop();
            }

            if (stack.isEmpty()) {
                roots.add(node);
            } else {
                stack.peek().getChildren().add(node);
                node.setParent(stack.peek());
            }
            stack.push(node);

            // Collect content until next heading
            StringBuilder content = new StringBuilder();
            List<TableStructure> nodeTables = new ArrayList<>();
            List<VariablePlaceholder> nodeVars = new ArrayList<>();
            for (int j = i + 1; j < elements.size(); j++) {
                DocumentElement next = elements.get(j);
                if (next.getHeadingLevel() > 0 && next.getHeadingLevel() <= node.getLevel()) break;
                if (next.getHeadingLevel() > node.getLevel()) continue; // skip sub-headings

                if (next.isTable()) {
                    TableStructure ts = new TableStructure();
                    ts.setCaption(next.getText());
                    ts.setHeaders(next.getTableHeaders());
                    ts.setRows(next.getTableRows());
                    ts.setMarkdown(next.getTableMarkdown());
                    nodeTables.add(ts);
                    content.append(next.getTableMarkdown()).append("\n\n");
                } else {
                    content.append(next.getText()).append("\n\n");
                    if (next.isHasVariables()) {
                        nodeVars.addAll(detectVariables(next.getText(), node.getTitle()));
                    }
                }
            }
            node.setBodyContent(content.toString().trim());
            node.setTables(nodeTables);
            node.setVariables(nodeVars);
            node.setHasTable(!nodeTables.isEmpty());

            // Generate writing tips from content analysis
            node.setDescription(generateDescription(node));
            node.setWritingTips(generateWritingTips(node));
            node.setSampleContent(extractSampleContent(node));
        }

        return roots;
    }

    // ---- Variable detection ----

    private List<VariablePlaceholder> detectVariables(String text, String chapterPath) {
        List<VariablePlaceholder> vars = new ArrayList<>();
        if (text == null || text.isBlank()) return vars;

        for (Pattern pattern : VAR_PATTERNS) {
            Matcher m = pattern.matcher(text);
            while (m.find()) {
                String placeholder = m.group().trim();
                // Deduplicate
                boolean exists = vars.stream().anyMatch(v -> v.getPlaceholder().equals(placeholder));
                if (!exists) {
                    VariablePlaceholder vp = new VariablePlaceholder();
                    vp.setPlaceholder(placeholder);
                    vp.setChapterPath(chapterPath);
                    vp.setType(categorizeVariable(placeholder));
                    vars.add(vp);
                }
            }
        }
        return vars;
    }

    private String categorizeVariable(String placeholder) {
        if (placeholder.contains("编号") || placeholder.contains("代码") || placeholder.matches("XXX-XX-XXX")) {
            return "编号/代码";
        }
        if (placeholder.contains("名称") || placeholder.contains("型号")) {
            return "名称/型号";
        }
        if (placeholder.contains("日期") || placeholder.contains("时间") || placeholder.contains("阶段")) {
            return "日期/时间";
        }
        if (placeholder.contains("指标") || placeholder.contains("参数") || placeholder.contains("性能")) {
            return "技术指标";
        }
        if (placeholder.startsWith("{") && placeholder.endsWith("}")) {
            return "结构化变量";
        }
        if (placeholder.matches("[×X]{2,}")) {
            return "占位标记";
        }
        if (placeholder.matches("_{3,}")) {
            return "填空占位";
        }
        return "其他";
    }

    // ---- Table extraction ----

    private List<TableStructure> extractTables(List<DocumentElement> elements) {
        List<TableStructure> tables = new ArrayList<>();
        for (int i = 0; i < elements.size(); i++) {
            DocumentElement el = elements.get(i);
            if (!el.isTable()) continue;

            TableStructure ts = new TableStructure();
            // Try to find caption from preceding element
            if (i > 0 && !elements.get(i - 1).isHeading() && !elements.get(i - 1).isTable()) {
                String prevText = elements.get(i - 1).getText();
                if (prevText.contains("表") || prevText.contains("Table")) {
                    ts.setCaption(prevText);
                }
            }
            ts.setHeaders(el.getTableHeaders());
            ts.setRows(el.getTableRows());
            ts.setMarkdown(el.getTableMarkdown());
            ts.setColumnCount(el.getTableHeaders().size());
            ts.setRowCount(el.getTableRows().size());

            // Detect variables in table
            StringBuilder allText = new StringBuilder();
            el.getTableHeaders().forEach(allText::append);
            el.getTableRows().forEach(row -> row.forEach(allText::append));
            ts.setVariablePlaceholders(
                detectVariables(allText.toString(), null).stream()
                    .map(VariablePlaceholder::getPlaceholder)
                    .distinct()
                    .toList()
            );

            tables.add(ts);
        }
        return tables;
    }

    private String detectTableCaption(XWPFTable table) {
        // POI doesn't easily give us the preceding paragraph,
        // so caption detection is done in extractTables by looking at previous element
        return null;
    }

    // ---- Title / security / code detection ----

    private String detectTitle(List<DocumentElement> elements, String fileName) {
        // Look for the most prominent text in the first few elements
        for (int i = 0; i < Math.min(elements.size(), 10); i++) {
            String text = elements.get(i).getText();
            // Skip noise
            if (text.length() > 5 && text.length() < 200
                && !text.startsWith("ICS") && !text.contains("备案号")
                && !PAGE_NUMBER.matcher(text).matches()) {
                // If this looks like a title (not a metadata line)
                if (!text.contains("：") && !text.contains(":") && text.length() > 5) {
                    return text;
                }
            }
        }
        // Fallback: derive from filename
        if (fileName != null) {
            String name = fileName;
            int dot = name.lastIndexOf('.');
            if (dot > 0) name = name.substring(0, dot);
            return name;
        }
        return "未命名模板";
    }

    private String detectSecurityLevel(String fullText) {
        if (fullText.contains("绝密")) return "绝密";
        if (fullText.contains("机密")) return "机密";
        if (fullText.contains("秘密")) return "秘密";
        if (fullText.contains("内部")) return "内部";
        return "公开";
    }

    private String detectDocCodeFormat(String fullText) {
        // Try to detect document numbering pattern
        Matcher m = Pattern.compile("([A-Z]+-XX-[A-Z]+-\\d{3})").matcher(fullText);
        if (m.find()) return m.group(1);
        m = Pattern.compile("([A-Z]{2,4}-[A-Z]{2,4}-[A-Z]{2,4})").matcher(fullText);
        if (m.find()) return m.group(1);
        return "";
    }

    // ---- Content analysis for AI hints ----

    private String generateDescription(ChapterNode node) {
        StringBuilder desc = new StringBuilder();
        if (node.isHasTable()) {
            desc.append("包含").append(node.getTables().size()).append("个表格");
        }
        if (!node.getVariables().isEmpty()) {
            if (!desc.isEmpty()) desc.append("，");
            desc.append("需填写").append(node.getVariables().size()).append("个变量");
        }
        if (!desc.isEmpty()) {
            desc.insert(0, "本章节");
        }
        return desc.toString();
    }

    private String generateWritingTips(ChapterNode node) {
        StringBuilder tips = new StringBuilder();

        // Variable hints
        if (!node.getVariables().isEmpty()) {
            tips.append("需填写的变量: ");
            tips.append(node.getVariables().stream()
                .map(VariablePlaceholder::getPlaceholder)
                .limit(10)
                .reduce((a, b) -> a + ", " + b).orElse(""));
            tips.append("。");
        }

        // Table hints
        if (node.isHasTable()) {
            if (!tips.isEmpty()) tips.append(" ");
            tips.append("参考表格模板填写，保持格式一致。");
        }

        // Formatting hints
        if (node.isBold() && node.getFontColor() != null) {
            if (!tips.isEmpty()) tips.append(" ");
            tips.append("标题使用特殊格式（加粗+颜色" + node.getFontColor() + "）。");
        }

        return tips.toString();
    }

    private String extractSampleContent(ChapterNode node) {
        // Use body content as sample, but truncate if too long
        String body = node.getBodyContent();
        if (body == null || body.isBlank()) return "";
        // Keep table structure and first paragraph as sample
        String[] parts = body.split("\n\n", 3);
        StringBuilder sample = new StringBuilder();
        for (int i = 0; i < Math.min(parts.length, 2); i++) {
            sample.append(parts[i]).append("\n\n");
        }
        return sample.toString().trim();
    }

    // ============================================================
    // Inner data classes
    // ============================================================

    @lombok.Data
    public static class TemplateStructure {
        private String fileName;
        private String title;
        private String securityLevel;
        private String docCodeFormat;
        private boolean hasCover;
        private Map<String, String> coverElements = new LinkedHashMap<>();
        private List<ChapterNode> chapterTree = new ArrayList<>();
        private List<VariablePlaceholder> variables = new ArrayList<>();
        private List<TableStructure> tables = new ArrayList<>();
        private String rawText;

        /** Flatten chapter tree to list (for DB insertion). */
        public List<ChapterNode> flattenChapters() {
            List<ChapterNode> flat = new ArrayList<>();
            flatten(chapterTree, flat);
            // Assign order numbers
            for (int i = 0; i < flat.size(); i++) {
                flat.get(i).setOrderNum(i + 1);
            }
            return flat;
        }

        private void flatten(List<ChapterNode> nodes, List<ChapterNode> result) {
            for (ChapterNode node : nodes) {
                result.add(node);
                flatten(node.getChildren(), result);
            }
        }
    }

    @lombok.Data
    public static class ChapterNode {
        private String title;
        private int level;
        private int orderNum;
        private String headingStyle;
        private String numberingFormat;
        private boolean bold;
        private String fontColor;
        private boolean hasTable;
        private String bodyContent;
        private String description;
        private String writingTips;
        private String sampleContent;
        private ChapterNode parent;
        private List<ChapterNode> children = new ArrayList<>();
        private List<TableStructure> tables = new ArrayList<>();
        private List<VariablePlaceholder> variables = new ArrayList<>();

        /** Get full chapter path like "3 > 3.1 > 3.1.2" */
        public String getPath() {
            if (parent == null) return title;
            return parent.getPath() + " > " + title;
        }
    }

    @lombok.Data
    public static class VariablePlaceholder {
        private String placeholder;
        private String chapterPath;
        private String type;
    }

    @lombok.Data
    public static class TableStructure {
        private String caption;
        private List<String> headers = new ArrayList<>();
        private List<List<String>> rows = new ArrayList<>();
        private String markdown;
        private int columnCount;
        private int rowCount;
        private List<String> variablePlaceholders = new ArrayList<>();
    }

    @lombok.Data
    static class DocumentElement {
        private String elementType; // "PARAGRAPH" or "TABLE"
        private String text;
        private int headingLevel;
        private boolean bold;
        private boolean underline;
        private String fontColor;
        private boolean hasVariables;
        // Table-specific
        private String tableMarkdown;
        private List<String> tableHeaders;
        private List<List<String>> tableRows;

        boolean isHeading() { return headingLevel > 0; }
        boolean isTable() { return "TABLE".equals(elementType); }
        boolean isParagraph() { return "PARAGRAPH".equals(elementType); }
        String getChapterPath() { return headingLevel > 0 ? text : null; }
    }
}
