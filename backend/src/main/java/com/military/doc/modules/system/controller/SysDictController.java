package com.military.doc.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.military.doc.common.result.Result;
import com.military.doc.modules.system.entity.SysDict;
import com.military.doc.modules.system.mapper.SysDictMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dicts")
@Tag(name = "字典管理")
public class SysDictController {

    @Autowired
    private SysDictMapper sysDictMapper;

    @GetMapping("/types")
    @Operation(summary = "获取所有字典类型")
    public Result<List<String>> listTypes() {
        List<String> types = sysDictMapper.selectList(null).stream()
            .map(SysDict::getDictType)
            .distinct()
            .sorted()
            .toList();
        return Result.success(types);
    }

    @GetMapping("/items/{dictType}")
    @Operation(summary = "按类型获取字典项列表")
    public Result<List<SysDict>> listByType(@PathVariable String dictType) {
        List<SysDict> list = sysDictMapper.selectList(
            new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getDictType, dictType)
                .eq(SysDict::getStatus, "ACTIVE")
                .orderByAsc(SysDict::getOrderNum)
        );
        return Result.success(list);
    }

    @PostMapping
    @Operation(summary = "创建字典项")
    public Result<SysDict> create(@RequestBody SysDict dict) {
        if (dict.getStatus() == null) {
            dict.setStatus("ACTIVE");
        }
        sysDictMapper.insert(dict);
        return Result.success(dict);
    }

    @GetMapping
    @Operation(summary = "分页查询字典项")
    public Result<Page<SysDict>> list(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "50") Integer pageSize,
            @RequestParam(required = false) String dictType) {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        if (dictType != null && !dictType.isEmpty()) {
            wrapper.eq(SysDict::getDictType, dictType);
        }
        wrapper.orderByAsc(SysDict::getDictType, SysDict::getOrderNum);
        return Result.success(sysDictMapper.selectPage(new Page<>(pageNo, pageSize), wrapper));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新字典项")
    public Result<SysDict> update(@PathVariable Long id, @RequestBody SysDict dict) {
        dict.setId(id);
        sysDictMapper.updateById(dict);
        return Result.success(sysDictMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除字典项")
    public Result<Void> delete(@PathVariable Long id) {
        sysDictMapper.deleteById(id);
        return Result.success();
    }
}
