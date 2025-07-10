package com.subBike.server.controller;

import com.subBike.server.entity.User;
import com.subBike.server.entity.dto.LoginRequest;
import com.subBike.server.entity.dto.LoginResponse;
import com.subBike.server.entity.dto.UserDto;
import com.subBike.server.service.IUserService;
import com.subBike.server.service.JwtTokenProvider;
import com.subBike.server.service.UserService;
import io.swagger.annotations.ApiOperation;
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
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.stream.Collectors;



@RestController//接口返回对象，转换成json文本
@CrossOrigin("*")
@RequestMapping("/api/users")//用/user/**访问
@Tag(name = "用户管理", description = "用户相关的API接口")
public class UserController {
    public UserController(JwtTokenProvider jwtTokenProvider){
        this.jwtTokenProvider=jwtTokenProvider;
    }


    @Autowired
    IUserService userService;
    private final JwtTokenProvider jwtTokenProvider;
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


//
////    //查询
@PostMapping("/login")

//@ApiResponses({
//
//})
//public ResponseEntity<ApiResponse<LoginResponse.LoginData>> login(
//        @RequestBody @Valid LoginRequest loginRequest) {
//
//    try {
//        // 验证用户凭据
//        User user = userService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());
//
//        if (user == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(ApiResponse.error(401, "用户名或密码错误"));
//        }
//
//        // 生成JWT token
//        String token = jwtTokenProvider.generateToken(user);
//        Long expiresIn = jwtTokenProvider.getExpirationTime();
//
//        // 构建用户信息
//        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
//                user.getUserID(),
//                user.getUsername(),
//                user.getEmail(),
//                user.getRole().stream().map(Role::getName).collect(Collectors.toList())
//        );
//
//        return ResponseEntity.ok(ApiResponse.success(loginData));
//
//    } catch (IllegalArgumentException e) {
//        return ResponseEntity.badRequest()
//                .body(ApiResponse.error(400, e.getMessage()));
//    } catch (Exception e) {
//
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(ApiResponse.error(500, "服务器内部错误"));
//    }
//}


    //修改 要求接受修改前和修改后的用户信息
   @GetMapping
   @Operation(summary = "修改用户信息",description = "修改用户信息")
   @ApiResponses(value = {
           @ApiResponse(responseCode = "200", description = "用户创建成功"),
           @ApiResponse(responseCode = "400", description = "请求参数错误"),
           @ApiResponse(responseCode = "500", description = "服务器内部错误")
   })
   public ResponseEntity<String> edit(
            @RequestBody
            @Parameter(description="修改信息",required=true)
            UserDto user,
            UserDto newUser)
   {
       try{
           if(userService.exsitByName(newUser.getUserName())) {
               return ResponseEntity.ok("用户名重复，请重新设置");
           }
           userService.edit(user,newUser);
           return ResponseEntity.ok("用户信息修改成功");
       }
       catch(Exception e){
           return ResponseEntity.badRequest().body(e.getMessage());
       }
   }



    //删除 要求接受删除用户的信息
   @DeleteMapping
   @Operation(summary = "修改用户信息",description = "修改用户信息")
   @ApiResponses(value = {
           @ApiResponse(responseCode = "200", description = "用户创建成功"),
           @ApiResponse(responseCode = "400", description = "请求参数错误"),
           @ApiResponse(responseCode = "500", description = "服务器内部错误")
   })
   public ResponseEntity<String> delete(
           @RequestBody
           @Parameter(description="修改信息",required=true)
           UserDto user)
   {
       try{
           if(!userService.exsitByName(user.getUserName())) {
               return ResponseEntity.ok("用户不存在");
           }
           return ResponseEntity.ok("用户删除成功");
       }
       catch(Exception e){
           return ResponseEntity.badRequest().body(e.getMessage());
       }
   }
}
