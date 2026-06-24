package com.military.doc.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.mapper.DocChapterMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DDXML block-level document engine.
 * Six commands: create, replace_block, text_replace, delete_block, text_delete, qa.
 * All operations are precise — no full-document rewrite needed.
 */
@Slf4j
@Service
public class BlockCommandEngine {

    private final DocChapterMapper chapterMapper;
    private final ObjectMapper objectMapper;

    // Tags allowed at document root
    private static final Set<String> ROOT_TAGS = Set.of("title","h2","h3","h4","p","ul","ol","blockquote","table","hr");
    // Tags allowed inside list
    private static final Set<String> LIST_TAGS = Set.of("li");
    // Inline tags
    private static final Set<String> INLINE_TAGS = Set.of("strong","em","del","u","code");
    // Block tags that can contain inline content
    private static final Set<String> CONTAINER_TAGS = Set.of("p","li","th","td","h2","h3","h4","title","blockquote");

    // DDXML tag regex: <tag id="N">content</tag> or self-closing <hr id="N"/>
    private static final Pattern TAG_PATTERN = Pattern.compile(
        "<(title|h[234]|p|ul|ol|li|strong|em|del|u|code|blockquote|table|thead|tbody|tr|th|td|hr)" +
        "\\s+id=\"(\\d+)\"\\s*>(.*?)</\\1>|<hr\\s+id=\"(\\d+)\"\\s*/>",
        Pattern.DOTALL);

    public BlockCommandEngine(DocChapterMapper chapterMapper, ObjectMapper objectMapper) {
        this.chapterMapper = chapterMapper;
        this.objectMapper = objectMapper;
    }

    // ==================== Data types ====================

    public record ContentBlock(int id, String type, String content) {
        public ContentBlock { if (content == null) content = ""; }
        public String toXml() {
            if ("hr".equals(type)) return "<hr id=\"" + id + "\"/>";
            String escaped = content.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
            return "<" + type + " id=\"" + id + "\">" + escaped + "</" + type + ">";
        }
    }

    public record BlockResult(List<ContentBlock> blocks, String plainText, int blockCount) {}

    // ==================== Parse ====================

    /** Parse DDXML string into ContentBlock list. */
    public List<ContentBlock> parse(String xml) {
        if (xml == null || xml.isBlank()) return List.of();
        List<ContentBlock> blocks = new ArrayList<>();
        Matcher m = TAG_PATTERN.matcher(xml);
        int id = 1;
        while (m.find()) {
            String tagName = m.group(1) != null ? m.group(1) : "hr";
            String idStr = m.group(2) != null ? m.group(2) : m.group(4);
            String content = m.group(3) != null ? m.group(3) : "";
            // Unescape
            content = content.replace("&lt;","<").replace("&gt;",">").replace("&amp;","&");
            try { id = Integer.parseInt(idStr); }
            catch (NumberFormatException ignored) {}
            blocks.add(new ContentBlock(id, tagName, content));
            id++;
        }
        return blocks;
    }

    /** Convert blocks to plain text (for search/fulltext/export). */
    public String toPlainText(List<ContentBlock> blocks) {
        StringBuilder sb = new StringBuilder();
        for (ContentBlock b : blocks) {
            if (ROOT_TAGS.contains(b.type) || "li".equals(b.type)) {
                if (sb.length() > 0) sb.append("\n");
                // Strip inline tags from content
                String plain = b.content.replaceAll("<[^>]+>","");
                if ("title".equals(b.type)) sb.append("# ").append(plain).append("\n");
                else if ("h2".equals(b.type)) sb.append("## ").append(plain).append("\n");
                else if ("h3".equals(b.type)) sb.append("### ").append(plain).append("\n");
                else if ("h4".equals(b.type)) sb.append("#### ").append(plain).append("\n");
                else sb.append(plain).append("\n");
            }
        }
        return sb.toString().trim();
    }

    // ==================== Commands ====================

    /** CREATE: Generate full document from DDXML, replace all blocks. */
    public BlockResult createDocument(Long chapterId, String ddxml) {
        List<ContentBlock> blocks = parse(ddxml);
        if (blocks.isEmpty()) return new BlockResult(List.of(), "", 0);

        String plain = toPlainText(blocks);
        try {
            String json = objectMapper.writeValueAsString(blocks);
            DocChapter ch = chapterMapper.selectById(chapterId);
            if (ch != null) {
                ch.setContentBlocks(json);
                ch.setContent(plain);
                ch.setFillStatus("FILLED");
                ch.setFillPercentage(100);
                chapterMapper.updateById(ch);
            }
        } catch (Exception e) { log.error("Failed to save blocks: {}", e.getMessage()); }

        return new BlockResult(blocks, plain, blocks.size());
    }

    /** REPLACE_BLOCK: Replace a single block by id. */
    public ContentBlock replaceBlock(Long chapterId, int blockId, String newXml) {
        List<ContentBlock> blocks = loadBlocks(chapterId);
        ContentBlock replacement = parseSingle(newXml, blockId);
        if (replacement == null) return null;

        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).id == blockId) { blocks.set(i, replacement); break; }
        }
        saveBlocks(chapterId, blocks);
        return replacement;
    }

    /** TEXT_REPLACE: Replace text within a block (keeps structure). */
    public ContentBlock textReplace(Long chapterId, int blockId, String oldText, String newText) {
        List<ContentBlock> blocks = loadBlocks(chapterId);
        for (int i = 0; i < blocks.size(); i++) {
            ContentBlock b = blocks.get(i);
            if (b.id == blockId) {
                ContentBlock updated = new ContentBlock(b.id, b.type, b.content.replace(oldText, newText));
                blocks.set(i, updated);
                saveBlocks(chapterId, blocks);
                return updated;
            }
        }
        return null;
    }

    /** DELETE_BLOCK: Remove a block by id. */
    public boolean deleteBlock(Long chapterId, int blockId) {
        List<ContentBlock> blocks = loadBlocks(chapterId);
        boolean removed = blocks.removeIf(b -> b.id == blockId);
        if (removed) saveBlocks(chapterId, blocks);
        return removed;
    }

    /** TEXT_DELETE: Remove specific text from a block. */
    public ContentBlock textDelete(Long chapterId, int blockId, String textToDelete) {
        List<ContentBlock> blocks = loadBlocks(chapterId);
        for (int i = 0; i < blocks.size(); i++) {
            ContentBlock b = blocks.get(i);
            if (b.id == blockId) {
                ContentBlock updated = new ContentBlock(b.id, b.type, b.content.replace(textToDelete, ""));
                blocks.set(i, updated);
                saveBlocks(chapterId, blocks);
                return updated;
            }
        }
        return null;
    }

    // ==================== Helpers ====================

    public List<ContentBlock> loadBlocks(Long chapterId) {
        DocChapter ch = chapterMapper.selectById(chapterId);
        if (ch == null || ch.getContentBlocks() == null) return new ArrayList<>();
        try {
            List<Map<String,Object>> raw = objectMapper.readValue(ch.getContentBlocks(), new TypeReference<List<Map<String,Object>>>() {});
            List<ContentBlock> result = new ArrayList<>();
            for (Map<String,Object> m : raw) {
                int id = ((Number) m.getOrDefault("id", 0)).intValue();
                String type = (String) m.get("type");
                String content = (String) m.getOrDefault("content", "");
                result.add(new ContentBlock(id, type, content));
            }
            return result;
        } catch (Exception e) { return new ArrayList<>(); }
    }

    private void saveBlocks(Long chapterId, List<ContentBlock> blocks) {
        try {
            String json = objectMapper.writeValueAsString(blocks);
            String plain = toPlainText(blocks);
            DocChapter ch = chapterMapper.selectById(chapterId);
            if (ch != null) {
                ch.setContentBlocks(json);
                ch.setContent(plain);
                chapterMapper.updateById(ch);
            }
        } catch (Exception e) { log.error("Failed to save blocks: {}", e.getMessage()); }
    }

    private ContentBlock parseSingle(String xml, int expectedId) {
        List<ContentBlock> list = parse(xml);
        return list.isEmpty() ? null : new ContentBlock(expectedId, list.get(0).type, list.get(0).content);
    }

    /** Parse a DDXML document and return plain text blocks summary. */
    public BlockResult parseContent(String ddxml) {
        List<ContentBlock> blocks = parse(ddxml);
        return new BlockResult(blocks, toPlainText(blocks), blocks.size());
    }
}
