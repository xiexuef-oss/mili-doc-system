package com.military.doc.modules.review.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.military.doc.common.result.Result;
import com.military.doc.modules.review.entity.ReviewMeetingDocument;
import com.military.doc.modules.review.service.ReviewMeetingDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/review-meeting-documents")
@Tag(name = "评审会议文档管理")
public class ReviewMeetingDocumentController {

    @Autowired
    private ReviewMeetingDocumentService meetingDocService;

    @PostMapping
    @Operation(summary = "添加会议评审文档")
    public Result<ReviewMeetingDocument> create(@RequestBody ReviewMeetingDocument doc, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        doc.setCreatedBy(userId);
        doc.setUpdatedBy(userId);
        meetingDocService.save(doc);
        return Result.success(doc);
    }

    @GetMapping
    @Operation(summary = "分页查询会议文档")
    public Result<Page<ReviewMeetingDocument>> list(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long meetingId) {
        LambdaQueryWrapper<ReviewMeetingDocument> wrapper = new LambdaQueryWrapper<>();
        if (meetingId != null) {
            wrapper.eq(ReviewMeetingDocument::getMeetingId, meetingId);
        }
        wrapper.orderByDesc(ReviewMeetingDocument::getCreatedAt);
        return Result.success(meetingDocService.page(new Page<>(pageNo, pageSize), wrapper));
    }

    @GetMapping("/meeting/{meetingId}")
    @Operation(summary = "获取会议的所有评审文档")
    public Result<List<ReviewMeetingDocument>> listByMeeting(@PathVariable Long meetingId) {
        List<ReviewMeetingDocument> docs = meetingDocService.list(
            new LambdaQueryWrapper<ReviewMeetingDocument>()
                .eq(ReviewMeetingDocument::getMeetingId, meetingId)
                .orderByAsc(ReviewMeetingDocument::getDocFileId)
        );
        return Result.success(docs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取会议文档详情")
    public Result<ReviewMeetingDocument> getById(@PathVariable Long id) {
        return Result.success(meetingDocService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新会议文档评审结果")
    public Result<ReviewMeetingDocument> update(@PathVariable Long id, @RequestBody ReviewMeetingDocument doc, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        doc.setId(id);
        doc.setUpdatedBy(userId);
        meetingDocService.updateById(doc);
        return Result.success(meetingDocService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "移除会议文档")
    public Result<Void> delete(@PathVariable Long id) {
        meetingDocService.removeById(id);
        return Result.success();
    }
}
