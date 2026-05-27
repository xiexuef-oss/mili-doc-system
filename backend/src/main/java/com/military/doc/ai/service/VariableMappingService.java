package com.military.doc.ai.service;

import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.mapper.DocChapterMapper;
import com.military.doc.modules.project.service.ProjectMasterDataService;
import com.military.doc.modules.template.entity.DocTemplateChapter;
import com.military.doc.modules.template.mapper.DocTemplateChapterMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class VariableMappingService {

    private final DocChapterMapper docChapterMapper;
    private final DocTemplateChapterMapper templateChapterMapper;
    private final ProjectMasterDataService masterDataService;

    // Match: ×××, ××, 【×××】, 【×待填写×】, ×××(any)×××
    private static final Pattern PLACEHOLDER_PATTERN =
        Pattern.compile("[【\\[]?[×X＊\\*]{2,}[^】\\]]*[×X＊\\*]{2,}[】\\]]?");

    // Map variable names to master data field paths
    private static final Map<Pattern, String> VARIABLE_MAP = new LinkedHashMap<>();

    static {
        VARIABLE_MAP.put(Pattern.compile("装[备配]名称"), "equipment.equipmentName");
        VARIABLE_MAP.put(Pattern.compile("装[备配]型号"), "equipment.equipmentModel");
        VARIABLE_MAP.put(Pattern.compile("装[备配]代号"), "equipment.equipmentCode");
        VARIABLE_MAP.put(Pattern.compile("任务书编号"), "equipment.taskBookCode");
        VARIABLE_MAP.put(Pattern.compile("合同编号"), "equipment.contractCode");
        VARIABLE_MAP.put(Pattern.compile("研制单位"), "equipment.developerUnit");
        VARIABLE_MAP.put(Pattern.compile("承制单位"), "equipment.manufacturerUnit");
        VARIABLE_MAP.put(Pattern.compile("总师[单员]位"), "equipment.chiefEngineerUnit");
        VARIABLE_MAP.put(Pattern.compile("主[总师任]"), "equipment.chiefEngineer");
        VARIABLE_MAP.put(Pattern.compile("项目负责人"), "equipment.projectManager");
        VARIABLE_MAP.put(Pattern.compile("项目名称"), "equipment.projectName");
        VARIABLE_MAP.put(Pattern.compile("产品名称"), "equipment.productName");
        VARIABLE_MAP.put(Pattern.compile("产品代号"), "equipment.productCode");
        VARIABLE_MAP.put(Pattern.compile("产品型号"), "equipment.productModel");
    }

    public VariableMappingService(DocChapterMapper docChapterMapper,
                                   DocTemplateChapterMapper templateChapterMapper,
                                   ProjectMasterDataService masterDataService) {
        this.docChapterMapper = docChapterMapper;
        this.templateChapterMapper = templateChapterMapper;
        this.masterDataService = masterDataService;
    }

    /**
     * Scan all template chapters for ××× placeholders and return their variable names.
     */
    public Set<String> scanTemplatePlaceholders(Long templateId) {
        Set<String> variables = new LinkedHashSet<>();
        List<DocTemplateChapter> chapters = templateChapterMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocTemplateChapter>()
                .eq(DocTemplateChapter::getTemplateId, templateId)
                .eq(DocTemplateChapter::getDeleted, 0)
        );

        for (DocTemplateChapter chapter : chapters) {
            String combined = String.join(" ",
                nullToEmpty(chapter.getDescription()),
                nullToEmpty(chapter.getWritingTips()),
                nullToEmpty(chapter.getSampleContent())
            );
            Matcher matcher = PLACEHOLDER_PATTERN.matcher(combined);
            while (matcher.find()) {
                variables.add(matcher.group());
            }
        }
        log.info("Template {} has {} placeholders: {}", templateId, variables.size(), variables);
        return variables;
    }

    /**
     * Resolve a placeholder text to its master data value.
     */
    public String resolveVariable(String placeholder, Long projectId) {
        String path = findPath(placeholder);
        if (path == null) return null;

        Map<String, Object> data = masterDataService.getFlattenedData(projectId);
        Object value = data.get(path);
        return value != null ? value.toString() : null;
    }

    /**
     * Auto-fill a single chapter's content by replacing ××× placeholders with master data values.
     */
    public DocChapter autoFillChapter(Long docChapterId, Long projectId) {
        DocChapter chapter = docChapterMapper.selectById(docChapterId);
        if (chapter == null) return null;

        String content = chapter.getContent();
        if (content == null || content.isBlank()) {
            log.info("Chapter {} has no content to auto-fill", docChapterId);
            return chapter;
        }

        Map<String, Object> data = masterDataService.getFlattenedData(projectId);
        String filled = replacePlaceholders(content, data);
        int fills = countReplacements(content, filled);
        log.info("Auto-filled chapter {}: {} replacements", docChapterId, fills);

        if (fills > 0) {
            chapter.setContent(filled);
            docChapterMapper.updateById(chapter);
        }
        return chapter;
    }

    /**
     * Auto-fill ALL chapters of a doc ledger.
     */
    public int autoFillAll(Long docLedgerId, Long projectId) {
        List<DocChapter> chapters = docChapterMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocChapter>()
                .eq(DocChapter::getDocLedgerId, docLedgerId)
                .eq(DocChapter::getDeleted, 0)
        );

        Map<String, Object> data = masterDataService.getFlattenedData(projectId);
        int totalFills = 0;

        for (DocChapter chapter : chapters) {
            String content = chapter.getContent();
            if (content == null || content.isBlank()) continue;

            String filled = replacePlaceholders(content, data);
            int fills = countReplacements(content, filled);
            if (fills > 0) {
                chapter.setContent(filled);
                docChapterMapper.updateById(chapter);
                totalFills += fills;
            }
        }

        log.info("Auto-filled {} chapters in ledger {}: {} total replacements",
            chapters.size(), docLedgerId, totalFills);
        return totalFills;
    }

    String replacePlaceholders(String text, Map<String, Object> data) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String placeholder = matcher.group();
            String path = findPath(placeholder);
            String replacement = null;
            if (path != null) {
                Object value = data.get(path);
                if (value != null && !value.toString().isBlank()) {
                    replacement = Matcher.quoteReplacement(value.toString());
                }
            }
            matcher.appendReplacement(sb, replacement != null ? replacement : Matcher.quoteReplacement(placeholder));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String findPath(String placeholder) {
        // Strip brackets for matching
        String clean = placeholder.replaceAll("[【】\\[\\]]", "");
        for (Map.Entry<Pattern, String> entry : VARIABLE_MAP.entrySet()) {
            if (entry.getKey().matcher(clean).find()) {
                return entry.getValue();
            }
        }
        return null;
    }

    private int countReplacements(String original, String replaced) {
        if (original.equals(replaced)) return 0;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(original);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
