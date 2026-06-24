package com.military.doc.ai.controller;

import com.military.doc.ai.service.LayeredGenerationService;
import com.military.doc.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Layered 3-step generation: outline → chapters → assemble+review.
 * Implements Section 4.3 of requirements-v2.0.md.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/layered")
@Tag(name = "分层生成")
public class LayeredGenerationController {

    private final LayeredGenerationService layeredService;

    public LayeredGenerationController(LayeredGenerationService layeredService) {
        this.layeredService = layeredService;
    }

    @PostMapping("/outline/preview")
    @Operation(summary = "Step1: 生成文档大纲（章节标题+摘要+关键要点）")
    public Result<LayeredGenerationService.OutlineResult> previewOutline(
            @RequestBody Map<String, Object> body) {
        Long docLedgerId = toLong(body.get("docLedgerId"));
        Long projectId = toLong(body.get("projectId"));
        return Result.success(layeredService.generateOutline(docLedgerId, projectId));
    }

    @GetMapping("/outline/stream")
    @Operation(summary = "Step1: 流式生成文档大纲")
    public SseEmitter streamOutline(@RequestParam Long docLedgerId,
                                     @RequestParam Long projectId) {
        SseEmitter emitter = new SseEmitter(120000L);
        CompletableFuture.runAsync(() -> {
            try {
                layeredService.generateOutlineStream(docLedgerId, projectId,
                    chunk -> {
                        try { emitter.send(SseEmitter.event().name("chunk").data(chunk)); }
                        catch (Exception e) { throw new RuntimeException("SSE_ABORT"); }
                    },
                    result -> {
                        try {
                            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result);
                            emitter.send(SseEmitter.event().name("done").data(json));
                            emitter.complete();
                        } catch (Exception ignored) {}
                    });
            } catch (Exception e) {
                try { emitter.completeWithError(e); } catch (Exception ignored) {}
            }
        });
        return emitter;
    }

    @GetMapping("/chapters/stream")
    @Operation(summary = "Step2: 逐章生成内容（每章含AI_META标记）")
    public SseEmitter streamChapters(@RequestParam Long docLedgerId,
                                      @RequestParam Long projectId) {
        SseEmitter emitter = new SseEmitter(1800000L);
        CompletableFuture.runAsync(() -> {
            try {
                layeredService.generateChaptersStream(docLedgerId, projectId,
                    chunk -> {
                        try { emitter.send(SseEmitter.event().name("chunk").data(chunk)); }
                        catch (Exception e) { throw new RuntimeException("SSE_ABORT"); }
                    },
                    chapterResult -> {
                        try {
                            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(chapterResult);
                            emitter.send(SseEmitter.event().name("chapter").data(json));
                        } catch (Exception ignored) {}
                    },
                    completed -> {
                        try {
                            emitter.send(SseEmitter.event().name("progress")
                                .data("{\"completed\":" + completed + "}"));
                        } catch (Exception ignored) {}
                    });
                emitter.send(SseEmitter.event().name("done").data("complete"));
                emitter.complete();
            } catch (Exception e) {
                try { emitter.completeWithError(e); } catch (Exception ignored) {}
            }
        });
        return emitter;
    }

    @PostMapping("/assemble")
    @Operation(summary = "Step3: 组装全文+自动审查+生成补充清单")
    public Result<LayeredGenerationService.AssembleResult> assembleAndReview(
            @RequestBody Map<String, Object> body) {
        Long docLedgerId = toLong(body.get("docLedgerId"));
        Long projectId = toLong(body.get("projectId"));
        return Result.success(layeredService.assembleAndReview(docLedgerId, projectId));
    }

    @GetMapping("/state/{docLedgerId}")
    @Operation(summary = "查询当前生成状态（处于哪一步）")
    public Result<LayeredGenerationService.GenerationState> getState(
            @PathVariable Long docLedgerId) {
        return Result.success(layeredService.getState(docLedgerId));
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long l) return l;
        if (value instanceof Integer i) return i.longValue();
        try { return Long.parseLong(value.toString()); }
        catch (NumberFormatException e) { return null; }
    }
}
