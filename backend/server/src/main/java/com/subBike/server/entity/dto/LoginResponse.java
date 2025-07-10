package com.subBike.server.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// 1. 首先定义返回数据的DTO类
@Data
@AllArgsConstructor
@NoArgsConstructor

public class LoginResponse {
    private int code;
    private String message;
    private LoginData data;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LoginData {
        private String token;
        private String tokenType;
        private Long expiresIn;  // token过期时间（秒）
        private UserInfo userInfo;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserInfo {
        private Long userId;
        private String username;
        private String nickname;
        private String email;
        private String avatar;
        private List<String> roles;
    }
}