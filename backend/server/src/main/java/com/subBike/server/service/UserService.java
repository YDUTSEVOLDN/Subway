package com.subBike.server.service;

import com.subBike.server.entity.User;
import com.subBike.server.mapper.UserMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.subBike.server.entity.dto.UserDto;

@Service    //spring bean
public class UserService implements IUserService{
    @Autowired
    UserMapper userMapper;
    @Override
    public void add(UserDto user){
        if (userMapper.existsByUserName(user.getUserName())) {
            throw new RuntimeException("用户名已存在，请选择其他用户名");
        }

        // 2. 检查邮箱是否已存在
        if (userMapper.existsByEmail(user.getEmail())) {
            throw new RuntimeException("邮箱已被注册，请使用其他邮箱");
        }

        User userpojo=new User();
        BeanUtils.copyProperties(user,userpojo);
        userMapper.save(userpojo);
        //mapper
    }

    public User get(Integer UserId){
        User searchUser=new User();
        return searchUser;
    }

}
