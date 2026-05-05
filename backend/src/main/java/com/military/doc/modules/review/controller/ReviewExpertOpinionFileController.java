package com.military.doc.modules.review.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.military.doc.common.result.Result;
import com.military.doc.modules.review.entity.ReviewExpertOpinionFile;
import com.military.doc.modules.review.service.ReviewExpertOpinionFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/review-expert-opinions")
@Tag(name = "专家评审意见管理")
public class ReviewExpertOpinionFileController {

    @Autowired
    private ReviewExpertOpinionFileService opinionService;

    @PostMapping
    @Operation(summary = "上传专家评审意见")
    public Result<ReviewExpertOpinionFile> create(@RequestBody ReviewExpertOpinionFile opinion, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        opinion.setCreatedBy(userId);
        opinion.setUpdatedBy(userId);
        if (opinion.getUploadedAt() == null) {
            opinion.setUploadedAt(LocalDateTime.now());
        }
        opinionService.save(opinion);
        return Result.success(opinion);
    }

    @GetMapping
    @Operation(summary = "分页查询专家意见")
    public Result<Page<ReviewExpertOpinionFile>> list(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long meetingId,
            @RequestParam(required = false) Long expertUserId,
            @RequestParam(required = false) String problemLevel) {
        LambdaQueryWrapper<ReviewExpertOpinionFile> wrapper = new LambdaQueryWrapper<>();
        if (meetingId != null) {
            wrapper.eq(ReviewExpertOpinionFile::getMeetingId, meetingId);
        }
        if (expertUserId != null) {
            wrapper.eq(ReviewExpertOpinionFile::getExpertUserId, expertUserId);
        }
        if (problemLevel != null) {
            wrapper.eq(ReviewExpertOpinionFile::getProblemLevel, problemLevel);
        }
        wrapper.orderByDesc(ReviewExpertOpinionFile::getCreatedAt);
        return Result.success(opinionService.page(new Page<>(pageNo, pageSize), wrapper));
    }

    @GetMapping("/meeting/{meetingId}")
    @Operation(summary = "获取会议的所有专家意见")
    public Result<List<ReviewExpertOpinionFile>> listByMeeting(@PathVariable Long meetingId) {
        List<ReviewExpertOpinionFile> opinions = opinionService.list(
            new LambdaQueryWrapper<ReviewExpertOpinionFile>()
                .eq(ReviewExpertOpinionFile::getMeetingId, meetingId)
                .orderByAsc(ReviewExpertOpinionFile::getExpertUserId)
        );
        return Result.success(opinions);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取专家意见详情")
    public Result<ReviewExpertOpinionFile> getById(@PathVariable Long id) {
        return Result.success(opinionService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新专家意见")
    public Result<ReviewExpertOpinionFile> update(@PathVariable Long id, @RequestBody ReviewExpertOpinionFile opinion, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        opinion.setId(id);
        opinion.setUpdatedBy(userId);
        opinionService.updateById(opinion);
        return Result.success(opinionService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除专家意见")
    public Result<Void> delete(@PathVariable Long id) {
        opinionService.removeById(id);
        return Result.success();
    }
}
