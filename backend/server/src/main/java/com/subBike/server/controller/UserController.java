package com.subBike.server.controller;

import com.subBike.server.entity.User;
import com.subBike.server.entity.dto.UserDto;
import com.subBike.server.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
@RestController//接口返回对象，转换成json文本
@CrossOrigin("*")
@RequestMapping("/api/users")//用/user/**访问
@Tag(name = "用户管理", description = "用户相关的API接口")
public class UserController {

    @Autowired
    IUserService userService;
    //增加
    @PostMapping//url:localhost:10086/user
    @Operation(summary = "添加用户", description = "创建新用户")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "用户创建成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public  ResponseEntity<String> add(
            @RequestBody
            @Parameter(description = "用户信息", required = true)
            UserDto user) {//自动转对象
        try {
            userService.add(user);
            return ResponseEntity.ok("用户注册成功");
        } catch (RuntimeException e) {
            // 返回400状态码和具体错误信息
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("服务器内部错误：" + e.getMessage());
        }


    }



//    //查询
    @GetMapping
    @Operation(summary = "搜索用户",description = "搜索对应用户")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "用户创建成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<String> search(
       @RequestBody
       @Parameter(description = "用户ID" ,required = true)
       Integer Userid){
        try{
            User checkUser=userService.get(Userid);
            return ResponseEntity.ok("用户成功登录");
        }catch(Exception e){

        }
        return null;
    }


//    //修改
//    @PutMapping
//    //删除
//    @DeleteMapping
}
