package com.military.doc.modules.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.military.doc.common.result.Result;
import com.military.doc.common.storage.FileStorageService;
import com.military.doc.modules.project.entity.ProjectInputFile;
import com.military.doc.modules.project.mapper.ProjectInputFileMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/input-files")
@Tag(name = "项目输入文件管理")
public class ProjectInputFileController {

    @Autowired
    private ProjectInputFileMapper inputFileMapper;

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping
    @Operation(summary = "获取项目输入文件列表")
    public Result<List<ProjectInputFile>> list(@PathVariable Long projectId) {
        return Result.success(inputFileMapper.selectList(
            new LambdaQueryWrapper<ProjectInputFile>()
                .eq(ProjectInputFile::getProjectId, projectId)
                .orderByDesc(ProjectInputFile::getUploadedAt)
        ));
    }

    @PostMapping
    @Operation(summary = "上传项目输入文件")
    public Result<ProjectInputFile> upload(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("inputType") String inputType,
            @RequestParam(value = "description", required = false) String description) {
        String objectId = fileStorageService.upload(file);
        ProjectInputFile entity = new ProjectInputFile();
        entity.setProjectId(projectId);
        entity.setFileName(file.getOriginalFilename());
        entity.setFileObjectId(objectId);
        entity.setFileSize(file.getSize());
        entity.setFileType(getExtension(file.getOriginalFilename()));
        entity.setInputType(inputType);
        entity.setDescription(description);
        inputFileMapper.insert(entity);
        return Result.success(entity);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除输入文件")
    public Result<Void> delete(@PathVariable Long projectId, @PathVariable Long id) {
        ProjectInputFile entity = inputFileMapper.selectById(id);
        if (entity != null && entity.getFileObjectId() != null) {
            fileStorageService.delete(entity.getFileObjectId());
        }
        inputFileMapper.deleteById(id);
        return Result.success();
    }

    @GetMapping("/{id}/download-url")
    @Operation(summary = "获取输入文件下载地址")
    public Result<String> getDownloadUrl(@PathVariable Long projectId, @PathVariable Long id) {
        ProjectInputFile entity = inputFileMapper.selectById(id);
        if (entity == null || entity.getFileObjectId() == null) {
            return Result.error("NOT_FOUND", "文件不存在");
        }
        return Result.success(fileStorageService.getAccessUrl(entity.getFileObjectId()));
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return i >= 0 ? filename.substring(i) : "";
    }
}
