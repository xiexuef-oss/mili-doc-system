package com.military.doc.modules.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.common.exception.BusinessException;
import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.mapper.DocChapterMapper;
import com.military.doc.modules.document.service.DocChapterService;
import com.military.doc.modules.template.entity.DocTemplateChapter;
import com.military.doc.modules.template.mapper.DocTemplateChapterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class DocChapterServiceImpl extends ServiceImpl<DocChapterMapper, DocChapter> implements DocChapterService {

    @Autowired private DocTemplateChapterMapper templateChapterMapper;

    @Override
    @Transactional
    public List<DocChapter> initFromTemplate(Long docLedgerId, Long templateId, Long operatorId) {
        List<DocTemplateChapter> templateChapters = templateChapterMapper.selectList(
                new LambdaQueryWrapper<DocTemplateChapter>()
                        .eq(DocTemplateChapter::getTemplateId, templateId)
                        .orderByAsc(DocTemplateChapter::getOrderNum));

        // Build template chapter map: templateChapterId -> templateChapter
        Map<Long, DocTemplateChapter> tcMap = new HashMap<>();
        for (DocTemplateChapter tc : templateChapters) {
            tcMap.put(tc.getId(), tc);
        }

        // First pass: create all doc chapters with parentId=0
        List<DocChapter> chapters = new ArrayList<>();
        for (DocTemplateChapter tc : templateChapters) {
            DocChapter dc = new DocChapter();
            dc.setDocLedgerId(docLedgerId);
            dc.setTemplateChapterId(tc.getId());
            dc.setChapterNumber(tc.getChapterNumber());
            dc.setChapterTitle(tc.getChapterTitle());
            dc.setChapterLevel(tc.getChapterLevel());
            dc.setOrderNum(tc.getOrderNum());
            dc.setFillStatus("EMPTY");
            dc.setFillPercentage(0);
            dc.setCreatedBy(operatorId);
            dc.setUpdatedBy(operatorId);
            dc.setParentId(0L);
            chapters.add(dc);
        }
        saveBatch(chapters);

        // Map template chapter ID -> new doc chapter ID (IDs are populated after saveBatch)
        Map<Long, Long> tcToDc = new HashMap<>();
        for (int i = 0; i < templateChapters.size(); i++) {
            tcToDc.put(templateChapters.get(i).getId(), chapters.get(i).getId());
        }

        // Second pass: resolve and update parent IDs
        List<DocChapter> needParentUpdate = new ArrayList<>();
        for (int i = 0; i < templateChapters.size(); i++) {
            DocTemplateChapter tc = templateChapters.get(i);
            if (tc.getParentId() != null && tc.getParentId() > 0) {
                Long newParentId = tcToDc.get(tc.getParentId());
                if (newParentId != null) {
                    chapters.get(i).setParentId(newParentId);
                    needParentUpdate.add(chapters.get(i));
                }
            }
        }
        if (!needParentUpdate.isEmpty()) {
            updateBatchById(needParentUpdate);
        }

        return chapters;
    }

    @Override
    public List<DocChapter> listByDocLedger(Long docLedgerId) {
        return baseMapper.selectList(new LambdaQueryWrapper<DocChapter>()
                .eq(DocChapter::getDocLedgerId, docLedgerId)
                .orderByAsc(DocChapter::getOrderNum));
    }

    @Override
    public List<Map<String, Object>> getChapterTree(Long docLedgerId) {
        List<DocChapter> all = listByDocLedger(docLedgerId);
        Map<Long, List<DocChapter>> childrenMap = new HashMap<>();
        for (DocChapter dc : all) {
            childrenMap.computeIfAbsent(dc.getParentId(), k -> new ArrayList<>()).add(dc);
        }
        return buildTreeNodes(childrenMap, 0L);
    }

    @Override
    @Transactional
    public DocChapter updateContent(Long chapterId, String content, String contentJson, Long operatorId) {
        DocChapter dc = baseMapper.selectById(chapterId);
        if (dc == null) throw BusinessException.notFound("章节不存在: " + chapterId);
        dc.setContent(content);
        dc.setContentJson(contentJson);
        dc.setUpdatedBy(operatorId);
        if (content != null && !content.isBlank()) {
            dc.setFillStatus("PARTIAL");
            dc.setFillPercentage(50);
        }
        baseMapper.updateById(dc);
        return dc;
    }

    @Override
    @Transactional
    public DocChapter updateFillStatus(Long chapterId, String fillStatus, Integer fillPercentage) {
        DocChapter dc = baseMapper.selectById(chapterId);
        if (dc == null) throw BusinessException.notFound("章节不存在: " + chapterId);
        dc.setFillStatus(fillStatus);
        dc.setFillPercentage(fillPercentage);
        baseMapper.updateById(dc);
        return dc;
    }

    @Override
    public Map<String, Object> getCompletionSummary(Long docLedgerId) {
        List<DocChapter> chapters = listByDocLedger(docLedgerId);
        int total = chapters.size();
        long filled = chapters.stream().filter(c -> "FILLED".equals(c.getFillStatus())).count();
        long partial = chapters.stream().filter(c -> "PARTIAL".equals(c.getFillStatus())).count();
        long empty = chapters.stream().filter(c -> "EMPTY".equals(c.getFillStatus())).count();
        double score = total > 0 ? Math.round((double) filled / total * 100.0) : 0;

        return Map.of(
            "totalChapters", total,
            "filledChapters", filled,
            "partialChapters", partial,
            "emptyChapters", empty,
            "completionScore", score
        );
    }

    @Override
    public Map<Long, Map<String, Object>> getCompletionSummaryBatch(List<Long> docLedgerIds) {
        if (docLedgerIds == null || docLedgerIds.isEmpty()) return Map.of();
        // Query all chapters for all docLedgerIds in one query
        List<DocChapter> allChapters = lambdaQuery().in(DocChapter::getDocLedgerId, docLedgerIds).list();
        // Group by docLedgerId
        Map<Long, List<DocChapter>> grouped = allChapters.stream()
            .collect(java.util.stream.Collectors.groupingBy(DocChapter::getDocLedgerId));

        Map<Long, Map<String, Object>> result = new java.util.LinkedHashMap<>();
        for (Long docLedgerId : docLedgerIds) {
            List<DocChapter> chapters = grouped.getOrDefault(docLedgerId, List.of());
            int total = chapters.size();
            long filled = chapters.stream().filter(c -> "FILLED".equals(c.getFillStatus())).count();
            long partial = chapters.stream().filter(c -> "PARTIAL".equals(c.getFillStatus())).count();
            long empty = chapters.stream().filter(c -> "EMPTY".equals(c.getFillStatus())).count();
            double score = total > 0 ? Math.round((double) filled / total * 100.0) : 0;
            result.put(docLedgerId, Map.of(
                "total", total,
                "filled", filled,
                "partial", partial,
                "empty", empty,
                "completionScore", score
            ));
        }
        return result;
    }

    @Override
    public DocChapter getById(Long chapterId) {
        DocChapter dc = baseMapper.selectById(chapterId);
        if (dc == null) throw BusinessException.notFound("章节不存在: " + chapterId);
        return dc;
    }

    private List<Map<String, Object>> buildTreeNodes(Map<Long, List<DocChapter>> childrenMap, Long parentId) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<DocChapter> children = childrenMap.getOrDefault(parentId, List.of());
        for (DocChapter dc : children) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", dc.getId());
            node.put("label", (dc.getChapterNumber() != null ? dc.getChapterNumber() + " " : "") + dc.getChapterTitle());
            node.put("chapterNumber", dc.getChapterNumber());
            node.put("fillStatus", dc.getFillStatus());
            node.put("fillPercentage", dc.getFillPercentage());
            node.put("isRequired", true);
            List<Map<String, Object>> subChildren = buildTreeNodes(childrenMap, dc.getId());
            if (!subChildren.isEmpty()) node.put("children", subChildren);
            result.add(node);
        }
        return result;
    }
}
