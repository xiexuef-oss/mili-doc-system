package com.military.doc.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.military.doc.common.result.Result;
import com.military.doc.modules.system.entity.SysPermission;
import com.military.doc.modules.system.mapper.SysPermissionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
@Tag(name = "权限管理")
public class SysPermissionController {

    @Autowired
    private SysPermissionMapper sysPermissionMapper;

    @PostMapping
    @Operation(summary = "创建权限")
    public Result<SysPermission> create(@RequestBody SysPermission perm) {
        sysPermissionMapper.insert(perm);
        return Result.success(perm);
    }

    @GetMapping
    @Operation(summary = "查询全部权限(平铺)")
    public Result<List<SysPermission>> list() {
        List<SysPermission> list = sysPermissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getOrderNum));
        return Result.success(list);
    }

    @GetMapping("/tree")
    @Operation(summary = "查询权限树")
    public Result<List<SysPermission>> tree() {
        List<SysPermission> all = sysPermissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getOrderNum));
        return Result.success(all);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取权限详情")
    public Result<SysPermission> getById(@PathVariable Long id) {
        return Result.success(sysPermissionMapper.selectById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新权限")
    public Result<SysPermission> update(@PathVariable Long id, @RequestBody SysPermission perm) {
        perm.setId(id);
        sysPermissionMapper.updateById(perm);
        return Result.success(sysPermissionMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除权限")
    public Result<Void> delete(@PathVariable Long id) {
        sysPermissionMapper.deleteById(id);
        return Result.success();
    }
}
