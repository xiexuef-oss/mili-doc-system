package com.military.doc.modules.template.controller;

import com.military.doc.ai.util.TemplateDocxParser;
import com.military.doc.ai.util.TemplateDocxParser.TemplateStructure;
import com.military.doc.ai.util.TemplateDocxParser.ChapterNode;
import com.military.doc.common.result.Result;
import com.military.doc.common.storage.FileStorageService;
import com.military.doc.modules.template.entity.*;
import com.military.doc.modules.template.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RestController
@RequestMapping("/api/v1/template-structure")
public class TemplateStructureController {

    @Autowired private DocTemplateCategoryService categoryService;
    @Autowired private DocTemplateV2Service templateV2Service;
    @Autowired private TemplateStructureService structureService;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private TemplateDocxParser templateDocxParser;

    // === Categories ===
    @GetMapping("/categories")
    public Result<List<DocTemplateCategory>> listCategories() {
        return Result.success(categoryService.listActive());
    }

    @GetMapping("/categories/tree")
    public Result<List<DocTemplateCategory>> categoryTree() {
        return Result.success(categoryService.getTree());
    }

    @PostMapping("/categories")
    public Result<DocTemplateCategory> createCategory(@RequestBody DocTemplateCategory category) {
        categoryService.save(category);
        return Result.success(category);
    }

    // === Templates V2 ===
    @GetMapping("/templates")
    public Result<List<DocTemplateV2>> listTemplates(@RequestParam(required = false) Long categoryId) {
        if (categoryId != null) {
            return Result.success(templateV2Service.listByCategory(categoryId));
        }
        return Result.success(templateV2Service.list());
    }

    @PostMapping("/templates")
    public Result<DocTemplateV2> createTemplate(@RequestBody DocTemplateV2 template) {
        templateV2Service.save(template);
        return Result.success(template);
    }

    @PutMapping("/templates/{id}")
    public Result<DocTemplateV2> updateTemplate(@PathVariable Long id, @RequestBody DocTemplateV2 template) {
        template.setId(id);
        templateV2Service.updateById(template);
        return Result.success(template);
    }

    @DeleteMapping("/templates/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        templateV2Service.removeById(id);
        return Result.success();
    }

    // === Chapters ===
    @GetMapping("/templates/{templateId}/chapters")
    public Result<List<DocTemplateChapter>> getChapters(@PathVariable Long templateId) {
        return Result.success(structureService.getChapterTree(templateId));
    }

    @GetMapping("/templates/{templateId}/chapters/required")
    public Result<List<DocTemplateChapter>> getRequiredChapters(@PathVariable Long templateId) {
        return Result.success(structureService.getRequiredChapters(templateId));
    }

    @PostMapping("/templates/{templateId}/chapters")
    public Result<DocTemplateChapter> createChapter(@PathVariable Long templateId,
                                                     @RequestBody DocTemplateChapter chapter) {
        chapter.setTemplateId(templateId);
        return Result.success(structureService.createChapter(chapter));
    }

    @PutMapping("/chapters/{id}")
    public Result<DocTemplateChapter> updateChapter(@PathVariable Long id,
                                                     @RequestBody DocTemplateChapter chapter) {
        chapter.setId(id);
        return Result.success(structureService.updateChapter(chapter));
    }

    @DeleteMapping("/chapters/{id}")
    public Result<Void> deleteChapter(@PathVariable Long id) {
        structureService.deleteChapter(id);
        return Result.success();
    }

    @PutMapping("/templates/{templateId}/chapters/reorder")
    public Result<Void> reorderChapters(@PathVariable Long templateId,
                                         @RequestBody List<Long> chapterIds) {
        structureService.reorderChapters(templateId, chapterIds);
        return Result.success();
    }

    // === Elements ===
    @GetMapping("/elements")
    public Result<List<DocTemplateElement>> listElements(@RequestParam(required = false) String category) {
        return Result.success(structureService.listElements(category));
    }

    @PostMapping("/elements")
    public Result<DocTemplateElement> createElement(@RequestBody DocTemplateElement element) {
        return Result.success(structureService.createElement(element));
    }

    @PostMapping("/chapters/{chapterId}/elements/{elementId}")
    public Result<Void> attachElement(@PathVariable Long chapterId, @PathVariable Long elementId,
                                       @RequestParam(defaultValue = "false") boolean required,
                                       @RequestParam(defaultValue = "0") int orderNum) {
        structureService.attachElement(chapterId, elementId, required, orderNum);
        return Result.success();
    }

    @DeleteMapping("/chapters/{chapterId}/elements/{elementId}")
    public Result<Void> detachElement(@PathVariable Long chapterId, @PathVariable Long elementId) {
        structureService.detachElement(chapterId, elementId);
        return Result.success();
    }

    @GetMapping("/chapters/{chapterId}/elements")
    public Result<List<DocTemplateElement>> getChapterElements(@PathVariable Long chapterId) {
        return Result.success(structureService.getChapterElements(chapterId));
    }

    // === DOCX Auto-Parse ===

    @PostMapping("/templates/{templateId}/upload-docx")
    public Result<Map<String, Object>> uploadAndParseDocx(
            @PathVariable Long templateId,
            @RequestParam("file") MultipartFile file) {
        DocTemplateV2 template = templateV2Service.getById(templateId);
        if (template == null) {
            return Result.error("NOT_FOUND", "模板不存在");
        }

        // 1. Store original file
        String objectId = fileStorageService.upload(file);
        template.setFileObjectId(objectId);
        template.setFileName(file.getOriginalFilename());
        template.setFileSize(file.getSize());
        template.setFileType(getExtension(file.getOriginalFilename()));
        templateV2Service.updateById(template);

        // 2. Parse DOCX structure
        TemplateStructure structure;
        try {
            structure = templateDocxParser.parse(file.getBytes(), file.getOriginalFilename());
        } catch (Exception e) {
            log.error("Failed to parse template DOCX: {}", e.getMessage());
            return Result.error("PARSE_ERROR", "模板文件解析失败: " + e.getMessage());
        }

        // 3. Delete existing chapters and recreate from parsed structure
        structureService.deleteChaptersByTemplateId(templateId);

        // 4. Create chapters from parsed tree
        List<ChapterNode> flatChapters = structure.flattenChapters();
        AtomicInteger created = new AtomicInteger(0);

        // First pass: create all chapters with temp IDs for parent linking
        Map<String, Long> pathToId = new LinkedHashMap<>();
        for (ChapterNode node : flatChapters) {
            DocTemplateChapter chapter = new DocTemplateChapter();
            chapter.setTemplateId(templateId);
            chapter.setChapterNumber(node.getNumberingFormat());
            chapter.setChapterTitle(node.getTitle());
            chapter.setChapterLevel(node.getLevel());
            chapter.setOrderNum(node.getOrderNum());
            chapter.setIsRequired(true);
            chapter.setDescription(buildFullDescription(node));
            chapter.setWritingTips(node.getWritingTips());
            chapter.setSampleContent(node.getSampleContent());
            chapter.setHeadingStyle(node.getHeadingStyle());
            chapter.setNumberingFormat(node.getNumberingFormat());
            chapter.setHasTable(node.isHasTable());
            if (node.isHasTable() && !node.getTables().isEmpty()) {
                chapter.setTableJson(toJson(node.getTables()));
            }
            if (!node.getVariables().isEmpty()) {
                chapter.setVariablePlaceholders(node.getVariables().stream()
                    .map(TemplateDocxParser.VariablePlaceholder::getPlaceholder)
                    .reduce((a, b) -> a + "," + b).orElse(""));
            }
            if (node.getFontColor() != null || node.isBold()) {
                StringBuilder emphasis = new StringBuilder();
                if (node.isBold()) emphasis.append("bold");
                if (node.getFontColor() != null) {
                    if (!emphasis.isEmpty()) emphasis.append(",");
                    emphasis.append("color:").append(node.getFontColor());
                }
                chapter.setFontEmphasis(emphasis.toString());
            }

            structureService.createChapter(chapter);
            pathToId.put(node.getPath(), chapter.getId());
            created.incrementAndGet();
        }

        // Second pass: update parent relationships
        for (ChapterNode node : flatChapters) {
            if (node.getParent() != null) {
                Long childId = pathToId.get(node.getPath());
                Long parentId = pathToId.get(node.getParent().getPath());
                if (childId != null && parentId != null) {
                    DocTemplateChapter child = structureService.getChapterById(childId);
                    if (child != null) {
                        child.setParentId(parentId);
                        structureService.updateChapter(child);
                    }
                }
            }
        }

        // 5. Return parse summary
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("templateId", templateId);
        result.put("title", structure.getTitle());
        result.put("securityLevel", structure.getSecurityLevel());
        result.put("docCodeFormat", structure.getDocCodeFormat());
        result.put("hasCover", structure.isHasCover());
        result.put("chaptersCreated", created.get());
        result.put("variablesDetected", structure.getVariables().size());
        result.put("tablesDetected", structure.getTables().size());
        result.put("totalChars", structure.getRawText().length());
        result.put("variableList", structure.getVariables().stream()
            .map(v -> Map.of("placeholder", v.getPlaceholder(), "type", v.getType()))
            .distinct()
            .toList());

        log.info("Template DOCX parsed: templateId={}, chapters={}, vars={}, tables={}",
            templateId, created.get(), structure.getVariables().size(), structure.getTables().size());
        return Result.success(result);
    }

    private String buildFullDescription(ChapterNode node) {
        StringBuilder desc = new StringBuilder();
        if (node.getDescription() != null && !node.getDescription().isBlank()) {
            desc.append(node.getDescription());
        }
        if (node.isHasTable()) {
            if (!desc.isEmpty()) desc.append("; ");
            desc.append("含").append(node.getTables().size()).append("个表格");
        }
        if (!node.getVariables().isEmpty()) {
            if (!desc.isEmpty()) desc.append("; ");
            desc.append("变量: ").append(node.getVariables().stream()
                .map(TemplateDocxParser.VariablePlaceholder::getPlaceholder)
                .limit(5).reduce((a, b) -> a + ", " + b).orElse(""));
        }
        return desc.toString();
    }

    private String toJson(List<TemplateDocxParser.TableStructure> tables) {
        if (tables == null || tables.isEmpty()) return null;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> list = new ArrayList<>();
            for (TemplateDocxParser.TableStructure t : tables) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("caption", t.getCaption());
                m.put("headers", t.getHeaders());
                m.put("columnCount", t.getColumnCount());
                m.put("rowCount", t.getRowCount());
                list.add(m);
            }
            return mapper.writeValueAsString(list);
        } catch (Exception e) {
            return null;
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return i >= 0 ? filename.substring(i) : "";
    }
}
