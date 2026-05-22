package com.military.doc.modules.project.controller;

import com.military.doc.common.result.Result;
import com.military.doc.modules.project.constant.MasterDataSchemaRegistry;
import com.military.doc.modules.project.entity.ProjectMasterData;
import com.military.doc.modules.project.service.ProjectMasterDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/master-data")
public class ProjectMasterDataController {

    @Autowired private ProjectMasterDataService masterDataService;

    @GetMapping
    public Result<ProjectMasterData> get(@PathVariable Long projectId) {
        return Result.success(masterDataService.getByProjectId(projectId));
    }

    @GetMapping("/flattened")
    public Result<Map<String, Object>> getFlattened(@PathVariable Long projectId) {
        return Result.success(masterDataService.getFlattenedData(projectId));
    }

    @PostMapping
    public Result<ProjectMasterData> save(@PathVariable Long projectId,
                                           @RequestBody Map<String, Object> data,
                                           @RequestParam Long operatorId) {
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
}
