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

    /**
     * Get master data as a parsed Map (JSONB fields parsed into objects/arrays).
     * This is the frontend-compatible format.
     */
    @Override
    public Map<String, Object> getParsedData(Long projectId) {
        ProjectMasterData pmd = getByProjectId(projectId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", pmd.getId());
        result.put("projectId", pmd.getProjectId());
        result.put("versionNo", pmd.getVersionNo());
        result.put("status", pmd.getStatus());
        result.put("createdBy", pmd.getCreatedBy());
        result.put("createdAt", pmd.getCreatedAt());
        result.put("updatedBy", pmd.getUpdatedBy());
        result.put("updatedAt", pmd.getUpdatedAt());
        result.put("equipmentInfo", parseJsonObject(pmd.getEquipmentInfo()));
        result.put("tacticalIndicators", parseJsonArray(pmd.getTacticalIndicators()));
        result.put("productTree", parseJsonArray(pmd.getProductTree()));
        result.put("milestones", parseJsonArray(pmd.getMilestones()));
        result.put("extendedFields", parseJsonObject(pmd.getExtendedFields()));
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonObject(String json) {
        try {
            if (json == null || json.isEmpty() || "null".equals(json)) return new LinkedHashMap<>();
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return new LinkedHashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseJsonArray(String json) {
        try {
            if (json == null || json.isEmpty() || "null".equals(json)) return List.of();
            return objectMapper.readValue(json, List.class);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    @Override
    @Transactional
    public ProjectMasterData saveOrUpdateMasterData(Long projectId, Map<String, Object> data, Long operatorId) {
        ProjectMasterData pmd = getOne(new LambdaQueryWrapper<ProjectMasterData>()
                .eq(ProjectMasterData::getProjectId, projectId));

        boolean isNew = (pmd == null);
        if (isNew) {
            pmd = new ProjectMasterData();
            pmd.setProjectId(projectId);
            pmd.setVersionNo(1);
            pmd.setCreatedBy(operatorId);
        } else {
            pmd.setVersionNo(pmd.getVersionNo() != null ? pmd.getVersionNo() + 1 : 1);
        }

        try {
            // Normalize field names from AI output to canonical form
            if (data.containsKey("equipmentInfo"))
                pmd.setEquipmentInfo(toJson(normalizeEquipmentInfo(data.get("equipmentInfo"))));
            if (data.containsKey("tacticalIndicators"))
                pmd.setTacticalIndicators(toJson(normalizeTacticalIndicators(data.get("tacticalIndicators"))));
            if (data.containsKey("productTree"))
                pmd.setProductTree(toJson(normalizeProductTree(data.get("productTree"))));
            if (data.containsKey("teamMembers"))
                pmd.setTeamMembers(toJson(normalizeTeamMembers(data.get("teamMembers"))));
            if (data.containsKey("milestones"))
                pmd.setMilestones(toJson(normalizeMilestones(data.get("milestones"))));
            if (data.containsKey("extendedFields"))
                pmd.setExtendedFields(toJson(data.get("extendedFields")));
        } catch (JsonProcessingException e) {
            throw BusinessException.validation("数据格式错误: " + e.getMessage());
        }

        pmd.setUpdatedBy(operatorId);
        saveOrUpdate(pmd);
        return pmd;
    }

    // ---- Field name normalization: map legacy AI output names to frontend-compatible names ----

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeEquipmentInfo(Object obj) {
        // equipmentInfo field names are stable; pass through
        if (obj instanceof Map) return (Map<String, Object>) obj;
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeTacticalIndicators(Object obj) {
        if (!(obj instanceof List)) return List.of();
        List<Map<String, Object>> list = (List<Map<String, Object>>) obj;
        return list.stream().map(row -> {
            Map<String, Object> n = new LinkedHashMap<>(row);
            // Legacy AI field -> canonical frontend field
            remap(n, "value", "required");
            remap(n, "requirementSource", "conclusion");
            remap(n, "requirementValue", "required");
            remap(n, "measuredValue", "actual");
            remap(n, "testMethod", "remark");
            // Ensure all frontend-expected keys exist
            n.putIfAbsent("indicatorName", "");
            n.putIfAbsent("required", "");
            n.putIfAbsent("actual", "");
            n.putIfAbsent("unit", "");
            n.putIfAbsent("conclusion", "");
            n.putIfAbsent("remark", "");
            return n;
        }).toList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeProductTree(Object obj) {
        if (!(obj instanceof List)) return List.of();
        List<Map<String, Object>> list = (List<Map<String, Object>>) obj;
        return list.stream().map(row -> {
            Map<String, Object> n = new LinkedHashMap<>(row);
            // Legacy AI field -> canonical frontend field
            remap(n, "itemName", "productName");
            remap(n, "itemCode", "productCode");
            remap(n, "parentCode", "parentProduct");
            remap(n, "nodeName", "productName");
            remap(n, "nodeCode", "productCode");
            remap(n, "parentNodeCode", "parentProduct");
            remap(n, "nodeLevel", "level");
            // Convert numeric level to Chinese string if needed
            Object lv = n.get("level");
            if (lv instanceof Integer || lv instanceof Long) {
                n.put("level", levelToString(((Number) lv).intValue()));
            }
            // Ensure all frontend-expected keys exist
            n.putIfAbsent("level", "");
            n.putIfAbsent("productName", "");
            n.putIfAbsent("productCode", "");
            n.putIfAbsent("parentProduct", "");
            n.putIfAbsent("quantity", "");
            n.putIfAbsent("remark", "");
            return n;
        }).toList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeTeamMembers(Object obj) {
        if (!(obj instanceof List)) return List.of();
        List<Map<String, Object>> list = (List<Map<String, Object>>) obj;
        return list.stream().map(row -> {
            Map<String, Object> n = new LinkedHashMap<>(row);
            remap(n, "unit", "department");
            remap(n, "organization", "department");
            remap(n, "contact", "phone");
            remap(n, "contactPhone", "phone");
            remap(n, "duties", "email");
            n.putIfAbsent("name", "");
            n.putIfAbsent("role", "");
            n.putIfAbsent("department", "");
            n.putIfAbsent("phone", "");
            n.putIfAbsent("email", "");
            return n;
        }).toList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeMilestones(Object obj) {
        if (!(obj instanceof List)) return List.of();
        List<Map<String, Object>> list = (List<Map<String, Object>>) obj;
        return list.stream().map(row -> {
            Map<String, Object> n = new LinkedHashMap<>(row);
            remap(n, "deadline", "plannedDate");
            remap(n, "deliverable", "keyDeliverables");
            remap(n, "deliverables", "keyDeliverables");
            remap(n, "milestoneName", "name");
            remap(n, "acceptanceCriteria", "keyDeliverables");
            n.putIfAbsent("stageCode", "");
            n.putIfAbsent("name", "");
            n.putIfAbsent("plannedDate", "");
            n.putIfAbsent("actualDate", "");
            n.putIfAbsent("keyDeliverables", "");
            n.putIfAbsent("status", "");
            return n;
        }).toList();
    }

    private void remap(Map<String, Object> map, String oldKey, String newKey) {
        if (map.containsKey(oldKey) && !oldKey.equals(newKey)) {
            Object val = map.remove(oldKey);
            if (val != null && (!(val instanceof String) || !((String) val).isEmpty())) {
                map.putIfAbsent(newKey, val);
            }
        }
    }

    private String levelToString(int level) {
        return switch (level) {
            case 1 -> "系统";
            case 2 -> "分系统";
            case 3 -> "设备";
            case 4 -> "组件";
            default -> "第" + level + "级";
        };
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
