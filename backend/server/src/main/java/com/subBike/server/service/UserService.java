package com.subBike.server.service;

import com.subBike.server.entity.User;
import com.subBike.server.mapper.UserMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import net.bytebuddy.implementation.bytecode.Throw;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.subBike.server.entity.dto.UserDto;
import org.springframework.util.StringUtils;

import java.util.Optional;



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
    @Override

    public User authenticate(String username, String password) {


        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {

            return null;
        }

        try {
            // 基础密码验证：直接比较明文密码
            Optional<User> userOpt = userMapper.findByUserNameAndPassword(username, password);

            if (userOpt.isPresent()) {

                return userOpt.get();
            } else {

                return null;
            }

        } catch (Exception e) {

            return null;
        }
    }
    @Override
    public User getUserByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        return userMapper.findByUserName(username)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在：" + username));
    }
    @Override
    public User get(String username){
        return userMapper.findByUserName(username).orElseThrow(()->new EntityNotFoundException("没有指定用户"));
    }
    @Override
    public boolean exsitByName(String username){
        return userMapper.existsByUserName(username);
    }
    @Override
    public void edit(UserDto old_user,UserDto new_user){
          String old_name=old_user.getUserName(),new_name=new_user.getUserName(),new_password=new_user.getPassword(),new_email=new_user.getEmail();
          userMapper.editUser(old_name,new_name,new_password,new_email);
    }
    @Override
    public void delete(UserDto user){
        userMapper.deleteUser(user.getUserName());
    }

}
