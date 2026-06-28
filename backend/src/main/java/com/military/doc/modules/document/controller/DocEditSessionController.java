package com.military.doc.modules.document.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.military.doc.common.result.Result;
import com.military.doc.common.security.ProjectAccessGuard;
import com.military.doc.modules.document.entity.DocEditSession;
import com.military.doc.modules.document.service.DocEditSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/doc-edit-sessions")
@Tag(name = "文档编辑会话管理")
public class DocEditSessionController {

    @Autowired
    private DocEditSessionService sessionService;
    @Autowired
    private ProjectAccessGuard accessGuard;

    @PostMapping("/open")
    @Operation(summary = "开启编辑会话")
    public Result<DocEditSession> openSession(@RequestBody DocEditSession session, Authentication authentication) {
        accessGuard.requireMemberForFile(session.getDocFileId(), authentication);
        Long userId = (Long) authentication.getPrincipal();
        session.setEditorUserId(userId);
        session.setCreatedBy(userId);
        session.setUpdatedBy(userId);
        session.setSessionStatus("OPEN");
        session.setOpenedAt(LocalDateTime.now());
        sessionService.save(session);
        return Result.success(session);
    }

    @PutMapping("/{id}/submit")
    @Operation(summary = "提交编辑会话")
    public Result<DocEditSession> submitSession(@PathVariable Long id, Authentication authentication) {
        DocEditSession session = sessionService.getById(id);
        if (session == null) {
            return Result.error("NOT_FOUND", "编辑会话不存在");
        }
        accessGuard.requireMemberForFile(session.getDocFileId(), authentication);
        session.setSessionStatus("SUBMITTED");
        session.setSubmittedAt(LocalDateTime.now());
        session.setUpdatedBy((Long) authentication.getPrincipal());
        sessionService.updateById(session);
        return Result.success(session);
    }

    @PutMapping("/{id}/close")
    @Operation(summary = "关闭编辑会话")
    public Result<DocEditSession> closeSession(@PathVariable Long id, Authentication authentication) {
        DocEditSession session = sessionService.getById(id);
        if (session == null) {
            return Result.error("NOT_FOUND", "编辑会话不存在");
        }
        accessGuard.requireMemberForFile(session.getDocFileId(), authentication);
        session.setSessionStatus("CLOSED");
        session.setUpdatedBy((Long) authentication.getPrincipal());
        sessionService.updateById(session);
        return Result.success(session);
    }

    @GetMapping("/doc-file/{docFileId}")
    @Operation(summary = "获取文档的编辑会话列表")
    public Result<List<DocEditSession>> listByDocFile(@PathVariable Long docFileId) {
        List<DocEditSession> sessions = sessionService.list(
            new LambdaQueryWrapper<DocEditSession>()
                .eq(DocEditSession::getDocFileId, docFileId)
                .orderByDesc(DocEditSession::getCreatedAt)
        );
        return Result.success(sessions);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取编辑会话详情")
    public Result<DocEditSession> getById(@PathVariable Long id) {
        return Result.success(sessionService.getById(id));
    }
}
