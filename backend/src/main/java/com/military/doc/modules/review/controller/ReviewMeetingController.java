package com.military.doc.modules.review.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.military.doc.common.result.Result;
import com.military.doc.modules.review.entity.ReviewMeeting;
import com.military.doc.modules.review.service.ReviewMeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/review-meetings")
@Tag(name = "评审会议管理")
public class ReviewMeetingController {

    @Autowired
    private ReviewMeetingService meetingService;

    @PostMapping
    @Operation(summary = "创建评审会议")
    public Result<ReviewMeeting> create(@RequestBody ReviewMeeting meeting, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        meeting.setCreatedBy(userId);
        meeting.setUpdatedBy(userId);
        if (meeting.getStatus() == null) {
            meeting.setStatus("DRAFT");
        }
        meetingService.save(meeting);
        return Result.success(meeting);
    }

    @GetMapping
    @Operation(summary = "分页查询评审会议")
    public Result<Page<ReviewMeeting>> list(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) String status) {
        LambdaQueryWrapper<ReviewMeeting> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(ReviewMeeting::getProjectId, projectId);
        }
        if (stageId != null) {
            wrapper.eq(ReviewMeeting::getStageId, stageId);
        }
        if (status != null) {
            wrapper.eq(ReviewMeeting::getStatus, status);
        }
        wrapper.orderByDesc(ReviewMeeting::getCreatedAt);
        return Result.success(meetingService.page(new Page<>(pageNo, pageSize), wrapper));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取评审会议详情")
    public Result<ReviewMeeting> getById(@PathVariable Long id) {
        return Result.success(meetingService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新评审会议")
    public Result<ReviewMeeting> update(@PathVariable Long id, @RequestBody ReviewMeeting meeting, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        meeting.setId(id);
        meeting.setUpdatedBy(userId);
        meetingService.updateById(meeting);
        return Result.success(meetingService.getById(id));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新会议状态")
    public Result<ReviewMeeting> updateStatus(@PathVariable Long id, @RequestParam String status, Authentication authentication) {
        ReviewMeeting meeting = meetingService.getById(id);
        if (meeting == null) {
            return Result.error("NOT_FOUND", "评审会议不存在");
        }
        meeting.setStatus(status);
        meeting.setUpdatedBy((Long) authentication.getPrincipal());
        meetingService.updateById(meeting);
        return Result.success(meeting);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除评审会议")
    public Result<Void> delete(@PathVariable Long id) {
        meetingService.removeById(id);
        return Result.success();
    }
}
