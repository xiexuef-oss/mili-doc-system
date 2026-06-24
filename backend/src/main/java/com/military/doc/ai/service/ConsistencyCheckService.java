package com.military.doc.ai.service;

import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.mapper.DocChapterMapper;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cross-document consistency check — content-level comparison.
 * Loads rules from dependencies.yml, validates numerical consistency
 * between related documents (MTBF, reliability metrics, etc.).
 *
 * Implements Section 7.2-7.3 and Section 4.2 consistency requirements.
 */
@Slf4j
@Service
public class ConsistencyCheckService {

    private final DocLedgerMapper docLedgerMapper;
    private final DocChapterMapper chapterMapper;
    private Map<String, Object> depConfig = Map.of();

    public ConsistencyCheckService(DocLedgerMapper docLedgerMapper,
                                    DocChapterMapper chapterMapper) {
        this.docLedgerMapper = docLedgerMapper;
        this.chapterMapper = chapterMapper;
    }

    @PostConstruct
    void loadConfig() {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("dependencies.yml")) {
            if (in != null) {
                Yaml yaml = new Yaml();
                depConfig = yaml.load(in);
                log.info("Loaded consistency rules from dependencies.yml");
            }
        } catch (Exception e) {
            log.warn("Failed to load dependencies.yml: {}", e.getMessage());
        }
    }

    /** Value extracted from document content for comparison. */
    @Data
    public static class ExtractedValue {
        String fieldName;    // e.g. "MTBF"
        double numericValue;
        String unit;         // e.g. "h"
        String sourceDocType;
        String sourceChapter;

        public ExtractedValue(String fieldName, double numericValue, String unit,
                               String sourceDocType, String sourceChapter) {
            this.fieldName = fieldName; this.numericValue = numericValue;
            this.unit = unit; this.sourceDocType = sourceDocType;
            this.sourceChapter = sourceChapter;
        }
    }

    public static class ConsistencyReport {
        public Long projectId; public int totalDocs; public int totalIssues;
        public List<ConsistencyIssue> issues = new ArrayList<>();
        public List<ValueComparison> comparisons = new ArrayList<>();
    }

    public static class ConsistencyIssue {
        public static final String SEVERITY_ERROR = "error";
        public static final String SEVERITY_WARN = "warn";
        public String rule; public String description; public String severity;
        public ConsistencyIssue() {}
        public ConsistencyIssue(String rule, String description, String severity) {
            this.rule = rule; this.description = description; this.severity = severity;
        }
    }

    public static class ValueComparison {
        public String fieldName; public double valueA; public double valueB;
        public String docA; public String docB; public boolean consistent;

        public ValueComparison() {}
        public ValueComparison(String fieldName, double valueA, double valueB,
                                String docA, String docB, boolean consistent) {
            this.fieldName = fieldName; this.valueA = valueA; this.valueB = valueB;
            this.docA = docA; this.docB = docB; this.consistent = consistent;
        }
    }

    /**
     * Full project-level consistency check with content comparison.
     */
    public ConsistencyReport checkProject(Long projectId) {
        List<DocLedger> docs = docLedgerMapper.selectList(
            new LambdaQueryWrapper<DocLedger>().eq(DocLedger::getProjectId, projectId));
        ConsistencyReport report = new ConsistencyReport();
        report.projectId = projectId;
        report.totalDocs = docs.size();

        // 1. Basic existence checks
        checkExistenceRules(docs, report);

        // 2. Content-level numerical comparisons
        checkNumericalConsistency(docs, report);

        // 3. YAML-defined conflict rules
        checkYamlRules(docs, report);

        report.totalIssues = report.issues.size();
        return report;
    }

    private void checkExistenceRules(List<DocLedger> docs, ConsistencyReport report) {
        Set<String> docTypes = new HashSet<>();
        for (DocLedger d : docs) {
            if (d.getDocType() != null) docTypes.add(d.getDocType());
        }

        if (docTypes.contains("reliability_outline") && docTypes.contains("reliability_prediction")) {
            report.issues.add(new ConsistencyIssue("大纲-预计", "大纲和预计报告均存在，将进行数值一致性检查", ConsistencyIssue.SEVERITY_WARN));
        }
        if (docTypes.contains("fmeca") && (!docTypes.contains("reliability_outline") || !docTypes.contains("reliability_prediction"))) {
            report.issues.add(new ConsistencyIssue("FMECA前置缺失", "FMECA需要大纲和预计报告作为输入", ConsistencyIssue.SEVERITY_ERROR));
        }
        // Check duplicates
        Map<String, Integer> counts = new HashMap<>();
        for (DocLedger d : docs) {
            if (d.getDocType() != null) counts.merge(d.getDocType(), 1, Integer::sum);
        }
        for (var e : counts.entrySet()) {
            if (e.getValue() > 1) {
                report.issues.add(new ConsistencyIssue("重复文档", e.getKey() + "存在" + e.getValue() + "份", ConsistencyIssue.SEVERITY_WARN));
            }
        }
    }

    /** Extract and compare numerical values from related documents. */
    private void checkNumericalConsistency(List<DocLedger> docs, ConsistencyReport report) {
        // Find related doc pairs and compare
        DocLedger outline = null, prediction = null;
        for (DocLedger d : docs) {
            if ("reliability_outline".equals(d.getDocType())) outline = d;
            if ("reliability_prediction".equals(d.getDocType())) prediction = d;
        }
        if (outline != null && prediction != null) {
            compareDocValues(outline, prediction, report);
        }
    }

    private void compareDocValues(DocLedger docA, DocLedger docB, ConsistencyReport report) {
        Map<String, ExtractedValue> valuesA = extractValues(docA);
        Map<String, ExtractedValue> valuesB = extractValues(docB);

        // Compare MTBF
        ExtractedValue mtbfA = valuesA.get("MTBF");
        ExtractedValue mtbfB = valuesB.get("MTBF");
        if (mtbfA != null && mtbfB != null) {
            double diff = Math.abs(mtbfA.numericValue - mtbfB.numericValue);
            boolean consistent = diff < 0.01 || (mtbfA.numericValue > 0 && diff / mtbfA.numericValue < 0.05);
            report.comparisons.add(new ValueComparison("MTBF",
                mtbfA.numericValue, mtbfB.numericValue,
                docA.getDocName(), docB.getDocName(), consistent));
            if (!consistent) {
                report.issues.add(new ConsistencyIssue("MTBF不一致",
                    String.format("%s: %.0f%s vs %s: %.0f%s — 差异 %.0f%s",
                        docA.getDocName(), mtbfA.numericValue, mtbfA.unit,
                        docB.getDocName(), mtbfB.numericValue, mtbfB.unit,
                        diff, mtbfA.unit),
                    ConsistencyIssue.SEVERITY_ERROR));
            }
        }

        // Compare reliability R(t)
        ExtractedValue relA = valuesA.get("可靠度");
        ExtractedValue relB = valuesB.get("可靠度");
        if (relA != null && relB != null) {
            report.comparisons.add(new ValueComparison("可靠度",
                relA.numericValue, relB.numericValue,
                docA.getDocName(), docB.getDocName(),
                Math.abs(relA.numericValue - relB.numericValue) < 0.01));
        }
    }

    /** Extract numerical values from a document's chapter content. */
    private Map<String, ExtractedValue> extractValues(DocLedger doc) {
        Map<String, ExtractedValue> result = new LinkedHashMap<>();
        if (doc.getId() == null) return result;

        List<DocChapter> chapters = chapterMapper.selectList(
            new LambdaQueryWrapper<DocChapter>()
                .eq(DocChapter::getDocLedgerId, doc.getId())
                .eq(DocChapter::getDeleted, 0));

        // Patterns for extracting numerical values
        record NumPattern(String field, Pattern regex, String unit) {}
        List<NumPattern> patterns = List.of(
            new NumPattern("MTBF", Pattern.compile("MTBF[=：:是为]?\\s*(\\d+(?:\\.\\d+)?)\\s*(h|小时|hrs?)?"), "h"),
            new NumPattern("可靠度", Pattern.compile("可靠度[=：:是为]?\\s*R\\s*\\(?[^)]*\\)?\\s*[=：:是为]?\\s*(0?\\.\\d+)"), ""),
            new NumPattern("使用寿命", Pattern.compile("(?:使用)?寿命[=：:是为]?\\s*(\\d+(?:\\.\\d+)?)\\s*(年)?"), "年")
        );

        for (DocChapter ch : chapters) {
            String content = ch.getContent();
            if (content == null) continue;
            for (NumPattern np : patterns) {
                Matcher m = np.regex.matcher(content);
                if (m.find()) {
                    try {
                        double val = Double.parseDouble(m.group(1));
                        String unit = m.groupCount() >= 2 && m.group(2) != null ? m.group(2) : np.unit;
                        result.put(np.field, new ExtractedValue(np.field, val, unit,
                            doc.getDocType(), ch.getChapterNumber()));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void checkYamlRules(List<DocLedger> docs, ConsistencyReport report) {
        if (depConfig.isEmpty()) return;
        try {
            Map<String, Object> dd = (Map<String, Object>) depConfig.get("document_dependencies");
            if (dd == null) return;

            // Check consistency rules from YAML
            for (var entry : dd.entrySet()) {
                Map<String, Object> dep = (Map<String, Object>) entry.getValue();
                List<Map<String, Object>> conflicts = (List<Map<String, Object>>) dep.get("conflicts");
                if (conflicts != null) {
                    for (Map<String, Object> conflict : conflicts) {
                        String check = (String) conflict.get("check");
                        String ifMismatch = (String) conflict.get("if_mismatch");
                        if (check != null) {
                            report.issues.add(new ConsistencyIssue(
                                "YAML冲突规则: " + entry.getKey(),
                                check + (ifMismatch != null ? " → " + ifMismatch : ""),
                                "error".equals(ifMismatch) ? ConsistencyIssue.SEVERITY_ERROR : ConsistencyIssue.SEVERITY_WARN));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to process YAML rules: {}", e.getMessage());
        }
    }

    /** Simple project-level check returning summary only. */
    public Map<String, Object> quickCheck(Long projectId) {
        ConsistencyReport report = checkProject(projectId);
        return Map.of(
            "totalDocs", report.totalDocs,
            "totalIssues", report.totalIssues,
            "errorCount", report.issues.stream().filter(i -> ConsistencyIssue.SEVERITY_ERROR.equals(i.severity)).count(),
            "warnCount", report.issues.stream().filter(i -> ConsistencyIssue.SEVERITY_WARN.equals(i.severity)).count(),
            "comparisons", report.comparisons.size(),
            "issues", report.issues,
            "comparisons", report.comparisons
        );
    }
}
