package com.military.doc.modules.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.common.exception.BusinessException;
import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.mapper.DocChapterMapper;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import com.military.doc.modules.document.service.DocChapterService;
import com.military.doc.modules.template.entity.DocTemplateChapter;
import com.military.doc.modules.template.mapper.DocTemplateChapterMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DocChapterServiceImpl extends ServiceImpl<DocChapterMapper, DocChapter> implements DocChapterService {

    @Autowired private DocTemplateChapterMapper templateChapterMapper;
    @Autowired private DocLedgerMapper docLedgerMapper;
    @Autowired private ObjectMapper objectMapper;

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
            "total", total,
            "filled", filled,
            "partial", partial,
            "empty", empty,
            "score", score
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
            node.put("chapterTitle", dc.getChapterTitle());
            node.put("chapterLevel", dc.getChapterLevel());
            node.put("orderNum", dc.getOrderNum());
            node.put("fillStatus", dc.getFillStatus());
            node.put("fillPercentage", dc.getFillPercentage());
            node.put("isRequired", true);
            node.put("content", dc.getContent());
            List<Map<String, Object>> subChildren = buildTreeNodes(childrenMap, dc.getId());
            if (!subChildren.isEmpty()) node.put("children", subChildren);
            result.add(node);
        }
        return result;
    }

    @Override
    @Transactional
    public ChapterStructureValidation fixStructure(Long docLedgerId) {
        List<DocChapter> chapters = listByDocLedger(docLedgerId);
        if (chapters.isEmpty()) {
            return new ChapterStructureValidation(true, 0, 0, List.of(), "无章节，无需修复");
        }

        // Find the template associated with this doc ledger
        DocLedger ledger = docLedgerMapper.selectById(docLedgerId);
        Long templateId = findTemplateForLedger(ledger);

        if (templateId != null) {
            // Template-based fix: match chapters to template chapters by title
            fixByTemplate(chapters, templateId);
        } else {
            // Inference-based fix: normalize chapter numbers from title patterns
            fixByInference(chapters);
        }

        // Re-validate after fix
        return validateStructure(docLedgerId);
    }

    private Long findTemplateForLedger(DocLedger ledger) {
        if (ledger == null) return null;
        // Find the first chapter that has a templateChapterId
        List<DocChapter> chapters = listByDocLedger(ledger.getId());
        for (DocChapter ch : chapters) {
            if (ch.getTemplateChapterId() != null) {
                DocTemplateChapter tplCh = templateChapterMapper.selectById(ch.getTemplateChapterId());
                if (tplCh != null) return tplCh.getTemplateId();
            }
        }
        return null;
    }

    private void fixByTemplate(List<DocChapter> chapters, Long templateId) {
        List<DocTemplateChapter> tplChapters = templateChapterMapper.selectList(
            new LambdaQueryWrapper<DocTemplateChapter>()
                .eq(DocTemplateChapter::getTemplateId, templateId)
                .orderByAsc(DocTemplateChapter::getOrderNum));

        // Build template chapter map: title → template chapter
        Map<String, DocTemplateChapter> tplByTitle = new java.util.LinkedHashMap<>();
        for (DocTemplateChapter tc : tplChapters) {
            tplByTitle.put(normalizeTitle(tc.getChapterTitle()), tc);
        }

        // Build id map for old chapters
        Map<Long, DocChapter> oldById = new HashMap<>();
        for (DocChapter ch : chapters) oldById.put(ch.getId(), ch);

        // Match each doc chapter to the best template chapter by title
        Map<Long, DocTemplateChapter> matchMap = new HashMap<>(); // docChapterId → tplChapter
        for (DocChapter ch : chapters) {
            String chTitle = normalizeTitle(ch.getChapterTitle());
            // Try exact match first
            DocTemplateChapter matched = tplByTitle.get(chTitle);
            if (matched == null) {
                // Try substring match
                for (var entry : tplByTitle.entrySet()) {
                    if (chTitle.contains(entry.getKey()) || entry.getKey().contains(chTitle)) {
                        matched = entry.getValue();
                        break;
                    }
                }
            }
            if (matched != null) {
                matchMap.put(ch.getId(), matched);
            }
        }

        // Apply fixes from template
        // Build template parent-child map to resolve doc chapter parent relationships
        Map<Long, Long> tplToNewId = new HashMap<>();  // tplChapterId → docChapterId
        for (var entry : matchMap.entrySet()) {
            tplToNewId.put(entry.getValue().getId(), entry.getKey());
        }

        for (var entry : matchMap.entrySet()) {
            DocChapter ch = oldById.get(entry.getKey());
            DocTemplateChapter tc = entry.getValue();
            if (ch == null) continue;

            // Fix number and title
            ch.setChapterNumber(tc.getChapterNumber());
            ch.setChapterTitle(tc.getChapterTitle());
            ch.setChapterLevel(tc.getChapterLevel());
            ch.setOrderNum(tc.getOrderNum());
            ch.setTemplateChapterId(tc.getId());

            // Fix parent: resolve template parent → doc chapter parent
            if (tc.getParentId() != null && tc.getParentId() > 0) {
                Long newParentId = tplToNewId.get(tc.getParentId());
                if (newParentId != null) {
                    ch.setParentId(newParentId);
                }
            } else {
                ch.setParentId(0L);
            }

            baseMapper.updateById(ch);
        }

        log.info("Fixed {} chapters by template {} for ledger {}",
            matchMap.size(), templateId, chapters.get(0).getDocLedgerId());
    }

    private void fixByInference(List<DocChapter> chapters) {
        // Sort by current order
        chapters.sort(Comparator.comparing(c -> c.getOrderNum() != null ? c.getOrderNum() : 0));

        // Step 1: Try to extract proper chapter numbers from titles
        // Pattern: "3.1 标题" → number="3.1", "第3章" → number="3"
        Pattern numInTitle = Pattern.compile("^第?([0-9]+(?:\\.[0-9]+)*)[章\\s、.]*");
        for (DocChapter ch : chapters) {
            String num = ch.getChapterNumber();
            // If current number looks corrupted (single digit but title suggests deeper), try title
            if (num == null || num.length() <= 2) {
                String title = ch.getChapterTitle();
                if (title != null) {
                    Matcher m = numInTitle.matcher(title.trim());
                    if (m.find()) {
                        String extracted = m.group(1);
                        if (extracted.length() > num.length()) {
                            num = extracted;
                            ch.setChapterNumber(num);
                        }
                    }
                }
            }
        }

        // Step 2: Rebuild hierarchy from chapter numbers (e.g. "3.1" → child of "3")
        Map<String, DocChapter> byNumber = new java.util.LinkedHashMap<>();
        for (DocChapter ch : chapters) {
            String num = ch.getChapterNumber();
            if (num != null && !num.isBlank()) {
                byNumber.put(num.trim(), ch);
            }
        }

        // Assign parent based on number prefix
        for (DocChapter ch : chapters) {
            String num = ch.getChapterNumber();
            if (num == null || !num.contains(".")) {
                ch.setParentId(0L);
                int depth = num != null ? num.split("\\.").length : 1;
                ch.setChapterLevel(depth);
            } else {
                String parentNum = num.substring(0, num.lastIndexOf('.'));
                DocChapter parent = byNumber.get(parentNum);
                if (parent != null && !parent.getId().equals(ch.getId())) {
                    ch.setParentId(parent.getId());
                } else {
                    ch.setParentId(0L);
                }
                ch.setChapterLevel(num.split("\\.").length);
            }
            baseMapper.updateById(ch);
        }

        // Step 3: Renumber sequentially within each level
        Map<Long, List<DocChapter>> byParent = new HashMap<>();
        for (DocChapter ch : chapters) {
            byParent.computeIfAbsent(ch.getParentId(), k -> new ArrayList<>()).add(ch);
        }

        List<DocChapter> roots = byParent.getOrDefault(0L, new ArrayList<>());
        roots.sort(Comparator.comparing(c -> {
            String n = c.getChapterNumber();
            try { return Integer.parseInt(n.contains(".") ? n.substring(0, n.indexOf('.')) : n); }
            catch (Exception e) { return 0; }
        }));

        for (int i = 0; i < roots.size(); i++) {
            String newNum = String.valueOf(i + 1);
            updateNumber(roots.get(i), newNum, byParent);
        }
    }

    /** Recursively update chapter numbers to be sequential. */
    private void updateNumber(DocChapter ch, String newNumber, Map<Long, List<DocChapter>> byParent) {
        String oldNumber = ch.getChapterNumber();
        ch.setChapterNumber(newNumber);
        ch.setChapterLevel(newNumber.split("\\.").length);
        baseMapper.updateById(ch);

        List<DocChapter> children = byParent.getOrDefault(ch.getId(), List.of());
        children.sort(Comparator.comparing(c -> {
            String n = c.getChapterNumber();
            if (n == null) return 0;
            String[] parts = n.split("\\.");
            try { return Integer.parseInt(parts[parts.length - 1]); }
            catch (Exception e) { return 0; }
        }));

        for (int i = 0; i < children.size(); i++) {
            updateNumber(children.get(i), newNumber + "." + (i + 1), byParent);
        }
    }

    private void fixChildrenNumbering(DocChapter parent, Map<Long, List<DocChapter>> byParent,
                                       String parentPrefix, int[] counter) {
        List<DocChapter> children = byParent.get(parent.getId());
        if (children == null || children.isEmpty()) return;
        children.sort(Comparator.comparing(c -> c.getOrderNum() != null ? c.getOrderNum() : 0));
        for (int i = 0; i < children.size(); i++) {
            DocChapter child = children.get(i);
            child.setChapterNumber(parentPrefix + "." + (i + 1));
            child.setChapterLevel(parent.getChapterLevel() + 1);
            baseMapper.updateById(child);
            fixChildrenNumbering(child, byParent, child.getChapterNumber(), counter);
        }
    }

    private String normalizeTitle(String title) {
        if (title == null) return "";
        return title.replaceAll("[\\s\\p{Punct}0-9]", "").trim().toLowerCase();
    }

    @Override
    public ChapterStructureValidation validateStructure(Long docLedgerId) {
        List<DocChapter> chapters = listByDocLedger(docLedgerId);
        if (chapters.isEmpty()) {
            return new ChapterStructureValidation(true, 0, 0, List.of(), "文档暂无章节，无需校验");
        }

        List<StructureIssue> issues = new ArrayList<>();
        Map<Long, DocChapter> byId = new HashMap<>();
        Map<String, List<DocChapter>> byNumber = new java.util.LinkedHashMap<>();
        Map<Long, List<DocChapter>> byParent = new HashMap<>();

        for (DocChapter ch : chapters) {
            byId.put(ch.getId(), ch);
            String num = ch.getChapterNumber();
            if (num != null) {
                byNumber.computeIfAbsent(num.trim(), k -> new ArrayList<>()).add(ch);
            }
            byParent.computeIfAbsent(ch.getParentId(), k -> new ArrayList<>()).add(ch);
        }

        // 1. Check for duplicate chapter numbers at the same level
        for (var entry : byNumber.entrySet()) {
            if (entry.getValue().size() > 1) {
                // Check if they're siblings (same parent)
                Map<Long, List<DocChapter>> byPar = new HashMap<>();
                for (DocChapter ch : entry.getValue()) {
                    byPar.computeIfAbsent(ch.getParentId(), k -> new ArrayList<>()).add(ch);
                }
                for (var parEntry : byPar.entrySet()) {
                    if (parEntry.getValue().size() > 1) {
                        issues.add(new StructureIssue("ERROR",
                            entry.getKey(),
                            "重复的章节编号（同一父节点下出现" + parEntry.getValue().size() + "次）"));
                    }
                }
            }
        }

        // 2. Check chapter level vs numbering depth
        for (DocChapter ch : chapters) {
            String num = ch.getChapterNumber();
            if (num == null || num.isBlank()) continue;
            int dotCount = num.split("\\.").length;
            Integer level = ch.getChapterLevel();
            if (level != null && Math.abs(level - dotCount) > 1) {
                issues.add(new StructureIssue("WARNING",
                    num + " " + ch.getChapterTitle(),
                    "章节层级(" + level + ")与编号深度(" + dotCount + ")不匹配"));
            }
        }

        // 3. Check parent-child numbering consistency
        for (DocChapter ch : chapters) {
            if (ch.getParentId() == null || ch.getParentId() == 0) continue;
            DocChapter parent = byId.get(ch.getParentId());
            if (parent == null) {
                issues.add(new StructureIssue("ERROR",
                    ch.getChapterNumber() + " " + ch.getChapterTitle(),
                    "父章节不存在（parentId=" + ch.getParentId() + "）"));
                continue;
            }
            // Child number should start with parent number
            String parentNum = parent.getChapterNumber();
            String childNum = ch.getChapterNumber();
            if (parentNum != null && childNum != null && !childNum.startsWith(parentNum + ".")) {
                issues.add(new StructureIssue("ERROR",
                    childNum + " " + ch.getChapterTitle(),
                    "子章节编号(" + childNum + ")应以上级编号(" + parentNum + ")为前缀"));
            }
        }

        // 4. Check for required chapters that are empty
        for (DocChapter ch : chapters) {
            String json = ch.getContentJson();
            boolean isRequired = false;
            if (json != null && !json.isEmpty() && !"null".equals(json)) {
                try {
                    Map<String, Object> meta = objectMapper.readValue(json, Map.class);
                    isRequired = Boolean.TRUE.equals(meta.get("isRequired"));
                } catch (Exception ignored) {}
            }
            if (isRequired && (ch.getContent() == null || ch.getContent().isBlank())) {
                issues.add(new StructureIssue("WARNING",
                    ch.getChapterNumber() + " " + ch.getChapterTitle(),
                    "必填章节尚未填写内容"));
            }
        }

        // 5. Check sequential numbering at each level
        List<DocChapter> roots = byParent.getOrDefault(0L, List.of());
        roots.sort(Comparator.comparing(DocChapter::getOrderNum));
        checkSequentialNumbering(roots, issues, byParent);

        boolean valid = issues.stream().noneMatch(i -> "ERROR".equals(i.level()));
        String summary = valid
            ? "章节结构校验通过（" + chapters.size() + "章，" + issues.size() + "个提示）"
            : "章节结构存在问题：" + issues.stream().filter(i -> "ERROR".equals(i.level())).count() + "个错误，"
                + issues.stream().filter(i -> "WARNING".equals(i.level())).count() + "个警告";

        return new ChapterStructureValidation(valid, chapters.size(), issues.size(), issues, summary);
    }

    private void checkSequentialNumbering(List<DocChapter> siblings, List<StructureIssue> issues,
                                           Map<Long, List<DocChapter>> byParent) {
        if (siblings.isEmpty()) return;
        siblings.sort(Comparator.comparing(c -> {
            String n = c.getChapterNumber();
            if (n == null) return "";
            // Parse the last numeric segment for comparison
            String[] parts = n.split("\\.");
            try {
                return String.format("%04d", Integer.parseInt(parts[parts.length - 1]));
            } catch (NumberFormatException e) {
                return n;
            }
        }));

        // Check for gaps in the last segment
        for (int i = 1; i < siblings.size(); i++) {
            String prev = siblings.get(i - 1).getChapterNumber();
            String curr = siblings.get(i).getChapterNumber();
            if (prev == null || curr == null) continue;
            String[] prevParts = prev.split("\\.");
            String[] currParts = curr.split("\\.");
            if (prevParts.length != currParts.length) continue;
            try {
                int prevLast = Integer.parseInt(prevParts[prevParts.length - 1]);
                int currLast = Integer.parseInt(currParts[currParts.length - 1]);
                if (currLast != prevLast + 1) {
                    issues.add(new StructureIssue("WARNING",
                        curr + " " + siblings.get(i).getChapterTitle(),
                        "编号跳序：期望" + (prevLast + 1) + "，实际" + currLast));
                }
            } catch (NumberFormatException ignored) {}
        }

        // Recursively check children
        for (DocChapter ch : siblings) {
            List<DocChapter> children = byParent.getOrDefault(ch.getId(), List.of());
            if (!children.isEmpty()) {
                checkSequentialNumbering(children, issues, byParent);
            }
        }
    }
}
