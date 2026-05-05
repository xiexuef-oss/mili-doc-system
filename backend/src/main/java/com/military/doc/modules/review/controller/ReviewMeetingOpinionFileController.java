package com.military.doc.modules.review.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.military.doc.common.result.Result;
import com.military.doc.modules.review.entity.ReviewMeetingOpinionFile;
import com.military.doc.modules.review.service.ReviewMeetingOpinionFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/review-meeting-opinions")
@Tag(name = "会议意见汇总管理")
public class ReviewMeetingOpinionFileController {

    @Autowired
    private ReviewMeetingOpinionFileService opinionFileService;

    @PostMapping
    @Operation(summary = "上传会议意见汇总文件")
    public Result<ReviewMeetingOpinionFile> create(@RequestBody ReviewMeetingOpinionFile file, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        file.setUploadedBy(userId);
        file.setCreatedBy(userId);
        file.setUpdatedBy(userId);
        if (file.getUploadedAt() == null) {
            file.setUploadedAt(LocalDateTime.now());
        }
        if (file.getStatus() == null) {
            file.setStatus("DRAFT");
        }
        opinionFileService.save(file);
        return Result.success(file);
    }

    @GetMapping
    @Operation(summary = "分页查询意见汇总")
    public Result<Page<ReviewMeetingOpinionFile>> list(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long meetingId,
            @RequestParam(required = false) String opinionType) {
        LambdaQueryWrapper<ReviewMeetingOpinionFile> wrapper = new LambdaQueryWrapper<>();
        if (meetingId != null) {
            wrapper.eq(ReviewMeetingOpinionFile::getMeetingId, meetingId);
        }
        if (opinionType != null) {
            wrapper.eq(ReviewMeetingOpinionFile::getOpinionType, opinionType);
        }
        wrapper.orderByDesc(ReviewMeetingOpinionFile::getCreatedAt);
        return Result.success(opinionFileService.page(new Page<>(pageNo, pageSize), wrapper));
    }

    @GetMapping("/meeting/{meetingId}")
    @Operation(summary = "获取会议的所有意见汇总")
    public Result<List<ReviewMeetingOpinionFile>> listByMeeting(@PathVariable Long meetingId) {
        List<ReviewMeetingOpinionFile> files = opinionFileService.list(
            new LambdaQueryWrapper<ReviewMeetingOpinionFile>()
                .eq(ReviewMeetingOpinionFile::getMeetingId, meetingId)
                .orderByAsc(ReviewMeetingOpinionFile::getDocFileId)
        );
        return Result.success(files);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取意见汇总详情")
    public Result<ReviewMeetingOpinionFile> getById(@PathVariable Long id) {
        return Result.success(opinionFileService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新意见汇总")
    public Result<ReviewMeetingOpinionFile> update(@PathVariable Long id, @RequestBody ReviewMeetingOpinionFile file, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        file.setId(id);
        file.setUpdatedBy(userId);
        opinionFileService.updateById(file);
        return Result.success(opinionFileService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除意见汇总")
    public Result<Void> delete(@PathVariable Long id) {
        opinionFileService.removeById(id);
        return Result.success();
    }
}
