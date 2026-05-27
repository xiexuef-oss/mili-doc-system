package com.military.doc.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.military.doc.common.result.Result;
import com.military.doc.modules.system.entity.SysPermission;
import com.military.doc.modules.system.service.SysPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
@Tag(name = "权限管理")
public class SysPermissionController {

    @Autowired
    private SysPermissionService sysPermissionService;

    @PostMapping
    @Operation(summary = "创建权限")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SysPermission> create(@RequestBody SysPermission perm) {
        sysPermissionService.save(perm);
        return Result.success(perm);
    }

    @GetMapping
    @Operation(summary = "查询全部权限(平铺)")
    public Result<List<SysPermission>> list() {
        List<SysPermission> list = sysPermissionService.list(
                new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getOrderNum));
        return Result.success(list);
    }

    @GetMapping("/tree")
    @Operation(summary = "查询权限树")
    public Result<List<SysPermission>> tree() {
        List<SysPermission> all = sysPermissionService.list(
                new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getOrderNum));
        return Result.success(all);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取权限详情")
    public Result<SysPermission> getById(@PathVariable Long id) {
        return Result.success(sysPermissionService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新权限")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SysPermission> update(@PathVariable Long id, @RequestBody SysPermission perm) {
        perm.setId(id);
        sysPermissionService.updateById(perm);
        return Result.success(sysPermissionService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除权限")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        sysPermissionService.removeById(id);
        return Result.success();
    }
}
