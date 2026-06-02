package com.military.doc.modules.document.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.modules.document.entity.ProjectDocChecklist;
import com.military.doc.modules.document.entity.StageDocChecklistTemplate;
import com.military.doc.modules.document.mapper.ProjectDocChecklistMapper;
import com.military.doc.modules.document.mapper.StageDocChecklistTemplateMapper;
import com.military.doc.modules.project.entity.ProjectStage;
import com.military.doc.modules.project.mapper.ProjectStageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StageDocChecklistService {

    private final StageDocChecklistTemplateMapper templateMapper;
    private final ProjectDocChecklistMapper checklistMapper;
    private final ProjectStageMapper stageMapper;
    private final ObjectMapper objectMapper;
    private final DocLedgerService docLedgerService;

    public StageDocChecklistService(StageDocChecklistTemplateMapper templateMapper,
                                    ProjectDocChecklistMapper checklistMapper,
                                    ProjectStageMapper stageMapper,
                                    ObjectMapper objectMapper,
                                    DocLedgerService docLedgerService) {
        this.templateMapper = templateMapper;
        this.checklistMapper = checklistMapper;
        this.stageMapper = stageMapper;
        this.objectMapper = objectMapper;
        this.docLedgerService = docLedgerService;
    }

    /**
     * Get all templates applicable for a given stage code
     */
    public List<StageDocChecklistTemplate> getTemplatesByStage(String stageCode) {
        List<StageDocChecklistTemplate> all = templateMapper.selectList(
            new LambdaQueryWrapper<StageDocChecklistTemplate>()
                .orderByAsc(StageDocChecklistTemplate::getOrderNum));

        return all.stream()
            .filter(t -> {
                try {
                    @SuppressWarnings("unchecked")
                    List<String> stages = objectMapper.readValue(t.getApplicableStages(), List.class);
                    return stages != null && stages.contains(stageCode);
                } catch (Exception e) {
                    return false;
                }
            })
            .collect(Collectors.toList());
    }

    /**
     * Get all templates grouped by category
     */
    public Map<String, List<StageDocChecklistTemplate>> getTemplatesByCategory(String stageCode) {
        return getTemplatesByStage(stageCode).stream()
            .collect(Collectors.groupingBy(StageDocChecklistTemplate::getCategory,
                LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * Generate project checklist from template for a stage.
     * Deletes existing non-custom items and regenerates from templates.
     */
    @Transactional
    public List<ProjectDocChecklist> generateChecklist(Long projectId, Long stageId, String stageCode) {
        ProjectStage stage = stageMapper.selectById(stageId);
        if (stage == null) {
            throw new IllegalArgumentException("未找到阶段: " + stageId);
        }

        // Delete existing auto-generated items (keep custom ones)
        // First cascade-delete corresponding DocLedger entries
        List<ProjectDocChecklist> oldItems = checklistMapper.selectList(new LambdaQueryWrapper<ProjectDocChecklist>()
            .eq(ProjectDocChecklist::getProjectId, projectId)
            .eq(ProjectDocChecklist::getStageId, stageId)
            .eq(ProjectDocChecklist::getIsCustom, false));
        for (ProjectDocChecklist oldItem : oldItems) {
            docLedgerService.deleteByChecklistItemId(oldItem.getId());
        }
        // Then delete the checklist items themselves
        checklistMapper.delete(new LambdaQueryWrapper<ProjectDocChecklist>()
            .eq(ProjectDocChecklist::getProjectId, projectId)
            .eq(ProjectDocChecklist::getStageId, stageId)
            .eq(ProjectDocChecklist::getIsCustom, false));

        // Get templates for this stage
        List<StageDocChecklistTemplate> templates = getTemplatesByStage(stageCode);

        if (templates.isEmpty()) {
            log.warn("No checklist templates found for stageCode={}", stageCode);
            return List.of();
        }

        // Get max sort order from existing custom items
        List<ProjectDocChecklist> existing = checklistMapper.selectList(
            new LambdaQueryWrapper<ProjectDocChecklist>()
                .eq(ProjectDocChecklist::getProjectId, projectId)
                .eq(ProjectDocChecklist::getStageId, stageId));
        int maxOrder = existing.stream().mapToInt(c -> c.getSortOrder() != null ? c.getSortOrder() : 0).max().orElse(0);

        List<ProjectDocChecklist> items = new ArrayList<>();
        for (int i = 0; i < templates.size(); i++) {
            StageDocChecklistTemplate tmpl = templates.get(i);
            ProjectDocChecklist item = new ProjectDocChecklist();
            item.setProjectId(projectId);
            item.setStageId(stageId);
            item.setStageCode(stageCode);
            item.setTemplateId(tmpl.getId());
            item.setDocName(tmpl.getDocName());
            item.setCategory(tmpl.getCategory());
            item.setCategoryCode(tmpl.getCategoryCode());
            item.setDocStatus("NOT_STARTED");
            item.setSortOrder(maxOrder + i + 1);
            item.setIsCustom(false);
            checklistMapper.insert(item);
            items.add(item);
        }

        log.info("Generated {} checklist items for project={} stage={} (stageCode={})",
            items.size(), projectId, stageId, stageCode);

        // Auto-sync to document ledger (operatorId=0 for system-initiated)
        try {
            int synced = docLedgerService.syncFromChecklist(projectId, stageId, 0L);
            log.info("Synced {} checklist items to ledger for project={} stage={}", synced, projectId, stageId);
        } catch (Exception e) {
            log.warn("Failed to sync checklist to ledger for stage {}: {}", stageId, e.getMessage());
        }

        return items;
    }

    /**
     * Get checklist for a project stage
     */
    public List<ProjectDocChecklist> getChecklist(Long projectId, Long stageId) {
        return checklistMapper.selectList(
            new LambdaQueryWrapper<ProjectDocChecklist>()
                .eq(ProjectDocChecklist::getProjectId, projectId)
                .eq(ProjectDocChecklist::getStageId, stageId)
                .orderByAsc(ProjectDocChecklist::getSortOrder));
    }

    /**
     * Get checklist statistics
     */
    public Map<String, Object> getStats(Long projectId, Long stageId) {
        List<ProjectDocChecklist> items = getChecklist(projectId, stageId);
        long total = items.size();
        long completed = items.stream().filter(i -> "APPROVED".equals(i.getDocStatus())).count();
        long inProgress = items.stream().filter(i -> "IN_PROGRESS".equals(i.getDocStatus()) || "DRAFT".equals(i.getDocStatus())).count();
        long notStarted = items.stream().filter(i -> "NOT_STARTED".equals(i.getDocStatus())).count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("completed", completed);
        stats.put("inProgress", inProgress);
        stats.put("notStarted", notStarted);
        stats.put("completionRate", total > 0 ? Math.round(100.0 * completed / total) : 0);
        return stats;
    }

    /**
     * Add custom checklist item
     */
    @Transactional
    public ProjectDocChecklist addCustomItem(Long projectId, Long stageId, String docName, String category) {
        ProjectStage stage = stageMapper.selectById(stageId);
        if (stage == null) {
            throw new IllegalArgumentException("未找到阶段: " + stageId);
        }

        List<ProjectDocChecklist> existing = getChecklist(projectId, stageId);
        int maxOrder = existing.stream().mapToInt(c -> c.getSortOrder() != null ? c.getSortOrder() : 0).max().orElse(0);

        ProjectDocChecklist item = new ProjectDocChecklist();
        item.setProjectId(projectId);
        item.setStageId(stageId);
        item.setStageCode(stage.getStageCode());
        item.setDocName(docName);
        item.setCategory(category != null ? category : "自定义");
        item.setCategoryCode("CUSTOM");
        item.setDocStatus("NOT_STARTED");
        item.setSortOrder(maxOrder + 1);
        item.setIsCustom(true);
        checklistMapper.insert(item);
        log.info("Added custom checklist item: {} for project={} stage={}", docName, projectId, stageId);
        return item;
    }

    /**
     * Update checklist item (rename, change status, etc.)
     */
    @Transactional
    public ProjectDocChecklist updateItem(Long itemId, String docName, String docStatus,
                                           String responsiblePerson, String notes) {
        ProjectDocChecklist item = checklistMapper.selectById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("未找到检查清单项: " + itemId);
        }
        if (docName != null) item.setDocName(docName);
        if (docStatus != null) item.setDocStatus(docStatus);
        if (responsiblePerson != null) item.setResponsiblePerson(responsiblePerson);
        if (notes != null) item.setNotes(notes);
        item.setUpdatedAt(LocalDateTime.now());
        checklistMapper.updateById(item);
        return item;
    }

    /**
     * Delete checklist item and cascade-delete corresponding doc ledger entries.
     */
    @Transactional
    public void deleteItem(Long itemId) {
        ProjectDocChecklist item = checklistMapper.selectById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("未找到检查清单项: " + itemId);
        }
        // 级联删除对应的文档台账条目
        int ledgerDeleted = docLedgerService.deleteByChecklistItemId(itemId);
        checklistMapper.deleteById(itemId);
        log.info("Deleted checklist item: {} (id={}), cascade-deleted {} ledger entries",
            item.getDocName(), itemId, ledgerDeleted);
    }

    /**
     * Auto-generate checklist when stage is initialized.
     * Called from ProjectStageService.initializeProjectStages().
     */
    @Transactional
    public void autoGenerateForStage(Long projectId, Long stageId, String stageCode) {
        int count = generateChecklist(projectId, stageId, stageCode).size();
        log.info("Auto-generated {} checklist items for project={} stage={} stageCode={}", count, projectId, stageId, stageCode);
    }
}
