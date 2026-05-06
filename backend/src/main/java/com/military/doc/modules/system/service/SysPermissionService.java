package com.military.doc.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.military.doc.modules.system.entity.SysPermission;

import java.util.List;

public interface SysPermissionService extends IService<SysPermission> {
    List<SysPermission> getTree();
}
