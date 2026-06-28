package com.military.doc.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.military.doc.modules.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT DISTINCT r.role_code FROM sys_user_role ur " +
            "JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0 AND r.status = 'ACTIVE' " +
            "WHERE ur.user_id = #{userId} AND ur.deleted = 0")
    List<String> selectRoleCodesByUserId(Long userId);

    @Select("SELECT DISTINCT p.permission_code FROM sys_user_role ur " +
            "JOIN sys_role_permission rp ON rp.role_id = ur.role_id AND rp.deleted = 0 " +
            "JOIN sys_permission p ON p.id = rp.permission_id AND p.deleted = 0 " +
            "WHERE ur.user_id = #{userId} AND ur.deleted = 0")
    List<String> selectPermissionCodesByUserId(Long userId);
}