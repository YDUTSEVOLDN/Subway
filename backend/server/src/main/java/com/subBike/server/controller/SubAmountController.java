package com.subBike.server.controller;

import com.subBike.server.service.ISubAmountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController//接口返回对象，转换成json文本
@CrossOrigin("*")
@RequestMapping("/api/subway")//用/user/**访问
@Tag(name = "地铁流量", description = "地铁流量的API接口")
public class SubAmountController {
    @Autowired
    ISubAmountService subService;

//    @GetMapping
//    @Operation(summary = "搜索用户",description = "搜索对应用户")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "用户创建成功"),
//            @ApiResponse(responseCode = "400", description = "请求参数错误"),
//            @ApiResponse(responseCode = "500", description = "服务器内部错误")
//    })
//    public ResponseEntity<String> calculate(
//           @RequestParam("Date") Date date,
//           @RequestParam("Request") String request
//    )
//    {
//        try{
//
//        }
//    }

}
