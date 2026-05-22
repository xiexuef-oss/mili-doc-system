package com.military.doc.modules.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.common.exception.BusinessException;
import com.military.doc.modules.project.entity.ProjectMasterData;
import com.military.doc.modules.project.mapper.ProjectMasterDataMapper;
import com.military.doc.modules.project.service.ProjectMasterDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProjectMasterDataServiceImpl
        extends ServiceImpl<ProjectMasterDataMapper, ProjectMasterData>
        implements ProjectMasterDataService {

    @Autowired private ObjectMapper objectMapper;

    @Override
    public ProjectMasterData getByProjectId(Long projectId) {
        ProjectMasterData pmd = getOne(new LambdaQueryWrapper<ProjectMasterData>()
                .eq(ProjectMasterData::getProjectId, projectId));
        if (pmd == null) {
            pmd = new ProjectMasterData();
            pmd.setProjectId(projectId);
            pmd.setEquipmentInfo("{}");
            pmd.setTacticalIndicators("[]");
            pmd.setProductTree("[]");
            pmd.setTeamMembers("[]");
            pmd.setMilestones("[]");
            pmd.setExtendedFields("{}");
            pmd.setVersionNo(1);
            pmd.setStatus("DRAFT");
        }
        return pmd;
    }

    @Override
    @Transactional
    public ProjectMasterData saveOrUpdateMasterData(Long projectId, Map<String, Object> data, Long operatorId) {
        ProjectMasterData pmd = getOne(new LambdaQueryWrapper<ProjectMasterData>()
                .eq(ProjectMasterData::getProjectId, projectId));

        if (pmd == null) {
            pmd = new ProjectMasterData();
            pmd.setProjectId(projectId);
            pmd.setVersionNo(1);
            pmd.setCreatedBy(operatorId);
        } else {
            pmd.setVersionNo(pmd.getVersionNo() != null ? pmd.getVersionNo() + 1 : 1);
        }

        try {
            if (data.containsKey("equipmentInfo"))
                pmd.setEquipmentInfo(toJson(data.get("equipmentInfo")));
            if (data.containsKey("tacticalIndicators"))
                pmd.setTacticalIndicators(toJson(data.get("tacticalIndicators")));
            if (data.containsKey("productTree"))
                pmd.setProductTree(toJson(data.get("productTree")));
            if (data.containsKey("teamMembers"))
                pmd.setTeamMembers(toJson(data.get("teamMembers")));
            if (data.containsKey("milestones"))
                pmd.setMilestones(toJson(data.get("milestones")));
            if (data.containsKey("extendedFields"))
                pmd.setExtendedFields(toJson(data.get("extendedFields")));
        } catch (JsonProcessingException e) {
            throw BusinessException.validation("数据格式错误: " + e.getMessage());
        }

        pmd.setUpdatedBy(operatorId);
        saveOrUpdate(pmd);
        return pmd;
    }

    @Override
    public Map<String, Object> getFlattenedData(Long projectId) {
        ProjectMasterData pmd = getByProjectId(projectId);
        Map<String, Object> flat = new LinkedHashMap<>();
        try {
            if (pmd.getEquipmentInfo() != null)
                flattenObject("equipment", objectMapper.readValue(pmd.getEquipmentInfo(), Map.class), flat);
            if (pmd.getTacticalIndicators() != null)
                flat.put("tacticalIndicators", objectMapper.readValue(pmd.getTacticalIndicators(), Object.class));
            if (pmd.getProductTree() != null)
                flat.put("productTree", objectMapper.readValue(pmd.getProductTree(), Object.class));
            if (pmd.getTeamMembers() != null)
                flat.put("teamMembers", objectMapper.readValue(pmd.getTeamMembers(), Object.class));
            if (pmd.getMilestones() != null)
                flat.put("milestones", objectMapper.readValue(pmd.getMilestones(), Object.class));
        } catch (JsonProcessingException e) {
            // return empty map on parse error
        }
        return flat;
    }

    private String toJson(Object obj) throws JsonProcessingException {
        if (obj == null) return "{}";
        if (obj instanceof String s) return s;
        return objectMapper.writeValueAsString(obj);
    }

    @SuppressWarnings("unchecked")
    private void flattenObject(String prefix, Map<String, Object> source, Map<String, Object> target) {
        if (source == null) return;
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            target.put(prefix + "." + entry.getKey(), entry.getValue());
        }
    }
}
