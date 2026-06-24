package com.military.doc.modules.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.modules.ai.entity.AiCanvasOperation;
import com.military.doc.modules.ai.entity.AiDocumentSection;
import com.military.doc.modules.ai.mapper.AiCanvasOperationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j @Service @RequiredArgsConstructor
public class AiCanvasPatchService {
    private final AiDocumentService docService;
    private final AiDocumentSectionService sectionService;
    private final AiCanvasOperationMapper operationMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public void applyPatch(Long docId, Long userId, CanvasPatch.Patch patch) {
        String beforeSnapshot = null;
        try {
            // Capture before-snapshot for important operations
            if (isDestructiveOperation(patch.getType())) {
                beforeSnapshot = docService.createVersion(docId, userId, "auto-before-" + patch.getType())
                    .getSnapshot();
            }
        } catch (Exception ignored) {}

        try {
            Map<String, Object> payload = toPayloadMap(patch.getPayload());
            switch (patch.getType()) {
                // ── Document-level ──
                case "create_document" -> {
                    // create_document is handled by AiDocumentService.create(), here just log
                }
                case "set_document_title" ->
                    docService.updateTitle(docId, toString(payload, "title"));
                case "set_document_status" ->
                    docService.updateStatus(docId, toString(payload, "status"));

                // ── Outline ──
                case "set_outline" -> {
                    var sectionsPayload = objectMapper.convertValue(patch.getPayload(), CanvasPatch.SetOutlinePayload.class);
                    sectionService.setOutline(docId, sectionsPayload.getSections(), userId);
                }

                // ── Section CRUD ──
                case "add_section" -> {
                    Map<String, Object> secMap = (Map<String, Object>) payload.get("section");
                    if (secMap == null) secMap = payload;
                    String title = toString(secMap, "title");
                    int level = toInt(secMap, "level", 1);
                    Long afterId = toLong(payload.get("afterSectionId"));
                    Long parentId = toLong(payload.get("parentId"));
                    sectionService.addAfter(docId, title, level, afterId, parentId, userId);
                }
                case "delete_section" ->
                    sectionService.delete(toLong(payload.get("sectionId")));
                case "rename_section" ->
                    sectionService.rename(toLong(payload.get("sectionId")), toString(payload, "title"));
                case "move_section" -> {
                    Long sectionId = toLong(payload.get("sectionId"));
                    int sortOrder = toInt(payload, "sortOrder", 0);
                    AiDocumentSection sec = sectionService.getByDocumentId(docId).stream()
                        .filter(s -> s.getId().equals(sectionId)).findFirst().orElse(null);
                    if (sec != null) {
                        sec.setSortOrder(sortOrder);
                        sectionService.rename(sectionId, sec.getTitle()); // trigger update
                    }
                }

                // ── Section Content ──
                case "update_section_content" -> {
                    Long sid = toLong(payload.get("sectionId"));
                    String content = toString(payload, "content");
                    Object cj = payload.get("contentJson");
                    String cjStr = cj != null ? objectMapper.writeValueAsString(cj) : null;
                    sectionService.updateContent(sid, content, cjStr);
                }
                case "append_section_content" -> {
                    Long sid = toLong(payload.get("sectionId"));
                    String delta = toString(payload, "delta");
                    if (delta != null && !delta.isBlank()) {
                        AiDocumentSection sec = sectionService.getByDocumentId(docId).stream()
                            .filter(s -> s.getId().equals(sid)).findFirst().orElse(null);
                        if (sec != null) {
                            String newContent = (sec.getContent() != null ? sec.getContent() : "") + delta;
                            sectionService.updateContent(sid, newContent, null);
                        }
                    }
                }

                // ── Section Status ──
                case "update_section_status" ->
                    sectionService.updateStatus(toLong(payload.get("sectionId")), toString(payload, "status"));

                default ->
                    log.warn("Unknown patch type: {}", patch.getType());
            }

            // Record successful operation
            recordOperation(docId, userId, patch.getType(), List.of(patch), "success", null);

        } catch (Exception e) {
            log.error("Patch [{}] failed: {}", patch.getType(), e.getMessage());
            recordOperation(docId, userId, patch.getType(), List.of(patch), "failed", e.getMessage());
            throw new RuntimeException("Patch execution failed: " + patch.getType(), e);
        }
    }

    @Transactional
    public void applyPatches(Long docId, Long userId, List<CanvasPatch.Patch> patches) {
        for (CanvasPatch.Patch p : patches) {
            applyPatch(docId, userId, p);
        }
    }

    public List<AiCanvasOperation> listOperations(Long docId) {
        return operationMapper.selectList(
            new LambdaQueryWrapper<AiCanvasOperation>()
                .eq(AiCanvasOperation::getDocumentId, docId)
                .orderByDesc(AiCanvasOperation::getCreatedAt));
    }

    // ──────────────── Private helpers ────────────────

    private void recordOperation(Long docId, Long userId, String opType,
                                  List<CanvasPatch.Patch> patches, String status, String errorMsg) {
        try {
            AiCanvasOperation op = new AiCanvasOperation();
            op.setDocumentId(docId);
            op.setUserId(userId);
            op.setOperationType(opType);
            op.setPatches(objectMapper.writeValueAsString(patches));
            op.setStatus(status);
            op.setErrorMessage(errorMsg);
            operationMapper.insert(op);
        } catch (Exception e) {
            log.error("Failed to record operation: {}", e.getMessage());
        }
    }

    private boolean isDestructiveOperation(String type) {
        return "delete_section".equals(type) || "rewrite_all".equals(type)
            || "set_outline".equals(type) || "restore_version".equals(type);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toPayloadMap(Object payload) {
        if (payload instanceof Map) return (Map<String, Object>) payload;
        return objectMapper.convertValue(payload, Map.class);
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Long l) return l;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }

    private String toString(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : null;
    }

    private int toInt(Map<String, Object> m, String key, int defaultValue) {
        Object v = m.get(key);
        if (v instanceof Number n) return n.intValue();
        try { return v != null ? Integer.parseInt(v.toString()) : defaultValue; }
        catch (Exception e) { return defaultValue; }
    }
}
