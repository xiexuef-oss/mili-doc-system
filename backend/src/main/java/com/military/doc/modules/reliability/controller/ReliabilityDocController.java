package com.military.doc.modules.reliability.controller;

import com.military.doc.ai.service.ReliabilityGenerationService;
import com.military.doc.common.result.Result;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import com.military.doc.modules.reliability.calc.ReliabilityAllocator;
import com.military.doc.modules.reliability.calc.ReliabilityPredictor;
import com.military.doc.modules.reliability.entity.RelRequirement;
import com.military.doc.modules.reliability.mapper.RelRequirementMapper;
import com.military.doc.modules.project.mapper.ProjectStageMapper;
import com.military.doc.modules.project.entity.ProjectStage;
import com.military.doc.modules.project.mapper.ProjectStageMapper;
import com.military.doc.modules.project.entity.ProjectStage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 可靠性文档生成控制器。
 * 提供可靠性大纲、降额报告、预计报告、分配报告的生成 API。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/reliability")
public class ReliabilityDocController {

    private final ReliabilityGenerationService reliabilityGenerationService;
    private final ReliabilityPredictor reliabilityPredictor;
    private final ReliabilityAllocator reliabilityAllocator;
    private final RelRequirementMapper relRequirementMapper;
    private final DocLedgerMapper docLedgerMapper;
    private final ProjectStageMapper projectStageMapper;

    public ReliabilityDocController(ReliabilityGenerationService reliabilityGenerationService,
                                     ReliabilityPredictor reliabilityPredictor,
                                     ReliabilityAllocator reliabilityAllocator,
                                     RelRequirementMapper relRequirementMapper,
                                     DocLedgerMapper docLedgerMapper,
                                     ProjectStageMapper projectStageMapper) {
        this.reliabilityGenerationService = reliabilityGenerationService;
        this.reliabilityPredictor = reliabilityPredictor;
        this.reliabilityAllocator = reliabilityAllocator;
        this.relRequirementMapper = relRequirementMapper;
        this.docLedgerMapper = docLedgerMapper;
        this.projectStageMapper = projectStageMapper;
    }

    // ===== A档：可靠性大纲 =====

    @PostMapping("/outline/generate")
    public Result<Map<String, Object>> generateOutline(
            @RequestParam Long projectId,
            @RequestParam(required = false) Long stageId) {
        // 如果未指定阶段，使用项目当前活跃阶段
        if (stageId == null) stageId = getActiveStageId(projectId);
        DocLedger ledger = reliabilityGenerationService.findOrCreateLedger(
            projectId, stageId, "REL-OUTLINE-001", "可靠性大纲");
        // 生成内容
        String content = reliabilityGenerationService.generateReliabilityOutline(projectId, ledger.getId());
        // 保存到台账
        var saveResult = reliabilityGenerationService.saveContentToLedger(ledger.getId(), content);
        return Result.success(Map.of(
            "content", content,
            "docLedgerId", ledger.getId(),
            "docName", ledger.getDocName(),
            "chapters", saveResult.get("chapters"),
            "version", saveResult.getOrDefault("version", "V0.1")
        ));
    }

    @PostMapping(value = "/outline/generate-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateOutlineStream(
            @RequestParam Long projectId, @RequestParam Long docLedgerId) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5min timeout
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                reliabilityGenerationService.generateReliabilityOutlineStream(projectId, docLedgerId,
                    chunk -> {
                        try { emitter.send(SseEmitter.event().data(chunk)); }
                        catch (IOException e) { emitter.completeWithError(e); }
                    });
                emitter.complete();
            } catch (Exception e) {
                log.error("Outline stream failed", e);
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    // ===== A档：降额设计报告 =====

    @PostMapping("/derating/generate")
    public Result<Map<String, Object>> generateDerating(
            @RequestParam Long projectId,
            @RequestParam(required = false) Long stageId) {
        if (stageId == null) stageId = getActiveStageId(projectId);
        DocLedger ledger = reliabilityGenerationService.findOrCreateLedger(
            projectId, stageId, "REL-DERATING-001", "降额设计报告");
        String content = reliabilityGenerationService.generateDeratingReport(projectId, ledger.getId());
        var saveResult = reliabilityGenerationService.saveContentToLedger(ledger.getId(), content);
        return Result.success(Map.of(
            "content", content,
            "docLedgerId", ledger.getId(),
            "docName", ledger.getDocName(),
            "chapters", saveResult.get("chapters"),
            "version", saveResult.getOrDefault("version", "V0.1")
        ));
    }

    // ===== B档：可靠性预计报告 =====

    @PostMapping("/prediction/generate")
    public Result<Map<String, Object>> generatePrediction(
            @RequestParam Long projectId,
            @RequestParam(required = false) Long stageId,
            @RequestBody(required = false) List<ReliabilityPredictor.PredictionInputItem> bomItems,
            @RequestParam(defaultValue = "G_FIX") String environment) {
        if (stageId == null) stageId = getActiveStageId(projectId);

        DocLedger ledger = reliabilityGenerationService.findOrCreateLedger(
            projectId, stageId, "REL-PRED-001", "可靠性预计报告");

        String predictionJson = "{}";
        if (bomItems != null && !bomItems.isEmpty()) {
            var result = reliabilityPredictor.predictByStressAnalysis(bomItems, environment);
            predictionJson = result.toJson();
        }

        String content = reliabilityGenerationService.generatePredictionReport(
            projectId, ledger.getId(), predictionJson);
        var saveResult = reliabilityGenerationService.saveContentToLedger(ledger.getId(), content);
        return Result.success(Map.of(
            "content", content,
            "docLedgerId", ledger.getId(),
            "docName", ledger.getDocName(),
            "prediction", predictionJson,
            "chapters", saveResult.get("chapters")
        ));
    }

    @PostMapping("/prediction/preview")
    public Result<Map<String, Object>> previewPrediction(
            @RequestBody List<ReliabilityPredictor.PredictionInputItem> bomItems,
            @RequestParam(defaultValue = "G_FIX") String environment) {

        var result = reliabilityPredictor.predictByStressAnalysis(bomItems, environment);
        return Result.success(Map.of(
            "mtbf", result.getMtbf(),
            "totalFailureRate", result.getTotalFailureRate(),
            "itemCount", result.getItems().size(),
            "items", result.getItems(),
            "json", result.toJson()
        ));
    }

    // ===== B档：可靠性分配报告 =====

    @PostMapping("/allocation/generate")
    public Result<Map<String, Object>> generateAllocation(
            @RequestParam Long projectId,
            @RequestParam(required = false) Long stageId,
            @RequestBody Map<String, Object> allocationParams) {
        if (stageId == null) stageId = getActiveStageId(projectId);

        String method = (String) allocationParams.getOrDefault("method", "EQUAL");
        double systemMtbf = ((Number) allocationParams.getOrDefault("systemMtbf", 5000)).doubleValue();

        String allocationJson;
        try {
            var result = executeAllocation(method, systemMtbf, allocationParams);
            allocationJson = result.toJson();
        } catch (Exception e) {
            allocationJson = "{\"error\":\"" + e.getMessage() + "\"}";
        }

        DocLedger ledger = reliabilityGenerationService.findOrCreateLedger(
            projectId, stageId, "REL-ALLOC-001", "可靠性分配报告");
        String content = reliabilityGenerationService.generateAllocationReport(
            projectId, ledger.getId(), allocationJson);
        var saveResult = reliabilityGenerationService.saveContentToLedger(ledger.getId(), content);
        return Result.success(Map.of(
            "content", content,
            "docLedgerId", ledger.getId(),
            "docName", ledger.getDocName(),
            "allocation", allocationJson,
            "chapters", saveResult.get("chapters")
        ));
    }

    @PostMapping("/allocation/preview")
    public Result<Map<String, Object>> previewAllocation(
            @RequestBody Map<String, Object> params) {
        String method = (String) params.getOrDefault("method", "EQUAL");
        double systemMtbf = ((Number) params.getOrDefault("systemMtbf", 5000)).doubleValue();

        var result = executeAllocation(method, systemMtbf, params);
        return Result.success(Map.of(
            "method", result.getMethod(),
            "systemMtbf", result.getSystemMtbf(),
            "items", result.getItems(),
            "verified", result.isVerified(),
            "json", result.toJson()
        ));
    }

    // ===== 可靠性指标管理 =====

    @GetMapping("/requirement")
    public Result<List<RelRequirement>> getRequirements(@RequestParam Long projectId) {
        var list = relRequirementMapper.selectList(
            new LambdaQueryWrapper<RelRequirement>()
                .eq(RelRequirement::getProjectId, projectId)
                .orderByDesc(RelRequirement::getId));
        return Result.success(list);
    }

    @PostMapping("/requirement")
    public Result<RelRequirement> saveRequirement(@RequestBody RelRequirement req) {
        if (req.getId() == null) {
            req.setCreatedAt(LocalDateTime.now());
            relRequirementMapper.insert(req);
        } else {
            req.setUpdatedAt(LocalDateTime.now());
            relRequirementMapper.updateById(req);
        }
        return Result.success(req);
    }

    // ===== 辅助方法 =====

    private ReliabilityAllocator.AllocationResult executeAllocation(
            String method, double systemMtbf, Map<String, Object> params) {
        return switch (method.toUpperCase()) {
            case "EQUAL" -> {
                int n = ((Number) params.getOrDefault("units", 3)).intValue();
                yield reliabilityAllocator.equalAllocation(systemMtbf, n);
            }
            case "SCORING" -> {
                @SuppressWarnings("unchecked")
                var scores = (List<Map<String, Object>>) params.get("scores");
                var units = scores.stream().map(m -> {
                    var u = new ReliabilityAllocator.ScoringUnit();
                    u.name = (String) m.get("name");
                    u.complexity = ((Number) m.getOrDefault("complexity", 1)).doubleValue();
                    u.maturity = ((Number) m.getOrDefault("maturity", 1)).doubleValue();
                    u.dutyTime = ((Number) m.getOrDefault("dutyTime", 1)).doubleValue();
                    u.environment = ((Number) m.getOrDefault("environment", 1)).doubleValue();
                    return u;
                }).toList();
                yield reliabilityAllocator.scoringAllocation(systemMtbf, units);
            }
            case "AGREE" -> {
                @SuppressWarnings("unchecked")
                var agreeData = (List<Map<String, Object>>) params.get("units");
                var units = agreeData.stream().map(m -> {
                    var u = new ReliabilityAllocator.AgreeUnit();
                    u.name = (String) m.get("name");
                    u.partCount = ((Number) m.getOrDefault("partCount", 10)).intValue();
                    u.importance = ((Number) m.getOrDefault("importance", 0.8)).doubleValue();
                    u.dutyTime = ((Number) m.getOrDefault("dutyTime", 8)).doubleValue();
                    return u;
                }).toList();
                double missionTime = ((Number) params.getOrDefault("missionTime", 8)).doubleValue();
                yield reliabilityAllocator.agreeAllocation(systemMtbf, units, missionTime);
            }
            default -> throw new IllegalArgumentException("Unknown allocation method: " + method);
        };
    }

    private Long getActiveStageId(Long projectId) {
        try {
            var stage = projectStageMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProjectStage>()
                    .eq(ProjectStage::getProjectId, projectId)
                    .eq(ProjectStage::getStatus, "IN_PROGRESS")
                    .orderByAsc(ProjectStage::getId)
                    .last("LIMIT 1"));
            return stage != null ? stage.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
