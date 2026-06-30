package com.military.doc.ai.util;

/**
 * Shared utilities for cleaning and extracting structured data from LLM outputs.
 * Consolidates ~30 duplicated code blocks previously scattered across 15+ services.
 *
 * <p>Usage is intentionally static — these are pure functions with no dependencies,
 * so they can be called from anywhere without Spring injection.</p>
 */
public final class LlmOutputCleaner {

    private LlmOutputCleaner() {
        // utility class
    }

    // ── Markdown / artifact cleaning ──────────────────────────────────────

    /**
     * Clean common LLM output artifacts: leading/trailing whitespace,
     * markdown heading lines (LLMs sometimes generate a full document header
     * instead of just the requested section body), and leading {@code "null"}
     * strings that some models prepend to structured output.
     */
    public static String clean(String raw) {
        if (raw == null) return "";
        String result = raw.trim();
        // Remove ALL markdown heading lines
        result = result.replaceAll("(?m)^#{1,4}\\s+[^\\n]*\\n?", "").trim();
        result = stripLeadingNull(result);
        return result;
    }

    /**
     * Strip leading literal {@code "null"} strings (one or more repetitions).
     * Some LLMs prepend {@code "nullnull{...}"} to structured JSON output.
     */
    public static String stripLeadingNull(String raw) {
        if (raw == null) return "";
        return raw.replaceFirst("^(null)+", "").trim();
    }

    // ── JSON extraction ────────────────────────────────────────────────────

    /**
     * Extract the outermost JSON object ({@code {...}}) from LLM output,
     * optionally stripping markdown code fences first.
     *
     * @param response     raw LLM response text
     * @param stripFences  if true, remove {@code ```json} / {@code ```} fences before extraction
     * @return the extracted JSON substring, or the original text if no braces found
     */
    public static String extractJsonObject(String response, boolean stripFences) {
        if (response == null || response.isBlank()) return response;
        String text = stripFences ? stripMarkdownFences(response) : response;
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    /**
     * Extract the outermost JSON array ({@code [...]}) from LLM output,
     * optionally stripping markdown code fences first.
     *
     * @param response     raw LLM response text
     * @param stripFences  if true, remove {@code ```json} / {@code ```} fences before extraction
     * @return the extracted JSON substring, or the original text if no brackets found
     */
    public static String extractJsonArray(String response, boolean stripFences) {
        if (response == null || response.isBlank()) return response;
        String text = stripFences ? stripMarkdownFences(response) : response;
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    /**
     * Extract JSON text between arbitrary open/close delimiters,
     * with optional markdown fence removal.
     */
    public static String extractBetween(String raw, char open, char close, boolean stripFences) {
        if (raw == null || raw.isBlank()) return raw;
        String text = stripFences ? stripMarkdownFences(raw) : raw;
        int s = text.indexOf(open);
        int e = text.lastIndexOf(close);
        if (s >= 0 && e > s) {
            return text.substring(s, e + 1);
        }
        return text;
    }

    // ── Internal helpers ───────────────────────────────────────────────────

    private static String stripMarkdownFences(String text) {
        return text.trim()
            .replaceAll("```json\\s*", "")
            .replaceAll("```\\s*", "")
            .trim();
    }
}
