package com.military.doc.ai.util;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse AI-generated markdown into structured chapters.
 * Extracts heading hierarchy and section content from markdown text.
 */
@Slf4j
public class MarkdownChapterParser {

    /** Matches markdown headings: # Title, ##Title, ### 1. Scope, etc. Space after # is optional. */
    private static final Pattern HEADING = Pattern.compile("^(#{1,5})\\s*(.+)$", Pattern.MULTILINE);

    /**
     * A parsed section with its heading level, title, and body content.
     */
    public record ParsedSection(int level, String number, String title, String content, List<ParsedSection> children) {
        public String fullTitle() {
            return (number != null ? number + " " : "") + title;
        }
    }

    /** Matches markdown headings NOT at line start: inserts newline before them. */
    private static final Pattern INLINE_HEADING = Pattern.compile("(?<=[^\\n\\r])#{1,5}\\s*\\S");

    /**
     * Parse markdown content into a tree of sections based on heading hierarchy.
     *
     * @param markdown the full markdown text
     * @return list of top-level sections (each may have nested children)
     */
    public static List<ParsedSection> parse(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return List.of();
        }

        // Pre-process: ensure headings are on their own lines.
        // Fixes AI output like "#Title1###1.1 Subtitle" where headings run together.
        Matcher inline = INLINE_HEADING.matcher(markdown);
        StringBuffer sb = new StringBuffer();
        while (inline.find()) {
            inline.appendReplacement(sb, "\n" + inline.group());
        }
        inline.appendTail(sb);
        String normalized = sb.toString();

        // Split text into segments by headings
        List<Segment> segments = new ArrayList<>();
        Matcher m = HEADING.matcher(normalized);

        int lastEnd = 0;
        while (m.find()) {
            int level = m.group(1).length();
            String title = m.group(2).trim();
            // Strip any leftover # markers that may appear in group(2)
            title = title.replaceAll("^#{1,5}\\s*", "");

            // Extract chapter number from title (e.g., "1 范围" or "1范围" → number="1", title="范围")
            String number = null;
            String cleanTitle = title;
            Matcher numMatcher = Pattern.compile("^([0-9]+(?:\\.[0-9]+)*)\\s*(.*)$").matcher(title);
            if (numMatcher.find()) {
                number = numMatcher.group(1);
                cleanTitle = numMatcher.group(2);
            }
            // Truncate overly long titles — AI sometimes puts heading+body on one line
            if (cleanTitle.length() > 100) {
                cleanTitle = cleanTitle.substring(0, 100) + "...";
            }

            // Content before this heading (if any) is preamble
            segments.add(new Segment(level, number, cleanTitle, m.start(), m.end()));
            lastEnd = m.end();
        }

        // Build tree
        List<ParsedSection> roots = new ArrayList<>();
        Deque<ParsedSection> stack = new ArrayDeque<>();

        for (int i = 0; i < segments.size(); i++) {
            Segment seg = segments.get(i);
            // Content runs from end of this heading to start of next (or end of doc)
            int contentStart = seg.headingEnd;
            int contentEnd = (i + 1 < segments.size()) ? segments.get(i + 1).headingStart : normalized.length();
            String bodyContent = normalized.substring(contentStart, contentEnd).trim();

            ParsedSection section = new ParsedSection(seg.level, seg.number, seg.title, bodyContent, new ArrayList<>());

            // Pop stack until we find a parent (level < current level)
            while (!stack.isEmpty() && stack.peek().level() >= seg.level) {
                stack.pop();
            }

            if (stack.isEmpty()) {
                roots.add(section);
            } else {
                stack.peek().children().add(section);
            }

            stack.push(section);
        }

        log.debug("Parsed markdown: {} top-level sections, {} total segments", roots.size(), segments.size());
        return roots;
    }

    /**
     * Flatten the section tree into a list ordered by depth-first traversal,
     * assigning sequential order numbers and tracking parent-child relationships.
     */
    public static List<FlatSection> flatten(List<ParsedSection> roots) {
        List<FlatSection> flat = new ArrayList<>();
        flattenRecursive(roots, flat, 0, new int[]{0}, -1);
        return flat;
    }

    private static void flattenRecursive(List<ParsedSection> sections, List<FlatSection> flat,
                                          int depth, int[] counter, int parentIndex) {
        for (ParsedSection sec : sections) {
            counter[0]++;
            int myIndex = flat.size();
            flat.add(new FlatSection(sec, depth, counter[0], parentIndex));
            flattenRecursive(sec.children(), flat, depth + 1, counter, myIndex);
        }
    }

    /**
     * Flattened section with parent index for hierarchy reconstruction.
     * parentIndex = -1 means root-level section.
     */
    public record FlatSection(ParsedSection section, int depth, int orderNum, int parentIndex) {

        /** For backward compatibility. */
        public FlatSection(ParsedSection section, int depth, int orderNum) {
            this(section, depth, orderNum, -1);
        }

        /** Get the FlatSection index to use as DB parentId. Returns 0 for root-level items. */
        public int parentFlatIndex() {
            return parentIndex >= 0 ? parentIndex : -1;
        }
    }

    // ---- Internal ----

    private static class Segment {
        final int level;
        final String number;
        final String title;
        final int headingStart;
        final int headingEnd;

        Segment(int level, String number, String title, int headingStart, int headingEnd) {
            this.level = level;
            this.number = number;
            this.title = title;
            this.headingStart = headingStart;
            this.headingEnd = headingEnd;
        }
    }
}
