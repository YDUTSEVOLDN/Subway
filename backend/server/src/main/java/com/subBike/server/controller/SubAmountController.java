package com.subBike.server.controller;

import com.subBike.server.entity.SubAmount;
import com.subBike.server.entity.dto.AmountDto;
import com.subBike.server.service.ISubAmountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController//接口返回对象，转换成json文本
@CrossOrigin("*")
@RequestMapping("/api/subway")//用/user/**访问
@Tag(name = "地铁流量", description = "地铁流量的API接口")
public class SubAmountController {
    @Autowired
    ISubAmountService subService;

    @GetMapping
    @Operation(summary = "搜索流量",description = "按日期搜索流量")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<String> getAmount(
            // 显式指定日期格式为 yyyy-MM-dd
            @RequestParam("Date") @DateTimeFormat(pattern = "yyyy-MM-dd") Date date

    )
    {
        try{
            List<AmountDto>list=new ArrayList<>();
            list.addAll(subService.findByDate(date));
            return ResponseEntity.ok("数据查询成功");
        }
        catch(Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }




}
