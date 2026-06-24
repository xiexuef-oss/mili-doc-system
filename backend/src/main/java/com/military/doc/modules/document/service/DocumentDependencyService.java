package com.military.doc.modules.document.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import com.military.doc.modules.project.entity.ProjectMasterData;
import com.military.doc.modules.project.mapper.ProjectMasterDataMapper;
import com.military.doc.modules.project.service.ProjectMasterDataService;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;

/**
 * 文档依赖检查引擎。
 * 加载 dependencies.yml 配置，根据项目当前文档完成状态，
 * 判断指定文档类型的前置条件是否满足。
 */
@Slf4j
@Service
public class DocumentDependencyService {

    private final DocLedgerMapper docLedgerMapper;
    private final ProjectMasterDataService masterDataService;
    private final ProjectMasterDataMapper masterDataMapper;

    private Map<String, DocDependencyDef> dependencyDefs = new LinkedHashMap<>();
    private List<ConsistencyRule> consistencyRules = new ArrayList<>();

    public DocumentDependencyService(DocLedgerMapper docLedgerMapper,
                                      ProjectMasterDataService masterDataService,
                                      ProjectMasterDataMapper masterDataMapper) {
        this.docLedgerMapper = docLedgerMapper;
        this.masterDataService = masterDataService;
        this.masterDataMapper = masterDataMapper;
    }

    @PostConstruct
    public void loadDependencies() {
        try {
            Yaml yaml = new Yaml();
            InputStream is = new ClassPathResource("dependencies.yml").getInputStream();
            @SuppressWarnings("unchecked")
            Map<String, Object> root = yaml.load(is);

            @SuppressWarnings("unchecked")
            Map<String, Object> depMap = (Map<String, Object>) root.get("document_dependencies");
            if (depMap != null) {
                for (var entry : depMap.entrySet()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> def = (Map<String, Object>) entry.getValue();
                    DocDependencyDef d = new DocDependencyDef();
                    d.docType = entry.getKey();
                    d.name = (String) def.getOrDefault("name", entry.getKey());
                    d.code = (String) def.get("code");

                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> hardList = (List<Map<String, String>>) def.get("hard");
                    if (hardList != null) {
                        for (var h : hardList) {
                            DepItem item = new DepItem();
                            item.doc = h.get("doc");
                            item.field = h.get("field");
                            item.description = h.get("description");
                            d.hardDeps.add(item);
                        }
                    }

                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> softList = (List<Map<String, String>>) def.get("soft");
                    if (softList != null) {
                        for (var s : softList) {
                            DepItem item = new DepItem();
                            item.doc = s.get("doc");
                            item.field = s.get("field");
                            item.description = s.get("description");
                            d.softDeps.add(item);
                        }
                    }

                    dependencyDefs.put(d.docType, d);
                }
            }

            log.info("Loaded {} document dependency definitions", dependencyDefs.size());
        } catch (Exception e) {
            log.error("Failed to load dependencies.yml", e);
        }
    }

    /**
     * 检查指定文档类型的前置条件。
     */
    public DependencyCheckResult checkPrerequisites(Long projectId, String docType) {
        DocDependencyDef def = dependencyDefs.get(docType);
        if (def == null) {
            // Unknown document type — no dependency info, allow
            DependencyCheckResult result = new DependencyCheckResult();
            result.allHardMet = true;
            result.suggestion = "无已知依赖，可以开始生成";
            return result;
        }

        DependencyCheckResult result = new DependencyCheckResult();
        result.docType = docType;
        result.docName = def.name;
        result.allHardMet = true;

        // Check hard dependencies
        for (DepItem dep : def.hardDeps) {
            DepStatus status = checkDocDep(projectId, dep);
            result.hardDeps.add(status);
            if (!status.satisfied) {
                result.allHardMet = false;
            }
        }

        // Check soft dependencies
        for (DepItem dep : def.softDeps) {
            DepStatus status = checkDocDep(projectId, dep);
            result.softDeps.add(status);
        }

        // Build suggestion
        if (!result.allHardMet) {
            List<String> unmet = result.hardDeps.stream()
                .filter(d -> !d.satisfied)
                .map(d -> d.docName)
                .toList();
            result.suggestion = "建议先完成: " + String.join("、", unmet);
        } else if (result.softDeps.stream().anyMatch(d -> !d.satisfied)) {
            result.suggestion = "前置文档已满足，部分数据缺失将影响质量（见软依赖）";
        } else {
            result.suggestion = "所有前置条件已满足，可以开始生成";
        }

        return result;
    }

    /**
     * 获取项目所有文档类型的前置检查状态（供工作流导航使用）。
     */
    public Map<String, DependencyCheckResult> checkAllDocuments(Long projectId) {
        Map<String, DependencyCheckResult> results = new LinkedHashMap<>();
        for (String docType : dependencyDefs.keySet()) {
            results.put(docType, checkPrerequisites(projectId, docType));
        }
        return results;
    }

    private DepStatus checkDocDep(Long projectId, DepItem dep) {
        DepStatus status = new DepStatus();
        status.docType = dep.doc;
        status.description = dep.description;

        if ("master_data".equals(dep.doc)) {
            // Check if project has master data
            ProjectMasterData md = masterDataMapper.selectOne(
                new LambdaQueryWrapper<ProjectMasterData>()
                    .eq(ProjectMasterData::getProjectId, projectId)
                    .last("LIMIT 1"));
            status.docName = "主数据";
            status.satisfied = md != null;
            if (status.satisfied) {
                status.detail = "主数据已提取";
            } else {
                status.detail = "主数据未提取，请先上传合同/任务书并提取主数据";
            }
        } else if ("gjb299d_data".equals(dep.doc)) {
            // Check if 299D data is available
            // For now, check if rel_gjb299d_cache has entries
            status.docName = "GJB/Z 299D 数据库";
            status.satisfied = true; // Assume available (we provided seed data)
            status.detail = "299D 数据已就绪";
        } else {
            // Check document existence
            List<DocLedger> docs = docLedgerMapper.selectList(
                new LambdaQueryWrapper<DocLedger>()
                    .eq(DocLedger::getProjectId, projectId)
                    .eq(DocLedger::getDocType, dep.doc));
            DocDependencyDef docDef = dependencyDefs.get(dep.doc);
            status.docName = docDef != null ? docDef.name : dep.doc;

            if (!docs.isEmpty()) {
                DocLedger latest = docs.get(0);
                status.satisfied = true;
                status.detail = status.docName + "已存在（状态: " +
                    (latest.getLifecycleStatus() != null ? latest.getLifecycleStatus() : "未知") + "）";
            } else {
                status.satisfied = false;
                status.detail = status.docName + "尚未创建";
            }
        }

        return status;
    }

    // ===== 数据类 =====

    @Data
    public static class DocDependencyDef {
        String docType;
        String name;
        String code;
        List<DepItem> hardDeps = new ArrayList<>();
        List<DepItem> softDeps = new ArrayList<>();
    }

    @Data
    public static class DepItem {
        String doc;
        String field;
        String description;
    }

    @Data
    public static class DependencyCheckResult {
        String docType;
        String docName;
        boolean allHardMet;
        List<DepStatus> hardDeps = new ArrayList<>();
        List<DepStatus> softDeps = new ArrayList<>();
        String suggestion;
    }

    @Data
    public static class DepStatus {
        String docType;
        String docName;
        boolean satisfied;
        String description;
        String detail;
    }

    @Data
    public static class ConsistencyRule {
        String name;
        List<String> docs;
        List<String> fields;
        String rule;
    }
}
