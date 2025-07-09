package com.subBike.server.controller;

import com.subBike.server.entity.User;
import com.subBike.server.entity.dto.UserDto;
import com.subBike.server.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController//接口返回对象，转换成json文本
@CrossOrigin("*")
@RequestMapping("/user")//用/user/**访问
public class UserController {

    @Autowired
    IUserService userService;
    //增加
    @PostMapping//url:localhost:8080/user
    public String add(@RequestBody UserDto user){//自动转对象
        userService.add(user);

        return "suceess!";
    }
//    //查询
//    @GetMapping
//    //修改
//    @PutMapping
//    //删除
//    @DeleteMapping
}
