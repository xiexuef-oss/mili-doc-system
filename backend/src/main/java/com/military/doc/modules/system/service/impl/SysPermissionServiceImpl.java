package com.military.doc.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.system.entity.SysPermission;
import com.military.doc.modules.system.mapper.SysPermissionMapper;
import com.military.doc.modules.system.service.SysPermissionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission> implements SysPermissionService {

    @Override
    public List<SysPermission> getTree() {
        List<SysPermission> all = this.list(new LambdaQueryWrapper<SysPermission>()
                .orderByAsc(SysPermission::getOrderNum));
        Map<Long, List<SysPermission>> childrenMap = all.stream()
                .filter(p -> p.getParentId() != null && p.getParentId() > 0)
                .collect(Collectors.groupingBy(SysPermission::getParentId));

        List<SysPermission> tree = new ArrayList<>();
        for (SysPermission perm : all) {
            if (perm.getParentId() == null || perm.getParentId() == 0) {
                tree.add(perm);
            }
        }
        // Attach children is not needed since Vue will build tree from flat list
        // But we can set a transient field if needed
        return tree;
    }

    public List<SysPermission> getFlatList() {
        return this.list(new LambdaQueryWrapper<SysPermission>()
                .orderByAsc(SysPermission::getOrderNum));
    }
}
