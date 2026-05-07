package com.military.doc.modules.system.service.impl;

import com.military.doc.common.exception.BusinessException;
import com.military.doc.common.security.JwtTokenProvider;
import com.military.doc.modules.system.entity.SysUser;
import com.military.doc.modules.system.mapper.SysUserMapper;
import com.military.doc.modules.system.service.AuthService;
import com.military.doc.modules.system.service.SysUserService;
import com.military.doc.modules.system.vo.LoginRequest;
import com.military.doc.modules.system.vo.LoginResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserService.getByUsername(request.getUsername());
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw BusinessException.validation("密码错误");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw BusinessException.validation("用户已被禁用");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setRoles(sysUserMapper.selectRoleCodesByUserId(user.getId()));
        return response;
    }
}