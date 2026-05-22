package com.military.doc.modules.document.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.modules.document.entity.CompletenessCheckResult;
import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.mapper.CompletenessCheckResultMapper;
import com.military.doc.modules.document.mapper.DocChapterMapper;
import com.military.doc.modules.template.entity.DocTemplateChapter;
import com.military.doc.modules.template.entity.DocTemplateChapterElement;
import com.military.doc.modules.template.entity.DocTemplateElement;
import com.military.doc.modules.template.mapper.DocTemplateChapterElementMapper;
import com.military.doc.modules.template.mapper.DocTemplateChapterMapper;
import com.military.doc.modules.template.mapper.DocTemplateElementMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CompletenessCheckService {

    @Autowired private DocChapterMapper docChapterMapper;
    @Autowired private DocTemplateChapterMapper templateChapterMapper;
    @Autowired private DocTemplateChapterElementMapper chapterElementMapper;
    @Autowired private DocTemplateElementMapper elementMapper;
    @Autowired private CompletenessCheckResultMapper checkResultMapper;
    @Autowired private ObjectMapper objectMapper;

    @Transactional
    public CompletenessCheckResult checkDocument(Long projectId, Long docLedgerId, Long operatorId) {
        List<DocChapter> chapters = docChapterMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocChapter>()
                        .eq(DocChapter::getDocLedgerId, docLedgerId)
                        .orderByAsc(DocChapter::getOrderNum));

        Map<Long, DocChapter> chapterMap = new HashMap<>();
        for (DocChapter dc : chapters) {
            if (dc.getTemplateChapterId() != null) {
                chapterMap.put(dc.getTemplateChapterId(), dc);
            }
        }

        // Get all required template chapters
        List<DocTemplateChapter> templateChapters = new ArrayList<>();
        Long templateId = null;
        if (!chapters.isEmpty() && chapters.get(0).getTemplateChapterId() != null) {
            DocTemplateChapter first = templateChapterMapper.selectById(chapters.get(0).getTemplateChapterId());
            if (first != null) {
                templateId = first.getTemplateId();
                templateChapters = templateChapterMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocTemplateChapter>()
                                .eq(DocTemplateChapter::getTemplateId, templateId)
                                .eq(DocTemplateChapter::getIsRequired, true)
                                .orderByAsc(DocTemplateChapter::getOrderNum));
            }
        }

        List<Map<String, Object>> details = new ArrayList<>();
        int totalItems = 0, passedItems = 0, warningItems = 0, errorItems = 0;

        for (DocTemplateChapter tc : templateChapters) {
            totalItems++;
            DocChapter dc = chapterMap.get(tc.getId());

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("chapterTitle", tc.getChapterTitle());
            item.put("chapterNumber", tc.getChapterNumber());
            item.put("standardRef", tc.getStandardClauseRef());

            if (dc == null) {
                item.put("severity", "ERROR");
                item.put("description", "缺少必填章节：未从模板初始化");
                item.put("tip", tc.getWritingTips());
                errorItems++;
                details.add(item);
                continue;
            }

            if ("EMPTY".equals(dc.getFillStatus())) {
                item.put("severity", "ERROR");
                item.put("description", "必填章节内容为空：" + tc.getChapterTitle());
                item.put("tip", tc.getWritingTips() != null ? tc.getWritingTips() : "请根据GJB要求填写本章节内容");
                errorItems++;
            } else if ("PARTIAL".equals(dc.getFillStatus())) {
                item.put("severity", "WARNING");
                item.put("description", "章节部分填充：完成度" + dc.getFillPercentage() + "%");
                int missingCount = checkContentSchemaFields(tc, dc);
                item.put("missingFields", missingCount);
                warningItems++;
            } else {
                passedItems++;
                item.put("severity", "PASS");
                item.put("description", "已填写");
            }

            // Check required elements
            List<DocTemplateChapterElement> rels = chapterElementMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocTemplateChapterElement>()
                            .eq(DocTemplateChapterElement::getChapterId, tc.getId())
                            .eq(DocTemplateChapterElement::getIsRequired, true));

            for (DocTemplateChapterElement rel : rels) {
                totalItems++;
                DocTemplateElement element = elementMapper.selectById(rel.getElementId());
                if (element != null) {
                    boolean found = dc != null && dc.getContent() != null &&
                            dc.getContent().contains(element.getElementName());
                    if (!found) {
                        warningItems++;
                        Map<String, Object> elemItem = new LinkedHashMap<>();
                        elemItem.put("severity", "WARNING");
                        elemItem.put("description", "缺少必要元素: " + element.getElementName());
                        elemItem.put("standardRef", element.getStandardRefs());
                        details.add(elemItem);
                    } else {
                        passedItems++;
                    }
                }
            }

            details.add(item);
        }

        BigDecimal score = totalItems > 0
                ? BigDecimal.valueOf((double) passedItems / totalItems * 100).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        CompletenessCheckResult result = new CompletenessCheckResult();
        result.setProjectId(projectId);
        result.setDocLedgerId(docLedgerId);
        result.setCheckType("CHAPTER");
        result.setTotalItems(totalItems);
        result.setPassedItems(passedItems);
        result.setWarningItems(warningItems);
        result.setErrorItems(errorItems);
        result.setScore(score);
        try {
            result.setDetailJson(objectMapper.writeValueAsString(details));
        } catch (JsonProcessingException e) {
            result.setDetailJson("[]");
        }
        result.setCheckedBy(operatorId);
        result.setCheckedAt(LocalDateTime.now());

        checkResultMapper.insert(result);
        return result;
    }

    public List<CompletenessCheckResult> getHistory(Long docLedgerId) {
        return checkResultMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CompletenessCheckResult>()
                        .eq(CompletenessCheckResult::getDocLedgerId, docLedgerId)
                        .orderByDesc(CompletenessCheckResult::getCheckedAt));
    }

    public Map<String, Object> getProjectSummary(Long projectId) {
        List<CompletenessCheckResult> results = checkResultMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CompletenessCheckResult>()
                        .eq(CompletenessCheckResult::getProjectId, projectId)
                        .orderByDesc(CompletenessCheckResult::getCheckedAt));

        if (results.isEmpty()) return Map.of("totalChecks", 0, "avgScore", 0);

        double avgScore = results.stream()
                .mapToDouble(r -> r.getScore() != null ? r.getScore().doubleValue() : 0)
                .average().orElse(0);

        int totalErrors = results.stream()
                .mapToInt(r -> r.getErrorItems() != null ? r.getErrorItems() : 0).sum();
        int totalWarnings = results.stream()
                .mapToInt(r -> r.getWarningItems() != null ? r.getWarningItems() : 0).sum();

        return Map.of(
                "totalChecks", results.size(),
                "avgScore", Math.round(avgScore * 100.0) / 100.0,
                "totalErrors", totalErrors,
                "totalWarnings", totalWarnings
        );
    }

    private int checkContentSchemaFields(DocTemplateChapter tc, DocChapter dc) {
        String schema = tc.getContentSchema();
        if (schema == null || schema.isBlank()) return 0;
        try {
            List<?> fields = objectMapper.readValue(schema, List.class);
            int missing = 0;
            for (Object f : fields) {
                if (f instanceof Map<?,?> m && Boolean.TRUE.equals(m.get("required"))) {
                    String fieldName = (String) m.get("fieldName");
                    if (dc.getContentJson() != null && !dc.getContentJson().contains(fieldName)) {
                        missing++;
                    }
                }
            }
            return missing;
        } catch (Exception e) {
            return 0;
        }
    }
}
