package com.military.doc.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.military.doc.modules.system.entity.SysUser;

public interface SysUserService extends IService<SysUser> {
    SysUser getByUsername(String username);
}