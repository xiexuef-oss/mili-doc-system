package com.military.doc.ai.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class TextSplitter {

    /** Match GJB-style chapter headings: 第X章, 第XX章, CHAPTER, #/##/### headings */
    private static final Pattern CHAPTER_PATTERN = Pattern.compile(
        "^\\s*(?:第[一二三四五六七八九十百零\\d]+章|CHAPTER\\s+\\d+|#{1,3}\\s+)",
        Pattern.MULTILINE
    );

    /** Match numbered section: 1. 1.1 1.1.1 */
    private static final Pattern NUMBERED_SECTION = Pattern.compile(
        "^\\s*\\d+(?:\\.\\d+)*\\s+",
        Pattern.MULTILINE
    );

    private static final int MIN_CHUNK_SIZE = 50;
    private static final int MAX_CHUNK_SIZE = 16000; // 内网部署，增大chunk

    public List<TextChunk> split(String text, String defaultCategory) {
        List<TextChunk> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        // Try chapter-level split first
        String[] parts = CHAPTER_PATTERN.split(text);
        if (parts.length >= 2) {
            // Re-extract titles from the original text
            var matcher = CHAPTER_PATTERN.matcher(text);
            List<String> titles = new ArrayList<>();
            while (matcher.find()) {
                String title = matcher.group().trim();
                // Also grab the rest of the heading line
                int end = matcher.end();
                int lineEnd = text.indexOf('\n', end);
                if (lineEnd > end && lineEnd - end < 100) {
                    title = title + " " + text.substring(end, lineEnd).trim();
                }
                titles.add(title);
            }

            for (int i = 0; i < parts.length; i++) {
                String body = parts[i].trim();
                if (body.length() < MIN_CHUNK_SIZE) continue;
                String title = i < titles.size() ? titles.get(i) : defaultCategory;
                if (body.length() > MAX_CHUNK_SIZE) {
                    body = body.substring(0, MAX_CHUNK_SIZE);
                }
                chunks.add(new TextChunk(title, body));
            }
        } else {
            // Try numbered sections
            parts = NUMBERED_SECTION.split(text);
            if (parts.length >= 2) {
                var matcher = NUMBERED_SECTION.matcher(text);
                List<String> titles = new ArrayList<>();
                while (matcher.find()) {
                    titles.add(matcher.group().trim());
                }
                for (int i = 0; i < parts.length; i++) {
                    String body = parts[i].trim();
                    if (body.length() < MIN_CHUNK_SIZE) continue;
                    String title = i < titles.size()
                        ? defaultCategory + " - " + titles.get(i)
                        : defaultCategory;
                    if (body.length() > MAX_CHUNK_SIZE) {
                        body = body.substring(0, MAX_CHUNK_SIZE);
                    }
                    chunks.add(new TextChunk(title, body));
                }
            } else {
                // Fallback: split by double newlines, use first line as title
                String[] paragraphs = text.split("\\n\\s*\\n");
                StringBuilder buf = new StringBuilder();
                for (String para : paragraphs) {
                    String trimmed = para.trim();
                    if (trimmed.isEmpty()) continue;
                    if (buf.length() + trimmed.length() > MAX_CHUNK_SIZE && buf.length() > MIN_CHUNK_SIZE) {
                        TextChunk chunk = makeChunk(buf.toString(), defaultCategory);
                        if (chunk != null) chunks.add(chunk);
                        buf.setLength(0);
                    }
                    buf.append(trimmed).append("\n\n");
                }
                if (buf.length() > MIN_CHUNK_SIZE) {
                    TextChunk chunk = makeChunk(buf.toString(), defaultCategory);
                    if (chunk != null) chunks.add(chunk);
                } else if (!chunks.isEmpty() && buf.length() > 0) {
                    // Append small remainder to last chunk
                    TextChunk last = chunks.get(chunks.size() - 1);
                    last.setContent(last.content() + "\n\n" + buf.toString().trim());
                }
            }
        }

        if (chunks.isEmpty()) {
            // Single-chunk fallback
            String title = text.lines().findFirst().map(l -> l.trim()).orElse(defaultCategory);
            if (title.length() > 100) title = title.substring(0, 100);
            String body = text.length() > MAX_CHUNK_SIZE ? text.substring(0, MAX_CHUNK_SIZE) : text;
            chunks.add(new TextChunk(title, body));
        }

        log.info("TextSplitter: split {} chars into {} chunks", text.length(), chunks.size());
        return chunks;
    }

    private TextChunk makeChunk(String body, String defaultCategory) {
        String title = body.lines().findFirst().map(l -> l.trim()).orElse(defaultCategory);
        if (title.length() > 100) title = title.substring(0, 100);
        return new TextChunk(title, body.trim());
    }

    // ---- Metadata-aware chunk result ----

    public List<TextChunk> split(String text, String defaultCategory, String sourceFileName, Integer pageNumber) {
        List<TextChunk> chunks = split(text, defaultCategory);
        for (TextChunk chunk : chunks) {
            chunk.setSourceFile(sourceFileName);
            chunk.setPageNumber(pageNumber);
        }
        return chunks;
    }

    public static class TextChunk {
        private final String title;
        private String content;
        // 元数据增强
        private String sourceFile;
        private Integer pageNumber;
        private String chapterPath;
        private List<String> detectedStandardRefs;

        public TextChunk(String title, String content) {
            this.title = title;
            this.content = content;
        }

        public String title() { return title; }
        public String content() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getSourceFile() { return sourceFile; }
        public void setSourceFile(String f) { this.sourceFile = f; }
        public Integer getPageNumber() { return pageNumber; }
        public void setPageNumber(Integer p) { this.pageNumber = p; }
        public String getChapterPath() { return chapterPath; }
        public void setChapterPath(String p) { this.chapterPath = p; }
        public List<String> getDetectedStandardRefs() { return detectedStandardRefs; }
        public void setDetectedStandardRefs(List<String> refs) { this.detectedStandardRefs = refs; }

        /** Generate structured metadata for embedding */
        public String toEmbeddingText() {
            StringBuilder sb = new StringBuilder();
            if (sourceFile != null) sb.append("[来源] ").append(sourceFile).append("\n");
            if (pageNumber != null) sb.append("[页码] ").append(pageNumber).append("\n");
            if (chapterPath != null) sb.append("[路径] ").append(chapterPath).append("\n");
            if (detectedStandardRefs != null && !detectedStandardRefs.isEmpty()) {
                sb.append("[标准] ").append(String.join(", ", detectedStandardRefs)).append("\n");
            }
            sb.append("[标题] ").append(title).append("\n");
            sb.append("[内容] ").append(content);
            return sb.toString();
        }
    }
}
