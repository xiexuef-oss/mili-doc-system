package com.military.doc.modules.template.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.military.doc.common.result.Result;
import com.military.doc.common.storage.FileStorageService;
import com.military.doc.modules.template.entity.DocTemplate;
import com.military.doc.modules.template.mapper.DocTemplateMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/templates")
@Tag(name = "模版管理")
public class DocTemplateController {

    @Autowired
    private DocTemplateMapper docTemplateMapper;

    @Autowired
    private FileStorageService fileStorageService;

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
        return Result.success(docTemplateMapper.selectPage(new Page<>(pageNo, pageSize), wrapper));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取模版详情")
    public Result<DocTemplate> getById(@PathVariable Long id) {
        return Result.success(docTemplateMapper.selectById(id));
    }

    @PostMapping
    @Operation(summary = "创建模版")
    public Result<DocTemplate> create(@RequestBody DocTemplate template) {
        if (template.getStatus() == null) {
            template.setStatus("ACTIVE");
        }
        docTemplateMapper.insert(template);
        return Result.success(template);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新模版")
    public Result<DocTemplate> update(@PathVariable Long id, @RequestBody DocTemplate template) {
        template.setId(id);
        docTemplateMapper.updateById(template);
        return Result.success(docTemplateMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除模版")
    public Result<Void> delete(@PathVariable Long id) {
        docTemplateMapper.deleteById(id);
        return Result.success();
    }

    @PostMapping("/{id}/upload")
    @Operation(summary = "上传模版文件")
    public Result<DocTemplate> uploadFile(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        String objectId = fileStorageService.upload(file);
        DocTemplate template = docTemplateMapper.selectById(id);
        if (template == null) {
            return Result.error("NOT_FOUND", "模版不存在");
        }
        if (template.getFileObjectId() != null) {
            fileStorageService.delete(template.getFileObjectId());
        }
        template.setFileObjectId(objectId);
        template.setFileName(file.getOriginalFilename());
        template.setFileSize(file.getSize());
        template.setFileType(getExtension(file.getOriginalFilename()));
        docTemplateMapper.updateById(template);
        return Result.success(docTemplateMapper.selectById(id));
    }

    @GetMapping("/{id}/download-url")
    @Operation(summary = "获取模版文件下载地址")
    public Result<String> getDownloadUrl(@PathVariable Long id) {
        DocTemplate template = docTemplateMapper.selectById(id);
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
        return Result.success(docTemplateMapper.selectList(wrapper));
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return i >= 0 ? filename.substring(i) : "";
    }
}
