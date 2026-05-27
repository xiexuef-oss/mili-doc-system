package com.military.doc.modules.document.controller;

import com.military.doc.common.result.Result;
import com.military.doc.modules.document.entity.ProjectDocChecklist;
import com.military.doc.modules.document.entity.StageDocChecklistTemplate;
import com.military.doc.modules.document.service.DocLedgerService;
import com.military.doc.modules.document.service.StageDocChecklistService;
import com.military.doc.modules.project.constant.StageDefinition;
import com.military.doc.modules.project.entity.ProjectStage;
import com.military.doc.modules.project.mapper.ProjectStageMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "阶段文档清单")
public class StageDocChecklistController {

    private final StageDocChecklistService checklistService;
    private final DocLedgerService docLedgerService;
    private final ProjectStageMapper stageMapper;

    public StageDocChecklistController(StageDocChecklistService checklistService,
                                        DocLedgerService docLedgerService,
                                        ProjectStageMapper stageMapper) {
        this.checklistService = checklistService;
        this.docLedgerService = docLedgerService;
        this.stageMapper = stageMapper;
    }

    @GetMapping("/stage-checklist/templates")
    @Operation(summary = "获取指定阶段的文档清单模板（复用GJB编写指南全部15大类约190个文档）")
    public Result<List<StageDocChecklistTemplate>> getTemplates(@RequestParam String stageCode) {
        return Result.success(checklistService.getTemplatesByStage(stageCode));
    }

    @GetMapping("/stage-checklist/templates/by-category")
    @Operation(summary = "按类别分组获取阶段文档清单模板")
    public Result<Map<String, List<StageDocChecklistTemplate>>> getTemplatesByCategory(@RequestParam String stageCode) {
        return Result.success(checklistService.getTemplatesByCategory(stageCode));
    }

    @PostMapping("/projects/{projectId}/stages/{stageId}/checklist/generate")
    @Operation(summary = "为项目阶段生成文档清单（从GJB模板库自动生成，同时同步到文档台账）")
    public Result<List<ProjectDocChecklist>> generate(@PathVariable Long projectId,
                                                      @PathVariable Long stageId) {
        ProjectStage stage = stageMapper.selectById(stageId);
        if (stage == null) {
            return Result.error("STAGE_NOT_FOUND", "阶段不存在: " + stageId);
        }
        return Result.success(checklistService.generateChecklist(projectId, stageId, stage.getStageCode()));
    }

    @PostMapping("/projects/{projectId}/stages/{stageId}/checklist/sync-to-ledger")
    @Operation(summary = "手动将文档清单同步到文档台账")
    public Result<Map<String, Object>> syncToLedger(@PathVariable Long projectId,
                                                     @PathVariable Long stageId) {
        int count = docLedgerService.syncFromChecklist(projectId, stageId, 0L);
        return Result.success(Map.of("syncedCount", count, "message", "成功同步 " + count + " 条到文档台账"));
    }

    @GetMapping("/projects/{projectId}/stages/{stageId}/checklist")
    @Operation(summary = "获取项目阶段的文档清单")
    public Result<List<ProjectDocChecklist>> getChecklist(@PathVariable Long projectId,
                                                           @PathVariable Long stageId) {
        return Result.success(checklistService.getChecklist(projectId, stageId));
    }

    @GetMapping("/projects/{projectId}/stages/{stageId}/checklist/stats")
    @Operation(summary = "获取文档清单统计信息")
    public Result<Map<String, Object>> getStats(@PathVariable Long projectId,
                                                 @PathVariable Long stageId) {
        return Result.success(checklistService.getStats(projectId, stageId));
    }

    @PostMapping("/projects/{projectId}/stages/{stageId}/checklist/items")
    @Operation(summary = "添加自定义文档")
    public Result<ProjectDocChecklist> addCustomItem(@PathVariable Long projectId,
                                                      @PathVariable Long stageId,
                                                      @RequestBody Map<String, String> body) {
        String docName = body.get("docName");
        if (docName == null || docName.isBlank()) {
            return Result.error("PARAM_ERROR", "docName is required");
        }
        String category = body.get("category");
        return Result.success(checklistService.addCustomItem(projectId, stageId, docName, category));
    }

    @PutMapping("/projects/{projectId}/stages/{stageId}/checklist/items/{itemId}")
    @Operation(summary = "更新文档清单项（修改名称/状态/负责人/备注）")
    public Result<ProjectDocChecklist> updateItem(@PathVariable Long projectId,
                                                   @PathVariable Long stageId,
                                                   @PathVariable Long itemId,
                                                   @RequestBody Map<String, String> body) {
        return Result.success(checklistService.updateItem(itemId,
            body.get("docName"),
            body.get("docStatus"),
            body.get("responsiblePerson"),
            body.get("notes")));
    }

    @DeleteMapping("/projects/{projectId}/stages/{stageId}/checklist/items/{itemId}")
    @Operation(summary = "删除文档清单项")
    public Result<String> deleteItem(@PathVariable Long projectId,
                                      @PathVariable Long stageId,
                                      @PathVariable Long itemId) {
        checklistService.deleteItem(itemId);
        return Result.success("删除成功");
    }

    @GetMapping("/stages/definitions/with-checklist")
    @Operation(summary = "获取预定义阶段列表，附带各阶段的文档模板数量")
    public Result<List<Map<String, Object>>> definitionsWithCount() {
        List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (StageDefinition def : StageDefinition.ALL) {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("code", def.code());
            map.put("name", def.name());
            map.put("order", def.order());
            map.put("description", def.description());
            map.put("defaultBaselineType", def.defaultBaselineType());
            map.put("templateCount", checklistService.getTemplatesByStage(def.code()).size());
            list.add(map);
        }
        return Result.success(list);
    }
}
