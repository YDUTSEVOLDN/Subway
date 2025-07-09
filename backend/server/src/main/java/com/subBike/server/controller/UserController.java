package com.subBike.server.controller;

import com.subBike.server.entity.User;
import com.subBike.server.entity.dto.UserDto;
import org.springframework.web.bind.annotation.*;

@RestController//接口返回对象，转换成json文本
@CrossOrigin("*")
@RequestMapping("/user")//用/user/**访问
public class UserController {
    //增加
//    @PostMapping
//    public String add(@RequestBody UserDto user){//自动转对象
//
//        return "suceess!";
//    }
//    //查询
//    @GetMapping
//    //修改
//    @PutMapping
//    //删除
//    @DeleteMapping
}
