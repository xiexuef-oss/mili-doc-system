package com.military.doc.common.util;

/**
 * Shared string utilities.
 */
public final class Str {

    private Str() {}

    public static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    /** Rough token count: ~3.5 chars per token for Chinese/English mixed text. */
    public static int estimateTokens(String s) {
        if (s == null || s.isEmpty()) return 0;
        return (int) Math.ceil(s.length() / 3.5);
    }

    /** Truncate to maxTokens, appending a notice. */
    public static String truncateByTokens(String s, int maxTokens) {
        if (s == null || s.isEmpty()) return "";
        int maxChars = maxTokens * 3;
        if (s.length() <= maxChars) return s;
        return s.substring(0, maxChars) + "\n\n...(上下文已达到长度限制，已截断)";
    }
}
