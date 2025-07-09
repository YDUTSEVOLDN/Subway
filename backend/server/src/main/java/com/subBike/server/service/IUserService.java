package com.subBike.server.service;

import com.subBike.server.entity.User;
import com.subBike.server.entity.dto.UserDto;

public interface IUserService {
    /**
     * 添加用户
     * @param user 用户信息
     * @throws RuntimeException 当用户名或邮箱已存在时抛出异常
     */
    void add(UserDto user) throws RuntimeException;

    User get(Integer id);
}