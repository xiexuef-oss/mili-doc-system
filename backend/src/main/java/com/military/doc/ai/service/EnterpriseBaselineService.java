package com.military.doc.ai.service;

import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 企业基线系统（Phase 5）。
 * 按企业->产品类型->文档类型三级存储基线数据。
 * 每个项目终稿提交后自动更新基线。
 */
@Slf4j
@Service
public class EnterpriseBaselineService {

    private final DocLedgerMapper docLedgerMapper;

    // In-memory store (production should use DB)
    private final Map<String, Baseline> baselines = new ConcurrentHashMap<>();

    public EnterpriseBaselineService(DocLedgerMapper docLedgerMapper) {
        this.docLedgerMapper = docLedgerMapper;
    }

    /**
     * 获取或创建基线。
     */
    public Baseline getBaseline(String domain, String productType, String docType) {
        String key = domain + ":" + productType + ":" + docType;
        return baselines.computeIfAbsent(key, k -> {
            Baseline b = new Baseline();
            b.domain = domain;
            b.productType = productType;
            b.docType = docType;
            return b;
        });
    }

    /**
     * 从终稿差异分析更新基线。
     */
    public void updateFromDiff(Long projectId, DiffAnalysisService.DiffReport diff) {
        DocLedger doc = docLedgerMapper.selectById(diff.getLedgerId());
        if (doc == null) return;

        String domain = "ELECTRONIC"; // Default
        String productType = doc.getDocCategory() != null ? doc.getDocCategory() : "通用";
        String docType = doc.getDocType() != null ? doc.getDocType() : "unknown";

        Baseline baseline = getBaseline(domain, productType, docType);
        baseline.sampleCount++;

        // Track chapter count trend
        if (baseline.avgChapterCount == 0) {
            baseline.avgChapterCount = diff.getFinalChapterCount();
        } else {
            baseline.avgChapterCount = (baseline.avgChapterCount * (baseline.sampleCount - 1)
                + diff.getFinalChapterCount()) / baseline.sampleCount;
        }

        // Track warning resolution rate
        double resolved = diff.getWarningsResolved();
        baseline.totalWarningsResolved += resolved;

        log.info("Baseline updated: {}:{}:{} (samples={})", domain, productType, docType, baseline.sampleCount);
    }

    /**
     * 获取所有基线摘要。
     */
    public Map<String, Object> getBaselineSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        for (var entry : baselines.entrySet()) {
            Baseline b = entry.getValue();
            if (b.sampleCount >= 3) { // Only show mature baselines
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("samples", b.sampleCount);
                info.put("avgChapters", Math.round(b.avgChapterCount));
                info.put("warningsResolved", b.totalWarningsResolved);
                summary.put(entry.getKey(), info);
            }
        }
        return summary;
    }

    @Data
    public static class Baseline {
        String domain;
        String productType;
        String docType;
        int sampleCount;
        double avgChapterCount;
        double totalWarningsResolved;
    }
}
