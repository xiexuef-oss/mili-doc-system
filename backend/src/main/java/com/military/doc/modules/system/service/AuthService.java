package com.military.doc.modules.system.service;

import com.military.doc.modules.system.vo.LoginRequest;
import com.military.doc.modules.system.vo.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}