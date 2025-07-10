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
    User get(String username);//获取用户信息
    boolean exsitByName(String username);//查找是否存在用户
    void edit(UserDto old_user,UserDto new_user);//修改用户信息
    void delete(UserDto user);//删除用户信息


    public User getUserByUsername(String username) ;
    User authenticate(String username, String rawPassword);



}