package com.military.doc.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.system.entity.SysRolePermission;
import com.military.doc.modules.system.mapper.SysRolePermissionMapper;
import com.military.doc.modules.system.service.SysRolePermissionService;
import org.springframework.stereotype.Service;

@Service
public class SysRolePermissionServiceImpl extends ServiceImpl<SysRolePermissionMapper, SysRolePermission>
        implements SysRolePermissionService {
}
