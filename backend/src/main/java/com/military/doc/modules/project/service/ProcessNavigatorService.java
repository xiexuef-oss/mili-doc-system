package com.military.doc.modules.project.service;

import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import com.military.doc.modules.project.entity.ConfigurationBaseline;
import com.military.doc.modules.project.entity.ProjectStage;
import com.military.doc.modules.project.mapper.ConfigurationBaselineMapper;
import com.military.doc.modules.project.mapper.ProjectStageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Aggregates project stage, document, and check data into a unified navigator view.
 */
@Service
public class ProcessNavigatorService {

    @Autowired private ProjectStageMapper stageMapper;
    @Autowired private DocLedgerMapper docLedgerMapper;
    @Autowired private ConfigurationBaselineMapper baselineMapper;

    public Map<String, Object> getNavigatorData(Long projectId) {
        List<ProjectStage> stages = stageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProjectStage>()
                        .eq(ProjectStage::getProjectId, projectId)
                        .orderByAsc(ProjectStage::getStageOrder));

        List<DocLedger> allDocs = docLedgerMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocLedger>()
                        .eq(DocLedger::getProjectId, projectId));

        List<Map<String, Object>> stageList = new ArrayList<>();
        ProjectStage currentStage = null;

        for (ProjectStage stage : stages) {
            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("id", stage.getId());
            sm.put("stageCode", stage.getStageCode());
            sm.put("stageName", stage.getStageName());
            sm.put("stageOrder", stage.getStageOrder());
            sm.put("status", stage.getStatus());

            // Count documents for this stage
            long totalDocs = allDocs.stream()
                    .filter(d -> stage.getId().equals(d.getStageId())).count();
            long releasedDocs = allDocs.stream()
                    .filter(d -> stage.getId().equals(d.getStageId())
                            && "RELEASED".equals(d.getLifecycleStatus())).count();
            long draftingDocs = allDocs.stream()
                    .filter(d -> stage.getId().equals(d.getStageId())
                            && "DRAFTING".equals(d.getLifecycleStatus())).count();

            sm.put("totalDocs", totalDocs);
            sm.put("releasedDocs", releasedDocs);
            sm.put("draftingDocs", draftingDocs);
            sm.put("completionRate", totalDocs > 0 ? Math.round((double) releasedDocs / totalDocs * 100) : 0);

            // Baselines for this stage
            List<ConfigurationBaseline> baselines = baselineMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ConfigurationBaseline>()
                            .eq(ConfigurationBaseline::getProjectId, projectId)
                            .eq(ConfigurationBaseline::getStageId, stage.getId()));
            sm.put("baselines", baselines.stream().map(b -> Map.of(
                    "id", b.getId(), "name", b.getBaselineName(),
                    "type", b.getBaselineType(), "status", b.getBaselineStatus()
            )).toList());

            if (Boolean.TRUE.equals(stage.getIsCurrent()) || "IN_PROGRESS".equals(stage.getStatus())) {
                currentStage = stage;
            }

            stageList.add(sm);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stages", stageList);
        result.put("totalDocs", allDocs.size());
        result.put("releasedDocs", allDocs.stream().filter(d -> "RELEASED".equals(d.getLifecycleStatus())).count());
        result.put("currentStage", currentStage != null ? Map.of(
                "id", currentStage.getId(),
                "stageCode", currentStage.getStageCode(),
                "stageName", currentStage.getStageName()
        ) : null);

        // Stage definitions
        List<Map<String, String>> stageDefs = List.of(
                Map.of("code", "L", "name", "论证阶段", "order", "1"),
                Map.of("code", "F", "name", "方案阶段", "order", "2"),
                Map.of("code", "C", "name", "工程研制阶段", "order", "3"),
                Map.of("code", "S", "name", "设计定型阶段", "order", "4"),
                Map.of("code", "D", "name", "生产定型阶段", "order", "5"),
                Map.of("code", "P", "name", "批生产阶段", "order", "6"),
                Map.of("code", "N", "name", "退役阶段", "order", "7")
        );
        result.put("stageDefinitions", stageDefs);

        return result;
    }
}
