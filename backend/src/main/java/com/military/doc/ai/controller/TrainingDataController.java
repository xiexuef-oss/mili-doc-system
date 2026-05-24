package com.military.doc.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.military.doc.ai.entity.TrainingExample;
import com.military.doc.ai.service.TrainingDataService;
import com.military.doc.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai/training")
public class TrainingDataController {

    @Autowired
    private TrainingDataService trainingDataService;

    @PostMapping("/collect")
    public Result<TrainingExample> collect(@RequestBody Map<String, Object> body, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Long docFileId = toLong(body.get("docFileId"));
        Long projectId = toLong(body.get("projectId"));
        Long catalogId = toLong(body.get("catalogId"));
        return Result.success(trainingDataService.collect(docFileId, projectId, catalogId, userId));
    }

    @GetMapping("/examples")
    public Result<Page<TrainingExample>> list(
            @RequestParam(required = false) String quality,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(trainingDataService.list(quality, page, size));
    }

    @PutMapping("/examples/{id}/approve")
    public Result<TrainingExample> approve(@PathVariable Long id) {
        return Result.success(trainingDataService.approve(id));
    }

    @PutMapping("/examples/{id}/reject")
    public Result<TrainingExample> reject(@PathVariable Long id) {
        return Result.success(trainingDataService.reject(id));
    }

    @GetMapping("/export")
    public Result<Map<String, String>> export(@RequestParam(defaultValue = "APPROVED") String quality) {
        return Result.success(Map.of("data", trainingDataService.exportJsonl(quality)));
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.longValue();
        return Long.parseLong(obj.toString());
    }
}
