package com.military.doc.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.entity.ProjectDocChecklist;
import com.military.doc.modules.document.entity.StageDocChecklistTemplate;
import com.military.doc.modules.document.mapper.DocChapterMapper;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import com.military.doc.modules.document.mapper.ProjectDocChecklistMapper;
import com.military.doc.modules.document.mapper.StageDocChecklistTemplateMapper;
import com.military.doc.modules.project.entity.ProjectMasterData;
import com.military.doc.modules.project.mapper.ProjectMasterDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 主数据完整性看板服务。
 * 小企业最核心的痛点：从输入文件中提取的字段是否完整、哪些文档缺数据、去哪里补。
 */
@Slf4j
@Service
public class CompletenessDashboardService {

    private final ProjectMasterDataMapper masterDataMapper;
    private final DocLedgerMapper docLedgerMapper;
    private final DocChapterMapper docChapterMapper;
    private final ProjectDocChecklistMapper checklistMapper;
    private final StageDocChecklistTemplateMapper templateMapper;
    private final ObjectMapper objectMapper;

    private static final List<String> MASTER_DATA_SECTIONS = List.of(
        "equipmentInfo", "tacticalIndicators", "productTree", "milestones", "extendedFields");

    public CompletenessDashboardService(ProjectMasterDataMapper masterDataMapper,
                                        DocLedgerMapper docLedgerMapper,
                                        DocChapterMapper docChapterMapper,
                                        ProjectDocChecklistMapper checklistMapper,
                                        StageDocChecklistTemplateMapper templateMapper,
                                        ObjectMapper objectMapper) {
        this.masterDataMapper = masterDataMapper;
        this.docLedgerMapper = docLedgerMapper;
        this.docChapterMapper = docChapterMapper;
        this.checklistMapper = checklistMapper;
        this.templateMapper = templateMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * Build a full completeness report for a project stage.
     */
    public Map<String, Object> buildDashboard(Long projectId, Long stageId) {
        // 1. Master data completeness
        Map<String, Object> masterData = analyzeMasterData(projectId);

        // 2. Document-level completeness (how many XXX placeholders per doc)
        List<Map<String, Object>> docCompleteness = analyzeDocumentCompleteness(projectId, stageId);

        // 3. Stage overview (checklist vs ledger sync)
        Map<String, Object> stageOverview = buildStageOverview(projectId, stageId);

        // 4. Action items (what to fix)
        List<Map<String, Object>> actionItems = buildActionItems(masterData, docCompleteness, projectId);

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("masterData", masterData);
        dashboard.put("documents", docCompleteness);
        dashboard.put("stageOverview", stageOverview);
        dashboard.put("actionItems", actionItems);
        dashboard.put("summary", buildSummary(masterData, docCompleteness, stageOverview));
        return dashboard;
    }

    // ---- Master Data Analysis ----

    Map<String, Object> analyzeMasterData(Long projectId) {
        ProjectMasterData md = masterDataMapper.selectOne(
            new LambdaQueryWrapper<ProjectMasterData>()
                .eq(ProjectMasterData::getProjectId, projectId)
                .orderByDesc(ProjectMasterData::getId)
                .last("LIMIT 1"));

        int totalFields = 0;
        int filledFields = 0;
        int xxxFields = 0;
        List<Map<String, Object>> sections = new ArrayList<>();

        if (md != null) {
            for (String sectionName : MASTER_DATA_SECTIONS) {
                String json = getFieldValue(md, sectionName);
                List<Map<String, Object>> fields = parseFieldList(json);
                totalFields += fields.size();
                long sectionFilled = fields.stream().filter(f -> !isPlaceholder(f)).count();
                long sectionXxx = fields.stream().filter(this::isPlaceholder).count();
                filledFields += (int) sectionFilled;
                xxxFields += (int) sectionXxx;

                Map<String, Object> section = new LinkedHashMap<>();
                section.put("section", sectionName);
                section.put("label", sectionLabel(sectionName));
                section.put("totalFields", fields.size());
                section.put("filledFields", (int) sectionFilled);
                section.put("xxxFields", (int) sectionXxx);
                section.put("fillRate", fields.isEmpty() ? 0 :
                    Math.round(100.0 * sectionFilled / fields.size()));
                section.put("xxxDetails", fields.stream()
                    .filter(this::isPlaceholder)
                    .map(f -> f.get("key"))
                    .limit(20).toList());
                sections.add(section);
            }
        } else {
            for (String sectionName : MASTER_DATA_SECTIONS) {
                Map<String, Object> section = new LinkedHashMap<>();
                section.put("section", sectionName);
                section.put("label", sectionLabel(sectionName));
                section.put("totalFields", 0);
                section.put("filledFields", 0);
                section.put("xxxFields", 0);
                section.put("fillRate", 0);
                section.put("xxxDetails", List.of());
                sections.add(section);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("exists", md != null);
        result.put("totalFields", totalFields);
        result.put("filledFields", filledFields);
        result.put("xxxFields", xxxFields);
        result.put("fillRate", totalFields > 0 ? Math.round(100.0 * filledFields / totalFields) : 0);
        result.put("sections", sections);
        return result;
    }

    // ---- Document Completeness ----

    List<Map<String, Object>> analyzeDocumentCompleteness(Long projectId, Long stageId) {
        List<DocLedger> ledgers = docLedgerMapper.selectList(
            new LambdaQueryWrapper<DocLedger>()
                .eq(DocLedger::getProjectId, projectId)
                .eq(DocLedger::getStageId, stageId));

        List<Map<String, Object>> docs = new ArrayList<>();
        for (DocLedger ledger : ledgers) {
            List<DocChapter> chapters = docChapterMapper.selectList(
                new LambdaQueryWrapper<DocChapter>()
                    .eq(DocChapter::getDocLedgerId, ledger.getId())
                    .eq(DocChapter::getDeleted, 0));

            int totalChapters = chapters.size();
            int chaptersWithContent = 0;
            int xxxCount = 0;
            List<String> xxxChapters = new ArrayList<>();

            for (DocChapter ch : chapters) {
                String content = ch.getContent();
                if (content != null && !content.isBlank()) {
                    chaptersWithContent++;
                    int cnt = countXxx(content);
                    xxxCount += cnt;
                    if (cnt > 0) {
                        xxxChapters.add(ch.getChapterNumber() + " " + ch.getChapterTitle());
                    }
                }
            }

            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("docLedgerId", ledger.getId());
            doc.put("docName", ledger.getDocName());
            doc.put("docCode", ledger.getDocCode());
            doc.put("lifecycleStatus", ledger.getLifecycleStatus());
            doc.put("totalChapters", totalChapters);
            doc.put("chaptersWithContent", chaptersWithContent);
            doc.put("xxxCount", xxxCount);
            doc.put("xxxChapters", xxxChapters);
            doc.put("contentFillRate", totalChapters > 0 ?
                Math.round(100.0 * chaptersWithContent / totalChapters) : 0);
            docs.add(doc);
        }

        return docs;
    }

    // ---- Stage Overview ----

    Map<String, Object> buildStageOverview(Long projectId, Long stageId) {
        List<ProjectDocChecklist> checklist = checklistMapper.selectList(
            new LambdaQueryWrapper<ProjectDocChecklist>()
                .eq(ProjectDocChecklist::getProjectId, projectId)
                .eq(ProjectDocChecklist::getStageId, stageId));

        List<DocLedger> ledgers = docLedgerMapper.selectList(
            new LambdaQueryWrapper<DocLedger>()
                .eq(DocLedger::getProjectId, projectId)
                .eq(DocLedger::getStageId, stageId));

        int totalChecklist = checklist.size();
        int syncedToLedger = (int) checklist.stream()
            .filter(c -> ledgers.stream().anyMatch(l ->
                l.getDocName() != null && l.getDocName().equals(c.getDocName())))
            .count();

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("checklistCount", totalChecklist);
        overview.put("ledgerCount", ledgers.size());
        overview.put("syncedCount", syncedToLedger);
        overview.put("syncRate", totalChecklist > 0 ?
            Math.round(100.0 * syncedToLedger / totalChecklist) : 0);
        return overview;
    }

    // ---- Action Items ----

    List<Map<String, Object>> buildActionItems(Map<String, Object> masterData,
                                                List<Map<String, Object>> docCompleteness,
                                                Long projectId) {
        List<Map<String, Object>> items = new ArrayList<>();

        // Action 1: Master data missing
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sections = (List<Map<String, Object>>) masterData.get("sections");
        long totalXxx = sections.stream().mapToLong(s ->
            ((Number) s.get("xxxFields")).longValue()).sum();
        if (totalXxx > 0) {
            items.add(Map.of(
                "priority", "HIGH",
                "type", "FILL_MASTER_DATA",
                "message", String.format("主数据中有 %d 个字段为 XXX 占位符", totalXxx),
                "action", "请补充项目主数据中的装备信息、战术指标、产品树等字段",
                "link", "/projects/" + projectId + "/master-data"));
        }

        // Action 2: Documents with too many XXX
        List<Map<String, Object>> highXxxDocs = docCompleteness.stream()
            .filter(d -> ((Number) d.get("xxxCount")).intValue() > 5)
            .toList();
        if (!highXxxDocs.isEmpty()) {
            items.add(Map.of(
                "priority", "MEDIUM",
                "type", "REDUCE_XXX",
                "message", String.format("%d 个文档中存在大量 XXX 占位符", highXxxDocs.size()),
                "action", "建议重新生成以下文档或补充主数据后重新生成",
                "details", highXxxDocs.stream().map(d -> d.get("docName")).limit(5).toList()));
        }

        // Action 3: Empty chapters
        List<Map<String, Object>> emptyDocs = docCompleteness.stream()
            .filter(d -> ((Number) d.get("contentFillRate")).intValue() == 0)
            .toList();
        if (!emptyDocs.isEmpty()) {
            items.add(Map.of(
                "priority", "HIGH",
                "type", "EMPTY_DOCUMENTS",
                "message", String.format("%d 个文档章节内容完全为空", emptyDocs.size()),
                "action", "请使用 AI 批量生成功能生成文档初稿",
                "details", emptyDocs.stream().map(d -> d.get("docName")).limit(10).toList()));
        }

        return items;
    }

    // ---- Summary ----

    Map<String, Object> buildSummary(Map<String, Object> masterData,
                                      List<Map<String, Object>> docCompleteness,
                                      Map<String, Object> stageOverview) {
        int masterFillRate = ((Number) masterData.get("fillRate")).intValue();
        long totalXxx = docCompleteness.stream().mapToLong(d -> ((Number) d.get("xxxCount")).intValue()).sum();
        int totalDocs = docCompleteness.size();
        long docsWithContent = docCompleteness.stream().filter(d -> ((Number) d.get("contentFillRate")).intValue() > 0).count();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("masterDataFillRate", masterFillRate);
        summary.put("totalXxxPlaceholders", totalXxx);
        summary.put("totalDocuments", totalDocs);
        summary.put("documentsWithContent", docsWithContent);
        summary.put("overallHealth", masterFillRate >= 60 && totalXxx < 50 ? "good" :
            masterFillRate >= 30 ? "fair" : "poor");
        return summary;
    }

    // ---- Helpers ----

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseFieldList(String json) {
        if (json == null || json.isBlank() || "{}".equals(json.trim()) || "[]".equals(json.trim())) {
            return List.of();
        }
        try {
            if (json.trim().startsWith("{")) {
                // Object → flatten key-value pairs
                Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
                return map.entrySet().stream()
                    .map(e -> (Map<String, Object>) new LinkedHashMap<String, Object>() {{
                        put("key", e.getKey());
                        put("value", e.getValue());
                    }})
                    .collect(Collectors.toList());
            }
            // Array of objects with "key" and "value"
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private boolean isPlaceholder(Map<String, Object> field) {
        Object val = field.get("value");
        if (val == null) return true;
        String s = val.toString();
        return s.isBlank() || "XXX".equals(s.trim()) || s.contains("XXX");
    }

    private int countXxx(String content) {
        if (content == null) return 0;
        int count = 0;
        int idx = 0;
        while ((idx = content.indexOf("XXX", idx)) != -1) {
            count++;
            idx += 3;
        }
        return count;
    }

    private String sectionLabel(String sectionName) {
        return switch (sectionName) {
            case "equipmentInfo" -> "装备信息";
            case "tacticalIndicators" -> "战术技术指标";
            case "productTree" -> "产品树";
            case "teamMembers" -> "团队成员";
            case "milestones" -> "里程碑";
            case "extendedFields" -> "扩展字段";
            default -> sectionName;
        };
    }

    private String getFieldValue(ProjectMasterData md, String fieldName) {
        return switch (fieldName) {
            case "equipmentInfo" -> md.getEquipmentInfo();
            case "tacticalIndicators" -> md.getTacticalIndicators();
            case "productTree" -> md.getProductTree();
            case "teamMembers" -> md.getTeamMembers();
            case "milestones" -> md.getMilestones();
            case "extendedFields" -> md.getExtendedFields();
            default -> null;
        };
    }
}
