package com.military.doc.modules.project.controller;

import com.military.doc.common.result.Result;
import com.military.doc.modules.project.constant.MasterDataSchemaRegistry;
import com.military.doc.modules.project.entity.ProjectMasterData;
import com.military.doc.ai.service.MasterDataExtractionService;
import com.military.doc.modules.project.service.ProjectMasterDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/master-data")
public class ProjectMasterDataController {

    @Autowired private ProjectMasterDataService masterDataService;
    @Autowired private MasterDataExtractionService extractionService;
    private static final Logger log = LoggerFactory.getLogger(ProjectMasterDataController.class);

    @GetMapping
    public Result<Map<String, Object>> get(@PathVariable Long projectId) {
        return Result.success(masterDataService.getParsedData(projectId));
    }

    @GetMapping("/flattened")
    public Result<Map<String, Object>> getFlattened(@PathVariable Long projectId) {
        return Result.success(masterDataService.getFlattenedData(projectId));
    }

    @PostMapping
    public Result<ProjectMasterData> save(@PathVariable Long projectId,
                                           @RequestBody Map<String, Object> data,
                                           Authentication authentication) {
        Long operatorId = (Long) authentication.getPrincipal();
        return Result.success(masterDataService.saveOrUpdateMasterData(projectId, data, operatorId));
    }

    @GetMapping("/schema")
    public Result<Map<String, Object>> getSchema() {
        return Result.success(MasterDataSchemaRegistry.getDefaultMasterData());
    }

    @GetMapping("/default")
    public Result<Map<String, Object>> getDefault() {
        return Result.success(MasterDataSchemaRegistry.getDefaultMasterData());
    }

    /**
     * 从项目输入文件中 AI 提取主数据
     */
    @PostMapping("/extract")
    public Result<Map<String, Object>> extractFromFiles(@PathVariable Long projectId) {
        try {
            Map<String, Object> extracted = extractionService.extractFromInputFiles(projectId);
            if (extracted.isEmpty()) {
                return Result.error("NO_DATA", "未找到可提取的输入文件或提取无结果，请先上传项目输入文件");
            }
            return Result.success(extracted);
        } catch (Exception e) {
            log.error("Failed to extract master data for project {}: {}", projectId, e.getMessage());
            return Result.error("EXTRACTION_FAILED", "AI 提取失败: " + e.getMessage());
        }
    }

}
