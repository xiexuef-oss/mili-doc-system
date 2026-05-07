package com.military.doc.modules.document.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.military.doc.common.result.Result;
import com.military.doc.modules.document.entity.DocEditLock;
import com.military.doc.modules.document.service.DocEditLockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/doc-locks")
@Tag(name = "文档锁定管理")
public class DocEditLockController {

    @Autowired
    private DocEditLockService lockService;

    @PostMapping("/lock")
    @Operation(summary = "锁定文档")
    public Result<DocEditLock> lock(@RequestBody DocEditLock lock, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        // 检查是否已被锁定
        DocEditLock existLock = lockService.getOne(
            new LambdaQueryWrapper<DocEditLock>()
                .eq(DocEditLock::getDocFileId, lock.getDocFileId())
                .eq(DocEditLock::getLockStatus, "ACTIVE")
        );
        if (existLock != null) {
            return Result.error("DOCUMENT_LOCKED", "文档已被用户锁定");
        }

        lock.setLockedBy(userId);
        lock.setCreatedBy(userId);
        lock.setUpdatedBy(userId);
        lock.setLockStatus("ACTIVE");
        if (lock.getLockType() == null) {
            lock.setLockType("EDIT");
        }
        lockService.save(lock);
        return Result.success(lock);
    }

    @PutMapping("/{id}/unlock")
    @Operation(summary = "解锁文档")
    public Result<DocEditLock> unlock(@PathVariable Long id, Authentication authentication) {
        DocEditLock lock = lockService.getById(id);
        if (lock == null) {
            return Result.error("NOT_FOUND", "锁定记录不存在");
        }
        lock.setLockStatus("RELEASED");
        lock.setUpdatedBy((Long) authentication.getPrincipal());
        lockService.updateById(lock);
        return Result.success(lock);
    }

    @GetMapping("/doc-file/{docFileId}")
    @Operation(summary = "查询文档锁定状态")
    public Result<DocEditLock> getByDocFile(@PathVariable Long docFileId) {
        DocEditLock lock = lockService.getOne(
            new LambdaQueryWrapper<DocEditLock>()
                .eq(DocEditLock::getDocFileId, docFileId)
                .eq(DocEditLock::getLockStatus, "ACTIVE")
        );
        return Result.success(lock);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取锁定详情")
    public Result<DocEditLock> getById(@PathVariable Long id) {
        return Result.success(lockService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除锁定记录")
    public Result<Void> delete(@PathVariable Long id) {
        lockService.removeById(id);
        return Result.success();
    }
}
