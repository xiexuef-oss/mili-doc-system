package com.military.doc.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.military.doc.common.result.Result;
import com.military.doc.modules.system.entity.SysUser;
import com.military.doc.modules.system.entity.SysUserRole;
import com.military.doc.modules.system.mapper.SysUserRoleMapper;
import com.military.doc.modules.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "用户管理")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @PostMapping
    @Operation(summary = "创建用户")
    public Result<SysUser> create(@RequestBody SysUser user) {
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));
        }
        if (user.getStatus() == null) {
            user.setStatus("ACTIVE");
        }
        sysUserService.save(user);
        user.setPassword(null);
        return Result.success(user);
    }

    @GetMapping
    @Operation(summary = "分页查询用户")
    public Result<Page<SysUser>> list(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                .or().like(SysUser::getRealName, keyword)
                .or().like(SysUser::getEmail, keyword));
        }
        wrapper.orderByDesc(SysUser::getCreatedAt);
        Page<SysUser> page = sysUserService.page(new Page<>(pageNo, pageSize), wrapper);
        page.getRecords().forEach(u -> u.setPassword(null));
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情")
    public Result<SysUser> getById(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户")
    public Result<SysUser> update(@PathVariable Long id, @RequestBody SysUser user) {
        user.setId(id);
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        sysUserService.updateById(user);
        SysUser updated = sysUserService.getById(id);
        updated.setPassword(null);
        return Result.success(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
    public Result<Void> delete(@PathVariable Long id) {
        sysUserService.removeById(id);
        return Result.success();
    }

    @GetMapping("/{id}/roles")
    @Operation(summary = "获取用户的角色ID列表")
    public Result<List<Long>> getRoles(@PathVariable Long id) {
        List<SysUserRole> urs = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        List<Long> roleIds = urs.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        return Result.success(roleIds);
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "设置用户角色")
    public Result<Void> setRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        sysUserRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        for (Long roleId : roleIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(id);
            ur.setRoleId(roleId);
            sysUserRoleMapper.insert(ur);
        }
        return Result.success();
    }
}
