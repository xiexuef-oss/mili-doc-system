package com.military.doc.ai.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 智能上下文截断服务。
 * 按优先级四级截断，确保关键信息（标准条款、模板）永不丢失。
 *
 * <p>优先级:
 * TIER_1 (永不截断): 标准条款、GJB 要求、模板结构
 * TIER_2 (最后截断): 项目主数据
 * TIER_3 (先截断): 输入文件摘要
 * TIER_4 (最先截断): 输入文件全文、知识库详情
 */
@Slf4j
@Service
public class SmartTruncationService {

    public enum ContextTier {
        TIER_1, TIER_2, TIER_3, TIER_4
    }

    /** 标记章节的优先级映射 */
    private static final Map<String, ContextTier> TIER_MAP = new LinkedHashMap<>();

    static {
        // TIER_1: 标准条款和模板 — 永不截断
        TIER_MAP.put("适用标准", ContextTier.TIER_1);
        TIER_MAP.put("标准条款", ContextTier.TIER_1);
        TIER_MAP.put("模板", ContextTier.TIER_1);
        TIER_MAP.put("模板库", ContextTier.TIER_1);
        TIER_MAP.put("模版", ContextTier.TIER_1);
        TIER_MAP.put("GJB", ContextTier.TIER_1);
        TIER_MAP.put("适用标准条款", ContextTier.TIER_1);

        // TIER_2: 主数据 — 尽可能保留
        TIER_MAP.put("主数据", ContextTier.TIER_2);
        TIER_MAP.put("项目主数据", ContextTier.TIER_2);
        TIER_MAP.put("项目信息", ContextTier.TIER_2);
        TIER_MAP.put("装备信息", ContextTier.TIER_2);
        TIER_MAP.put("战术指标", ContextTier.TIER_2);

        // TIER_3: 输入文件摘要
        TIER_MAP.put("输入文件", ContextTier.TIER_3);
        TIER_MAP.put("输入文档", ContextTier.TIER_3);

        // TIER_4: 全文内容、知识库文章
        TIER_MAP.put("全文", ContextTier.TIER_4);
        TIER_MAP.put("知识库", ContextTier.TIER_4);
        TIER_MAP.put("知识", ContextTier.TIER_4);
    }

    private static final Pattern SECTION_HEADING = Pattern.compile("^#{1,4}\\s+(.+)$", Pattern.MULTILINE);

    /**
     * 智能截断上下文文本。
     *
     * @param context 原始上下文
     * @param maxChars 最大字符数
     * @return 截断后的上下文
     */
    public String smartTruncate(String context, int maxChars) {
        if (context == null || context.isEmpty()) return context;
        if (context.length() <= maxChars) return context;

        List<Section> sections = parseSections(context);
        if (sections.isEmpty()) {
            // 无法解析章节，简单尾部截断
            log.warn("Cannot parse sections for smart truncation, falling back to tail truncation");
            return context.substring(0, maxChars - 50) + "\n\n[上下文超出限制，已截断...]";
        }

        // 按优先级分组
        List<Section> tier1 = new ArrayList<>();
        List<Section> tier2 = new ArrayList<>();
        List<Section> tier3 = new ArrayList<>();
        List<Section> tier4 = new ArrayList<>();
        List<Section> uncategorized = new ArrayList<>();

        for (Section sec : sections) {
            ContextTier tier = classifyTier(sec.title);
            switch (tier) {
                case TIER_1 -> tier1.add(sec);
                case TIER_2 -> tier2.add(sec);
                case TIER_3 -> tier3.add(sec);
                case TIER_4 -> tier4.add(sec);
                default -> uncategorized.add(sec);
            }
        }

        int originalLen = context.length();
        StringBuilder result = new StringBuilder();
        int budget = maxChars - 100; // 预留截断提示空间

        // TIER_1: 全部保留
        int t1Len = appendSections(result, tier1, Integer.MAX_VALUE);
        budget -= t1Len;

        // TIER_2: 全部保留（如预算不足，截断到 75%）
        int t2Len = appendSections(result, tier2, budget > 0 ? budget : 0);
        budget -= t2Len;

        // TIER_3: 保留50%或截断
        int t3Budget = Math.max(0, budget / 2);
        int t3Len = appendSections(result, tier3, t3Budget);
        budget = budget - t3Len;

        // TIER_4: 只保留摘要（每节前200字符）
        if (budget > 0) {
            for (Section sec : tier4) {
                String summary = summarizeSection(sec, Math.min(budget, 200));
                if (!summary.isEmpty()) {
                    result.append(summary).append("\n");
                    budget -= summary.length();
                }
            }
        }

        // Uncategorized: 剩余预算
        if (budget > 0) {
            appendSections(result, uncategorized, budget);
        }

        result.append("\n\n> ⚠️ 上下文超出模型窗口限制，已智能截断（原始 ")
            .append(originalLen).append(" 字符 → ").append(result.length())
            .append(" 字符）。优先级较低的信息（输入文件全文、知识库详情）已被省略。");

        log.info("Smart truncation: {} → {} chars (tier1={}, tier2={}, tier3={}, tier4={})",
            originalLen, result.length(), t1Len, t2Len, t3Len,
            tier4.stream().mapToInt(s -> s.content.length()).sum());

        return result.toString();
    }

    // ---- private helpers ----

    private List<Section> parseSections(String text) {
        List<Section> sections = new ArrayList<>();
        Matcher m = SECTION_HEADING.matcher(text);
        int lastStart = 0;
        String lastTitle = null;

        while (m.find()) {
            if (lastTitle != null) {
                String content = text.substring(lastStart, m.start()).trim();
                sections.add(new Section(lastTitle, content));
            }
            lastTitle = m.group(1).trim();
            lastStart = m.end();
        }
        // 最后一个section
        if (lastTitle != null) {
            String content = text.substring(lastStart).trim();
            sections.add(new Section(lastTitle, content));
        } else {
            // 没有标题，整个文本作为一个section
            sections.add(new Section("正文", text.trim()));
        }
        return sections;
    }

    private ContextTier classifyTier(String title) {
        for (var entry : TIER_MAP.entrySet()) {
            if (title.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null; // uncategorized
    }

    private int appendSections(StringBuilder sb, List<Section> sections, int budget) {
        int totalLen = 0;
        for (Section sec : sections) {
            int available = budget - totalLen;
            if (available <= 0) break;

            String content = sec.content;
            if (content.length() > available) {
                content = content.substring(0, Math.max(0, available - 30)) + "\n... [已截断]";
            }
            sb.append("## ").append(sec.title).append("\n").append(content).append("\n\n");
            totalLen += sec.title.length() + content.length() + 6;
        }
        return totalLen;
    }

    private String summarizeSection(Section sec, int maxLen) {
        if (sec.content.length() <= maxLen) {
            return "## " + sec.title + "\n" + sec.content + "\n";
        }
        return "## " + sec.title + "\n" + sec.content.substring(0, maxLen - 20)
            + "... [摘要]\n";
    }

    private record Section(String title, String content) {}
}
