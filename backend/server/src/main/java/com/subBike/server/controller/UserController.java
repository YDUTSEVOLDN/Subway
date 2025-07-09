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
import jakarta.persistence.EntityNotFoundException;
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
            @RequestParam("Username") String username,  // 对应 URL 中的 Username 参数
            @RequestParam("password") String password ){
        try{
            User usercopy=userService.get(username);
            if(usercopy.getPassword().equals(password)) return ResponseEntity.ok("用户成功登录");
            else return ResponseEntity.ok("用户名或密码错误，请重新输入");
        }catch(EntityNotFoundException e){
            return ResponseEntity.badRequest().body("请注册新用户");
        }
        catch(Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


//    //修改
//    @GetMapping
//    @Operation(summary = "修改用户信息",description = "修改用户信息")
//    @ApiResponses(value = {
//           @ApiResponse(responseCode = "200", description = "用户创建成功"),
 //           @ApiResponse(responseCode = "400", description = "请求参数错误"),
 //           @ApiResponse(responseCode = "500", description = "服务器内部错误")
 //   })
 //   public ResponseEntity<String> edit(
  //          @RequestBody
    //        @Parameter(description="修改信息",required=true)


   // )
//    //删除
//   @DeleteMapping
}
