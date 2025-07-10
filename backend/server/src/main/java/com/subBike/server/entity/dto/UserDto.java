package com.subBike.server.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "用户数据传输对象")
public class UserDto {

    @Schema(description = "用户名", example = "张三", required = true)
    private String userName;

    @Schema(description = "密码", example = "123456", required = true)
    private String password;

    @Schema(description = "邮箱", example = "zhangsan@example.com", required = true)
    private String email;

    UserDto(String name,String password,String email){
        userName=name;
        this.password=password;
        this.email=email;
    }
    // getter和setter方法保持不变
    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}