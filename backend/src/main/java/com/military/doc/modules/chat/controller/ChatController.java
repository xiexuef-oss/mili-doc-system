package com.military.doc.modules.chat.controller;

import com.military.doc.common.result.Result;
import com.military.doc.modules.chat.service.ChatAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@Tag(name = "对话式文档生成")
public class ChatController {

    private final ChatAgentService agentService;
    private final ConcurrentHashMap<String, Map<String, Object>> tasks = new ConcurrentHashMap<>();

    public ChatController(ChatAgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/message")
    @Operation(summary = "发送对话消息，返回taskId供轮询")
    public Result<Map<String, Object>> sendMessage(@RequestBody Map<String, Object> body,
                                                    Authentication authentication) {
        Long projectId = toLong(body.get("projectId"));
        String sessionId = (String) body.getOrDefault("sessionId", UUID.randomUUID().toString());
        String message = (String) body.get("message");
        Long userId = (Long) authentication.getPrincipal();

        if (projectId == null || message == null) {
            return Result.error("PARAM_ERROR", "projectId and message required");
        }

        String taskId = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("status", "running");
        task.put("progress", "已连接");
        tasks.put(taskId, task);

        CompletableFuture.runAsync(() -> {
            try {
                Consumer<String> onProgress = (step) -> task.put("progress", step);
                Map<String, Object> result = agentService.chat(projectId, sessionId, message, userId, onProgress);
                task.put("status", "done");
                task.put("result", result);
            } catch (Exception e) {
                log.error("Chat agent failed", e);
                task.put("status", "error");
                task.put("error", e.getMessage());
            }
        });

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("taskId", taskId);
        resp.put("status", "running");
        return Result.success(resp);
    }

    @GetMapping("/task/{taskId}")
    @Operation(summary = "轮询任务状态")
    public Result<Map<String, Object>> getTask(@PathVariable String taskId) {
        Map<String, Object> task = tasks.get(taskId);
        if (task == null) return Result.error("NOT_FOUND", "任务不存在");
        return Result.success(task);
    }

    @PostMapping("/generate-all/{projectId}/{stageId}")
    public Result<Map<String, Object>> generateAll(@PathVariable Long projectId, @PathVariable Long stageId,
                                                    Authentication authentication) {
        return Result.success(agentService.generateAllDocs(projectId, stageId, (Long) authentication.getPrincipal()));
    }

    @GetMapping("/quality-report/{projectId}")
    public Result<Map<String, Object>> qualityReport(@PathVariable Long projectId,
                                                      @RequestParam(required = false) Long stageId) {
        return Result.success(agentService.qualityReport(projectId, stageId));
    }

    private Long toLong(Object v) {
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) { try { return Long.parseLong(s); } catch (NumberFormatException e) {} }
        return null;
    }
}
