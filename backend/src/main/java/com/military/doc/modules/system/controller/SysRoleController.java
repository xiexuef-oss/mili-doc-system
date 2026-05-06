package com.military.doc.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.military.doc.common.result.Result;
import com.military.doc.modules.system.entity.SysRole;
import com.military.doc.modules.system.entity.SysRolePermission;
import com.military.doc.modules.system.mapper.SysRoleMapper;
import com.military.doc.modules.system.mapper.SysRolePermissionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "角色管理")
public class SysRoleController {

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysRolePermissionMapper sysRolePermissionMapper;

    @PostMapping
    @Operation(summary = "创建角色")
    public Result<SysRole> create(@RequestBody SysRole role) {
        if (role.getStatus() == null) {
            role.setStatus("ACTIVE");
        }
        sysRoleMapper.insert(role);
        return Result.success(role);
    }

    @GetMapping
    @Operation(summary = "分页查询角色")
    public Result<Page<SysRole>> list(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(SysRole::getRoleCode, keyword)
                .or().like(SysRole::getRoleName, keyword));
        }
        wrapper.orderByAsc(SysRole::getOrderNum);
        return Result.success(sysRoleMapper.selectPage(new Page<>(pageNo, pageSize), wrapper));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取角色详情")
    public Result<SysRole> getById(@PathVariable Long id) {
        return Result.success(sysRoleMapper.selectById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新角色")
    public Result<SysRole> update(@PathVariable Long id, @RequestBody SysRole role) {
        role.setId(id);
        sysRoleMapper.updateById(role);
        return Result.success(sysRoleMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色")
    public Result<Void> delete(@PathVariable Long id) {
        sysRoleMapper.deleteById(id);
        return Result.success();
    }

    @GetMapping("/{id}/permissions")
    @Operation(summary = "获取角色的权限ID列表")
    public Result<List<Long>> getPermissions(@PathVariable Long id) {
        List<SysRolePermission> rps = sysRolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id));
        List<Long> permIds = rps.stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList());
        return Result.success(permIds);
    }

    @PutMapping("/{id}/permissions")
    @Operation(summary = "设置角色权限")
    public Result<Void> setPermissions(@PathVariable Long id, @RequestBody List<Long> permissionIds) {
        sysRolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id));
        for (Long permId : permissionIds) {
            SysRolePermission rp = new SysRolePermission();
            rp.setRoleId(id);
            rp.setPermissionId(permId);
            sysRolePermissionMapper.insert(rp);
        }
        return Result.success();
    }
}
