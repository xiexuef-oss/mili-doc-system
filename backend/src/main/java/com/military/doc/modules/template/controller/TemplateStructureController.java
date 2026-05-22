package com.military.doc.modules.template.controller;

import com.military.doc.common.result.Result;
import com.military.doc.modules.template.entity.*;
import com.military.doc.modules.template.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/template-structure")
public class TemplateStructureController {

    @Autowired private DocTemplateCategoryService categoryService;
    @Autowired private DocTemplateV2Service templateV2Service;
    @Autowired private TemplateStructureService structureService;

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
}
