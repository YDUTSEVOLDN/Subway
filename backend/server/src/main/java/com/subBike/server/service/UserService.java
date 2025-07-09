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
        User userpojo=new User();
        BeanUtils.copyProperties(user,userpojo);
        userMapper.save(userpojo);
        //mapper
    }



}
