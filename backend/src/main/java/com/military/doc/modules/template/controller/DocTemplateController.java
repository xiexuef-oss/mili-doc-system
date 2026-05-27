package com.military.doc.modules.template.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.military.doc.common.result.Result;
import com.military.doc.common.storage.FileStorageService;
import com.military.doc.modules.template.entity.DocTemplate;
import com.military.doc.modules.template.service.DocTemplateService;
import com.military.doc.modules.template.service.TemplateGenerateService;
import com.military.doc.modules.document.entity.DocFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/templates")
@Tag(name = "模版管理")
public class DocTemplateController {

    @Autowired
    private DocTemplateService docTemplateService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private TemplateGenerateService templateGenerateService;

    @GetMapping
    @Operation(summary = "分页查询模版")
    public Result<Page<DocTemplate>> list(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String templateType,
            @RequestParam(required = false) String applicableProjectType,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<DocTemplate> wrapper = new LambdaQueryWrapper<>();
        if (templateType != null && !templateType.isEmpty()) {
            wrapper.eq(DocTemplate::getTemplateType, templateType);
        }
        if (applicableProjectType != null && !applicableProjectType.isEmpty()) {
            wrapper.eq(DocTemplate::getApplicableProjectType, applicableProjectType);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(DocTemplate::getTemplateName, keyword)
                .or().like(DocTemplate::getTemplateCode, keyword));
        }
        wrapper.orderByDesc(DocTemplate::getCreatedAt);
        return Result.success(docTemplateService.page(new Page<>(pageNo, pageSize), wrapper));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取模版详情")
    public Result<DocTemplate> getById(@PathVariable Long id) {
        return Result.success(docTemplateService.getById(id));
    }

    @PostMapping("/batch-upload")
    @Operation(summary = "批量上传模版文件")
    public Result<List<DocTemplate>> batchUpload(@RequestParam("files") List<MultipartFile> files) {
        List<DocTemplate> templates = files.stream().map(file -> {
            String objectId = fileStorageService.upload(file);
            DocTemplate template = new DocTemplate();
            template.setTemplateName(stripExtension(file.getOriginalFilename()));
            template.setTemplateCode("");
            template.setTemplateType("");
            template.setStatus("ACTIVE");
            template.setFileObjectId(objectId);
            template.setFileName(file.getOriginalFilename());
            template.setFileSize(file.getSize());
            template.setFileType(getExtension(file.getOriginalFilename()));
            return template;
        }).collect(java.util.stream.Collectors.toList());
        if (!templates.isEmpty()) {
            docTemplateService.saveBatch(templates);
        }
        return Result.success(templates);
    }

    @PostMapping("/{id}/generate")
    @Operation(summary = "从模版生成文档")
    public Result<DocFile> generate(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Long projectId = Long.valueOf(request.get("projectId").toString());
        @SuppressWarnings("unchecked")
        Map<String, String> variables = (Map<String, String>) request.get("variables");
        return Result.success(templateGenerateService.generate(id, projectId, variables));
    }

    @PostMapping
    @Operation(summary = "创建模版")
    public Result<DocTemplate> create(@RequestBody DocTemplate template) {
        if (template.getStatus() == null) {
            template.setStatus("ACTIVE");
        }
        docTemplateService.save(template);
        return Result.success(template);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新模版")
    public Result<DocTemplate> update(@PathVariable Long id, @RequestBody DocTemplate template) {
        template.setId(id);
        docTemplateService.updateById(template);
        return Result.success(docTemplateService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除模版")
    public Result<Void> delete(@PathVariable Long id) {
        docTemplateService.removeById(id);
        return Result.success();
    }

    @PostMapping("/{id}/upload")
    @Operation(summary = "上传模版文件")
    public Result<DocTemplate> uploadFile(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        String objectId = fileStorageService.upload(file);
        DocTemplate template = docTemplateService.getById(id);
        if (template == null) {
            return Result.error("NOT_FOUND", "模版不存在");
        }
        if (template.getFileObjectId() != null) {
            try { fileStorageService.delete(template.getFileObjectId()); } catch (Exception ignored) {}
        }
        template.setFileObjectId(objectId);
        template.setFileName(file.getOriginalFilename());
        template.setFileSize(file.getSize());
        template.setFileType(getExtension(file.getOriginalFilename()));
        docTemplateService.updateById(template);
        return Result.success(docTemplateService.getById(id));
    }

    @GetMapping("/{id}/download-url")
    @Operation(summary = "获取模版文件下载地址")
    public Result<String> getDownloadUrl(@PathVariable Long id) {
        DocTemplate template = docTemplateService.getById(id);
        if (template == null || template.getFileObjectId() == null) {
            return Result.error("NOT_FOUND", "文件不存在");
        }
        return Result.success(fileStorageService.getAccessUrl(template.getFileObjectId()));
    }

    @GetMapping("/types")
    @Operation(summary = "获取模版类型列表")
    public Result<List<DocTemplate>> listTypes() {
        LambdaQueryWrapper<DocTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(DocTemplate::getTemplateType).groupBy(DocTemplate::getTemplateType);
        return Result.success(docTemplateService.list(wrapper));
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return i >= 0 ? filename.substring(i) : "";
    }

    private String stripExtension(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return i >= 0 ? filename.substring(0, i) : filename;
    }
}
