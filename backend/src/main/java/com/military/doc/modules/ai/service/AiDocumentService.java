package com.military.doc.modules.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.modules.ai.entity.*;
import com.military.doc.modules.ai.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Slf4j @Service
public class AiDocumentService extends com.baomidou.mybatisplus.extension.service.impl.ServiceImpl<AiDocumentMapper, AiDocument> {
    private final AiDocumentSectionMapper sectionMapper;
    private final AiDocumentVersionMapper versionMapper;
    private final ObjectMapper objectMapper;

    public AiDocumentService(AiDocumentSectionMapper sectionMapper,
                              AiDocumentVersionMapper versionMapper, ObjectMapper objectMapper) {
        this.sectionMapper = sectionMapper;
        this.versionMapper = versionMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional public AiDocument create(Long userId, Long projectId, String title, String docType, String prompt) {
        AiDocument d = new AiDocument();
        d.setUserId(userId);
        d.setProjectId(projectId);
        d.setTitle(title);
        d.setDocumentType(docType);
        d.setSourcePrompt(prompt);
        d.setStatus("draft");
        d.setDeleted(0);  // Explicitly set to not-deleted for @TableLogic
        d.setContentJson(null);
        d.setOutlineJson(null);
        d.setMetadata(null);
        boolean ok = this.save(d);
        log.info("Created AiDocument id={}, title={}, saveOk={}", d.getId(), d.getTitle(), ok);
        return d;
    }
    public AiDocument getById(Long id) { return super.getById(id); }

    @Transactional public void updateTitle(Long id, String title) {
        AiDocument d = this.getById(id);
        if (d != null) { d.setTitle(title); this.updateById(d); }
    }

    @Transactional public void updateContent(Long id, String contentJson) {
        AiDocument d = this.getById(id);
        if (d != null) {
            d.setContentJson(contentJson);
            d.setOutlineJson(extractOutlineFromJson(contentJson));
            this.updateById(d);
        }
    }

    @Transactional public void updateStatus(Long id, String s) {
        AiDocument d = this.getById(id);
        if (d != null) { d.setStatus(s); this.updateById(d); }
    }

    @Transactional public void delete(Long id) { this.removeById(id); }

    public List<AiDocument> listByUser(Long uid) {
        return this.list(new LambdaQueryWrapper<AiDocument>()
            .eq(AiDocument::getUserId, uid)
            .orderByDesc(AiDocument::getUpdatedAt));
    }
    public List<AiDocument> listByProject(Long pid) {
        return this.list(new LambdaQueryWrapper<AiDocument>()
            .eq(AiDocument::getProjectId, pid)
            .orderByDesc(AiDocument::getUpdatedAt));
    }

    @Transactional public AiDocumentVersion createVersion(Long docId, Long userId, String reason) {
        AiDocument doc = this.getById(docId);
        if (doc == null) return null;
        List<AiDocumentSection> secs = sectionMapper.selectList(
            new LambdaQueryWrapper<AiDocumentSection>().eq(AiDocumentSection::getDocumentId, docId));
        try {
            Map<String, Object> snap = Map.of("document", doc, "sections", secs);
            AiDocumentVersion v = new AiDocumentVersion();
            v.setDocumentId(docId); v.setUserId(userId);
            v.setTitle(doc.getTitle()); v.setReason(reason);
            v.setSnapshot(objectMapper.writeValueAsString(snap));
            versionMapper.insert(v);
            return v;
        } catch (Exception e) {
            log.error("version failed", e);
            return null;
        }
    }

    public List<AiDocumentVersion> listVersions(Long docId) {
        return versionMapper.selectList(
            new LambdaQueryWrapper<AiDocumentVersion>()
                .eq(AiDocumentVersion::getDocumentId, docId)
                .orderByDesc(AiDocumentVersion::getCreatedAt));
    }

    @Transactional public AiDocument restoreVersion(Long docId, Long versionId) {
        AiDocumentVersion v = versionMapper.selectById(versionId);
        if (v == null || !v.getDocumentId().equals(docId)) return null;
        try {
            Map<String, Object> snap = objectMapper.readValue(v.getSnapshot(), Map.class);
            sectionMapper.delete(new LambdaQueryWrapper<AiDocumentSection>()
                .eq(AiDocumentSection::getDocumentId, docId));
            List<Map<String, Object>> sl = (List<Map<String, Object>>) snap.get("sections");
            if (sl != null) for (Map<String, Object> s : sl) {
                AiDocumentSection sec = objectMapper.convertValue(s, AiDocumentSection.class);
                sec.setId(null);
                sectionMapper.insert(sec);
            }
            return this.getById(docId);
        } catch (Exception e) {
            log.error("restore failed", e);
            return null;
        }
    }

    // ============== Helper methods for content conversion ==============

    
    private void walkContentText(List<Map<String, Object>> nodes, StringBuilder sb) {
        for (Map<String, Object> node : nodes) {
            String type = (String) node.get("type");
            if ("text".equals(type)) {
                sb.append(node.get("text"));
            } else if ("hardBreak".equals(type) || "paragraph".equals(type)) {
                sb.append("\n");
            }
            List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("content");
            if (children != null) {
                walkContentText(children, sb);
            }
        }
    }

    
    private void walkContentHtml(List<Map<String, Object>> nodes, StringBuilder sb) {
        for (Map<String, Object> node : nodes) {
            String type = (String) node.get("type");
            if ("paragraph".equals(type)) {
                sb.append("<p>");
                walkContentHtml(getChildren(node), sb);
                sb.append("</p>");
            } else if ("heading".equals(type)) {
                Map<String, Object> attrs = (Map<String, Object>) node.get("attrs");
                int level = attrs != null ? ((Number) attrs.getOrDefault("level", 1)).intValue() : 1;
                sb.append("<h").append(level).append(">");
                walkContentHtml(getChildren(node), sb);
                sb.append("</h").append(level).append(">");
            } else if ("text".equals(type)) {
                sb.append(escapeHtml((String) node.get("text")));
            } else if ("bulletList".equals(type)) {
                sb.append("<ul>");
                walkContentHtml(getChildren(node), sb);
                sb.append("</ul>");
            } else if ("orderedList".equals(type)) {
                sb.append("<ol>");
                walkContentHtml(getChildren(node), sb);
                sb.append("</ol>");
            } else if ("listItem".equals(type)) {
                sb.append("<li>");
                walkContentHtml(getChildren(node), sb);
                sb.append("</li>");
            } else if ("blockquote".equals(type)) {
                sb.append("<blockquote>");
                walkContentHtml(getChildren(node), sb);
                sb.append("</blockquote>");
            } else if ("codeBlock".equals(type)) {
                sb.append("<pre><code>");
                walkContentHtml(getChildren(node), sb);
                sb.append("</code></pre>");
            } else if ("table".equals(type)) {
                sb.append("<table>");
                walkContentHtml(getChildren(node), sb);
                sb.append("</table>");
            } else if ("tableRow".equals(type)) {
                sb.append("<tr>");
                walkContentHtml(getChildren(node), sb);
                sb.append("</tr>");
            } else if ("tableCell".equals(type) || "tableHeader".equals(type)) {
                String tag = "tableHeader".equals(type) ? "th" : "td";
                sb.append("<").append(tag).append(">");
                walkContentHtml(getChildren(node), sb);
                sb.append("</").append(tag).append(">");
            } else if ("hardBreak".equals(type)) {
                sb.append("<br/>");
            } else {
                // Unknown node type, still walk children
                walkContentHtml(getChildren(node), sb);
            }
        }
    }

    private List<Map<String, Object>> getChildren(Map<String, Object> node) {
        List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("content");
        return children != null ? children : List.of();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Package-private (not private): reused by AiDocumentWorkflowService.saveToDocLedger for node types it doesn't special-case itself. */
    void walkContentMarkdown(List<Map<String, Object>> nodes, StringBuilder sb) {
        for (Map<String, Object> node : nodes) {
            String type = (String) node.get("type");
            if ("paragraph".equals(type)) {
                walkContentMarkdown(getChildren(node), sb);
                sb.append("\n\n");
            } else if ("heading".equals(type)) {
                Map<String, Object> attrs = (Map<String, Object>) node.get("attrs");
                int level = attrs != null ? ((Number) attrs.getOrDefault("level", 1)).intValue() : 1;
                for (int i = 0; i < level; i++) sb.append("#");
                sb.append(" ");
                walkContentMarkdown(getChildren(node), sb);
                sb.append("\n\n");
            } else if ("text".equals(type)) {
                sb.append(node.get("text"));
            } else if ("bulletList".equals(type)) {
                for (Map<String, Object> item : getChildren(node)) {
                    sb.append("- ");
                    walkContentMarkdown(getChildren(item), sb);
                    sb.append("\n");
                }
                sb.append("\n");
            } else if ("orderedList".equals(type)) {
                int idx = 1;
                for (Map<String, Object> item : getChildren(node)) {
                    sb.append(idx++).append(". ");
                    walkContentMarkdown(getChildren(item), sb);
                    sb.append("\n");
                }
                sb.append("\n");
            } else if ("blockquote".equals(type)) {
                sb.append("> ");
                walkContentMarkdown(getChildren(node), sb);
                sb.append("\n\n");
            } else if ("codeBlock".equals(type)) {
                sb.append("```\n");
                walkContentMarkdown(getChildren(node), sb);
                sb.append("\n```\n\n");
            } else if ("table".equals(type)) {
                for (Map<String, Object> row : getChildren(node)) {
                    List<Map<String, Object>> cells = getChildren(row);
                    boolean isHeaderRow = !cells.isEmpty() && "tableHeader".equals(cells.get(0).get("type"));
                    sb.append("|");
                    for (Map<String, Object> cell : cells) {
                        StringBuilder cellSb = new StringBuilder();
                        walkContentMarkdown(getChildren(cell), cellSb);
                        sb.append(" ").append(cellSb.toString().trim().replaceAll("\\s*\\n+\\s*", " ")).append(" |");
                    }
                    sb.append("\n");
                    if (isHeaderRow) {
                        sb.append("|").append(" --- |".repeat(cells.size())).append("\n");
                    }
                }
                sb.append("\n");
            } else if ("hardBreak".equals(type)) {
                sb.append("\n");
            } else {
                walkContentMarkdown(getChildren(node), sb);
            }
        }
    }

    private String extractOutlineFromJson(String contentJson) {
        if (contentJson == null || contentJson.isBlank()) return "[]";
        try {
            List<Map<String, Object>> outline = new ArrayList<>();
            Map<String, Object> doc = objectMapper.readValue(contentJson, Map.class);
            List<Map<String, Object>> content = (List<Map<String, Object>>) doc.get("content");
            if (content != null) {
                int pos = 0;
                for (Map<String, Object> node : content) {
                    if ("heading".equals(node.get("type"))) {
                        Map<String, Object> attrs = (Map<String, Object>) node.get("attrs");
                        int level = attrs != null ? ((Number) attrs.getOrDefault("level", 1)).intValue() : 1;
                        String id = attrs != null && attrs.containsKey("id") ? attrs.get("id").toString() : null;
                        StringBuilder textSb = new StringBuilder();
                        walkContentText(getChildren(node), textSb);
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", id);
                        item.put("text", textSb.toString());
                        item.put("level", level);
                        item.put("pos", pos);
                        outline.add(item);
                    }
                    pos++;
                }
            }
            return objectMapper.writeValueAsString(outline);
        } catch (Exception e) {
            log.warn("extractOutlineFromJson failed", e);
            return "[]";
        }
    }
}
