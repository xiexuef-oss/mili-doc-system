package com.military.doc.ai.util;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke test for TemplateDocxParser — validates structure extraction from a real GJB-style DOCX.
 */
class TemplateDocxParserTest {

    @Test
    void parseRealDocx() throws IOException {
        Path testFile = Path.of(
            System.getProperty("user.dir"),
            "../军工产品研制与质量管理体系手册(标准版).docx"
        );
        if (!Files.exists(testFile)) {
            System.out.println("Test file not found at: " + testFile + " — skipping");
            return;
        }

        byte[] bytes = Files.readAllBytes(testFile);
        TemplateDocxParser parser = new TemplateDocxParser();
        TemplateDocxParser.TemplateStructure result = parser.parse(bytes, testFile.getFileName().toString());

        assertNotNull(result);
        assertNotNull(result.getTitle());
        System.out.println("=== DOCX Parse Result ===");
        System.out.println("Title: " + result.getTitle());
        System.out.println("Security: " + result.getSecurityLevel());
        System.out.println("DocCodeFormat: " + result.getDocCodeFormat());
        System.out.println("HasCover: " + result.isHasCover());
        System.out.println("Chapters: " + result.getChapterTree().size());
        System.out.println("Variables: " + result.getVariables().size());
        System.out.println("Tables: " + result.getTables().size());
        System.out.println("RawText length: " + result.getRawText().length());

        // Print first 3 chapters
        for (TemplateDocxParser.ChapterNode ch : result.flattenChapters().stream().limit(5).toList()) {
            System.out.printf("  Ch[%d]: L%d %s '%s' (hasTable=%s, vars=%d)%n",
                ch.getOrderNum(), ch.getLevel(),
                ch.getNumberingFormat() != null ? ch.getNumberingFormat() : "-",
                ch.getTitle().substring(0, Math.min(60, ch.getTitle().length())),
                ch.isHasTable(), ch.getVariables().size());
        }

        // Print variables
        int count = 0;
        for (var v : result.getVariables()) {
            System.out.printf("  Var: %s [%s]%n", v.getPlaceholder(), v.getType());
            if (++count >= 15) { System.out.println("  ... (" + result.getVariables().size() + " total)"); break; }
        }

        // Assertions
        assertFalse(result.getRawText().isEmpty(), "Full text should not be empty");
        System.out.println("\n✅ TemplateDocxParser smoke test passed");
    }
}
