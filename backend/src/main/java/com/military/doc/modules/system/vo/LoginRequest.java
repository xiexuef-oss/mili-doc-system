package com.military.doc.modules.system.vo;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}