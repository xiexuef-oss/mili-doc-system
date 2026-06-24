package com.military.doc.ai.util;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从 AI 生成的 Markdown 内容中解析 AI_META 元数据注释块。
 *
 * <p>AI_META 格式（XML 注释，嵌入 Markdown 末尾）：
 * <pre>{@code
 * <!-- AI_META
 * 章节: 可靠性要求
 * 完成度: 55
 * 待核实:
 *   - MTBF值来源：使用了合同中的数据，建议与方案报告交叉验证
 *   - 振动条件：合同未明确，按GJB 150.16A通用要求填写
 * 确定项:
 *   - 可靠性定量指标
 *   - 任务剖面定义
 * 缺项:
 *   - 维修性指标（主数据中无此信息）
 * -->
 * }</pre>
 */
@Slf4j
public class AiMetaParser {

    private static final Pattern META_BLOCK = Pattern.compile(
            "<!--\\s*AI_META\\s*(.*?)\\s*-->",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );

    private static final Pattern FIELD_PATTERN = Pattern.compile(
            "^(章节|完成度|待核实|确定项|缺项)\\s*[:：]\\s*(.*)",
            Pattern.MULTILINE
    );

    private static final Pattern LIST_ITEM = Pattern.compile(
            "^\\s*[-•]\\s*(.*)"
    );

    /**
     * 从完整文档内容中解析 AI_META 元数据。
     * 如果没有 AI_META 块或解析失败，返回 {@link AiMeta#empty()}。
     */
    public static AiMeta extract(String content) {
        if (content == null || content.isBlank()) {
            return AiMeta.empty();
        }

        Matcher blockMatcher = META_BLOCK.matcher(content);
        if (!blockMatcher.find()) {
            log.debug("No AI_META block found in content");
            return AiMeta.empty();
        }

        String metaBody = blockMatcher.group(1).trim();
        AiMeta meta = new AiMeta();
        meta.setParsed(true);

        String currentSection = null;
        List<String> currentList = null;

        for (String line : metaBody.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;

            Matcher fieldMatcher = FIELD_PATTERN.matcher(line);
            if (fieldMatcher.matches()) {
                String fieldName = fieldMatcher.group(1).trim();
                String fieldValue = fieldMatcher.group(2).trim();

                switch (fieldName) {
                    case "章节":
                        meta.setChapterName(fieldValue);
                        currentSection = null;
                        currentList = null;
                        break;
                    case "完成度":
                        try {
                            meta.setCompletionScore(Integer.parseInt(fieldValue.replaceAll("[^0-9]", "")));
                        } catch (NumberFormatException e) {
                            log.warn("Invalid completion score: {}", fieldValue);
                        }
                        currentSection = null;
                        currentList = null;
                        break;
                    case "待核实":
                        currentSection = "toVerify";
                        currentList = meta.getToVerify();
                        break;
                    case "确定项":
                        currentSection = "confirmed";
                        currentList = meta.getConfirmed();
                        break;
                    case "缺项":
                        currentSection = "missing";
                        currentList = meta.getMissing();
                        break;
                }
                continue;
            }

            // List item under current section
            Matcher itemMatcher = LIST_ITEM.matcher(line);
            if (itemMatcher.matches() && currentList != null) {
                currentList.add(itemMatcher.group(1).trim());
            }
        }

        return meta;
    }

    /**
     * 从内容中移除 AI_META 注释块，返回干净的文档内容。
     */
    public static String stripMeta(String content) {
        if (content == null) return null;
        return META_BLOCK.matcher(content).replaceAll("").trim();
    }

    /** Build an AI_META comment block from an AiMeta object. */
    public static String buildMetaBlock(AiMeta meta) {
        if (meta == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("<!-- AI_META\n");
        if (meta.getChapterName() != null) sb.append("章节: ").append(meta.getChapterName()).append("\n");
        sb.append("完成度: ").append(meta.getCompletionScore()).append("\n");
        if (meta.getConfirmed() != null && !meta.getConfirmed().isEmpty()) {
            sb.append("确定项:\n");
            for (String s : meta.getConfirmed()) sb.append("  - ").append(s).append("\n");
        }
        if (meta.getToVerify() != null && !meta.getToVerify().isEmpty()) {
            sb.append("待核实:\n");
            for (String s : meta.getToVerify()) sb.append("  - ").append(s).append("\n");
        }
        if (meta.getMissing() != null && !meta.getMissing().isEmpty()) {
            sb.append("缺项:\n");
            for (String s : meta.getMissing()) sb.append("  - ").append(s).append("\n");
        }
        sb.append("-->\n");
        return sb.toString();
    }
}
