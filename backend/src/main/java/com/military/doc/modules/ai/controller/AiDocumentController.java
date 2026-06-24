package com.military.doc.modules.ai.controller;

import com.military.doc.common.result.Result;
import com.military.doc.modules.ai.entity.*;
import com.military.doc.modules.ai.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j @RestController @RequestMapping("/api/v1/ai-documents") @RequiredArgsConstructor @Tag(name="AI文档画布")
public class AiDocumentController {
    private final AiDocumentService docService;
    private final AiDocumentSectionService sectionService;
    private final AiCanvasPatchService patchService;
    private final AiDocumentAgentService agentService;

    private Long userId(Authentication a) { return (Long) a.getPrincipal(); }

    /** Ensure the current user owns the document. Throws if not found or not owned. */
    private AiDocument requireOwnership(Long docId, Authentication auth) {
        AiDocument doc = docService.getById(docId);
        if (doc == null) throw new com.military.doc.common.exception.BusinessException("NOT_FOUND", "文档不存在");
        if (!doc.getUserId().equals(userId(auth)))
            throw new com.military.doc.common.exception.BusinessException("FORBIDDEN", "无权操作该文档");
        return doc;
    }

    @PostMapping
    public Result<Map<String,Object>> create(@RequestBody Map<String,Object> body, Authentication auth) {
        String prompt = (String) body.get("prompt");
        String docType = (String) body.getOrDefault("documentType", "");
        Long projectId = toLong(body.get("projectId"));
        if (prompt == null || prompt.isBlank()) return Result.error("PARAM_ERROR","prompt required");
        AiDocument doc = docService.create(userId(auth), projectId, prompt.substring(0,Math.min(100,prompt.length())), docType, prompt);
        return Result.success(Map.of("document",doc,"sections",List.of()));
    }

    @GetMapping
    public Result<List<AiDocument>> list(@RequestParam(required = false) Long projectId, Authentication auth) {
        List<AiDocument> docs = projectId != null
            ? docService.listByProject(projectId)
            : docService.listByUser(userId(auth));
        return Result.success(docs);
    }

    @GetMapping("/{id}")
    public Result<Map<String,Object>> get(@PathVariable Long id) {
        AiDocument doc = docService.getById(id);
        if (doc == null) return Result.error("NOT_FOUND","document not found");
        List<AiDocumentSection> secs = sectionService.getByDocumentId(id);
        return Result.success(Map.of("document",doc,"sections",secs));
    }

    @PostMapping("/{id}/generate")
    public SseEmitter generate(@PathVariable Long id, Authentication auth) {
        AiDocument doc = requireOwnership(id, auth);
        if (doc == null) { SseEmitter e = new SseEmitter(); e.completeWithError(new RuntimeException("not found")); return e; }
        SseEmitter emitter = new SseEmitter(1800000L);
        var securityContext = org.springframework.security.core.context.SecurityContextHolder.getContext();
        CompletableFuture.runAsync(() -> {
            org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);
            // Heartbeat inside async task to ensure emitter is ready
            Thread heartbeatThread = new Thread(() -> {
                try { Thread.sleep(5000); } catch (Exception ignored) {}
                while (!Thread.currentThread().isInterrupted()) {
                    try { Thread.sleep(8000); emitter.send(SseEmitter.event().comment("")); }
                    catch (Exception e) { break; }
                }
            });
            heartbeatThread.setDaemon(true);
            heartbeatThread.start();

            try {
                agentService.generateAllSections(doc, patch -> {
                    try { emitter.send(SseEmitter.event().name("patch").data(patch)); } catch(Exception ex) { /* client disconnected, content still persisted */ }
                }, msg -> {
                    try { emitter.send(SseEmitter.event().name("message").data(msg)); } catch(Exception ignored) {}
                });
                try { emitter.send(SseEmitter.event().name("done").data("{}")); } catch(Exception ignored) {}
                heartbeatThread.interrupt();
                try { emitter.complete(); } catch(Exception ignored) {}
            } catch(Exception e) { heartbeatThread.interrupt(); try { emitter.completeWithError(e); } catch(Exception ig){} }
        });
        return emitter;
    }

    @PatchMapping("/{id}/sections/{sectionId}")
    public Result<?> updateSection(@PathVariable Long id, @PathVariable Long sectionId, @RequestBody Map<String,Object> body, Authentication auth) {
        requireOwnership(id, auth);
        String content = (String) body.get("content"); String title = (String) body.get("title");
        if (content != null) sectionService.updateContent(sectionId, content, null);
        if (title != null) sectionService.rename(sectionId, title);
        return Result.success(Map.of("ok",true));
    }

    @PostMapping("/{id}/sections/{sectionId}/ai-edit")
    public Result<?> aiEditSection(@PathVariable Long id, @PathVariable Long sectionId, @RequestBody Map<String,String> body, Authentication auth) {
        requireOwnership(id, auth);
        String instruction = body.get("instruction"); String mode = body.getOrDefault("mode","rewrite");
        String result = agentService.editSection(sectionId, mode, instruction);
        sectionService.updateContent(sectionId, result, null);
        return Result.success(Map.of("content",result));
    }

    @PostMapping("/{id}/chat")
    public Result<?> chat(@PathVariable Long id, @RequestBody Map<String,String> body, Authentication auth) {
        requireOwnership(id, auth);
        String message = body.get("message");
        if (message == null || message.isBlank()) return Result.error("PARAM_ERROR","message required");
        AiDocument doc = docService.getById(id); if (doc == null) return Result.error("NOT_FOUND","doc not found");
        List<CanvasPatch.Patch> patches = agentService.handleChatInstruction(doc, message, userId(auth));
        patchService.applyPatches(id, userId(auth), patches);
        return Result.success(Map.of("reply","已处理","patches",patches));
    }

    @PostMapping("/{id}/sections")
    public Result<?> addSection(@PathVariable Long id, @RequestBody Map<String,Object> body, Authentication auth) {
        requireOwnership(id, auth);
        String title = (String) body.get("title"); Long parentId = toLong(body.get("parentId"));
        Long afterId = toLong(body.get("afterSectionId")); int level = body.get("level")!=null?((Number)body.get("level")).intValue():1;
        AiDocumentSection sec = sectionService.addAfter(id, title, level, afterId, parentId, userId(auth));
        return Result.success(Map.of("section",sec));
    }

    @DeleteMapping("/{id}/sections/{sectionId}")
    public Result<?> deleteSection(@PathVariable Long id, @PathVariable Long sectionId, Authentication auth) {
        requireOwnership(id, auth);
        sectionService.delete(sectionId); return Result.success(Map.of("ok",true));
    }

    @PatchMapping("/{id}/sections/{sectionId}/move")
    public Result<?> moveSection(@PathVariable Long id, @PathVariable Long sectionId, @RequestBody Map<String,Object> body, Authentication auth) {
        requireOwnership(id, auth);
        int sortOrder = body.get("sortOrder") != null ? ((Number) body.get("sortOrder")).intValue() : 0;
        Long parentId = body.get("parentId") != null ? toLong(body.get("parentId")) : null;
        sectionService.move(sectionId, sortOrder, parentId);
        return Result.success(Map.of("ok", true));
    }

    @PostMapping("/{id}/summarize")
    public Result<?> summarize(@PathVariable Long id, Authentication auth) {
        AiDocument doc = requireOwnership(id, auth);
        List<CanvasPatch.Patch> patches = agentService.handleChatInstruction(doc, "总结这份文档", userId(auth));
        patchService.applyPatches(id, userId(auth), patches);
        // Extract summary text from patches for direct display
        String summary = "";
        for (CanvasPatch.Patch p : patches) {
            if ("update_section_content".equals(p.getType())) {
                Map<String,Object> pl = (Map<String,Object>) p.getPayload();
                if (pl.containsKey("content")) summary = (String) pl.get("content");
            }
        }
        return Result.success(Map.of("summary", summary, "patches", patches));
    }

    @PutMapping("/{id}/content")
    public Result<?> updateContent(@PathVariable Long id, @RequestBody Map<String,Object> body, Authentication auth) {
        AiDocument doc = requireOwnership(id, auth);
        Object cj = body.get("contentJson");
        String cjStr = cj != null ? cj.toString() : null;
        docService.updateContent(id, cjStr);
        return Result.success(Map.of("ok", true));
    }

    @PostMapping("/{id}/ai/edit-selection")
    public Result<?> aiEditSelection(@PathVariable Long id, @RequestBody Map<String,Object> body, Authentication auth) {
        requireOwnership(id, auth);
        // body: { blockId, selectedText, beforeText, afterText, headingPath, mode, instruction }
        String blockId = (String) body.get("blockId");
        String selectedText = (String) body.get("selectedText");
        String beforeText = (String) body.getOrDefault("beforeText", "");
        String afterText = (String) body.getOrDefault("afterText", "");
        String mode = (String) body.getOrDefault("mode", "polish");
        String instruction = (String) body.getOrDefault("instruction", "");

        String system = "你是文档编辑专家。根据上下文编辑选中的文本。操作模式: polish/expand/shorten/rewrite。只输出编辑后的文本。";
        String prompt = "上文: " + beforeText + "\n\n【选中文本】: " + selectedText + "\n\n下文: " + afterText + "\n\n操作: " + mode + (instruction.isEmpty() ? "" : "\n指令: " + instruction) + "\n\n只输出编辑后的文本:";
        
        String result = agentService.quickChat(system, prompt);
        docService.createVersion(id, userId(auth), "ai-edit-selection");
        return Result.success(Map.of("type", "replace_block", "blockId", blockId, "newContent", result));
    }

    @PostMapping("/{id}/ai/insert-at-cursor")
    public Result<?> aiInsertAtCursor(@PathVariable Long id, @RequestBody Map<String,Object> body, Authentication auth) {
        requireOwnership(id, auth);
        String context = (String) body.getOrDefault("context", "");
        String instruction = (String) body.get("instruction");

        String system = "你是文档撰写专家。根据上下文和用户指令，在指定位置插入内容。只输出要插入的文本。";
        String prompt = "上文: " + context + "\n\n用户指令: " + instruction + "\n\n只输出插入的文本:";
        
        String result = agentService.quickChat(system, prompt);
        return Result.success(Map.of("type", "insert_at_cursor", "content", result));
    }

    @GetMapping("/{id}/versions") public Result<List<AiDocumentVersion>> listVersions(@PathVariable Long id, Authentication auth) { requireOwnership(id, auth); return Result.success(docService.listVersions(id)); }

    @PostMapping("/{id}/versions/{versionId}/restore")
    public Result<?> restoreVersion(@PathVariable Long id, @PathVariable Long versionId, Authentication auth) {
        requireOwnership(id, auth);
        docService.createVersion(id, userId(auth), "restore-before");
        AiDocument doc = docService.restoreVersion(id, versionId);
        return doc != null ? Result.success(Map.of("document",doc)) : Result.error("RESTORE_FAILED","restore failed");
    }

    @GetMapping("/{id}/operations") public Result<List<AiCanvasOperation>> listOperations(@PathVariable Long id, Authentication auth) { requireOwnership(id, auth); return Result.success(patchService.listOperations(id)); }

    private Long toLong(Object v) { if(v==null)return null; if(v instanceof Long l)return l; if(v instanceof Integer i)return i.longValue(); try{return Long.parseLong(v.toString());}catch(Exception e){return null;} }
}
