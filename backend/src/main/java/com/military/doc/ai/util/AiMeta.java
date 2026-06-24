package com.military.doc.ai.util;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 生成文档章节的质量元数据。
 * 通过解析 AI 输出的 <!-- AI_META ... --> 注释块获得。
 */
@Data
public class AiMeta {
    /** 章节名称 */
    private String chapterName;
    /** 完成度分数 0-100 */
    private Integer completionScore;
    /** 已确认正确的项 */
    private List<String> confirmed = new ArrayList<>();
    /** 待核实的项（AI 不确定，需人工确认） */
    private List<String> toVerify = new ArrayList<>();
    /** 缺项（主数据中无此信息） */
    private List<String> missing = new ArrayList<>();
    /** 是否成功解析到元数据 */
    private boolean parsed;

    public static AiMeta empty() {
        AiMeta meta = new AiMeta();
        meta.parsed = false;
        meta.completionScore = 0;
        return meta;
    }

    /**
     * 综合完成度：取 AI 自评分，无元数据时返回 0。
     */
    public int getScore() {
        return completionScore != null ? completionScore : 0;
    }

    public boolean hasWarnings() {
        return !toVerify.isEmpty() || !missing.isEmpty();
    }
}
