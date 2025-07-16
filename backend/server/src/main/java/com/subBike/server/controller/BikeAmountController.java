package com.subBike.server.controller;



import com.subBike.server.entity.SubAmount;
import com.subBike.server.entity.dto.*;
import com.subBike.server.service.IBikeAmountService;
import com.subBike.server.service.ISubAmountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Struct;
import java.util.*;
import java.util.stream.Collectors;

@RestController//接口返回对象，转换成json文本
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/bike")//用/user/**访问
@Tag(name = "地铁流量", description = "共享单车的API接口")
public class BikeAmountController {
    @Autowired
    IBikeAmountService bikeAmountService;

    @GetMapping("/date")
    @Operation(
            summary = "按日期查询单车量",
            description = "",
            parameters = {
                    @Parameter(name = "date", description = "查询日期（格式：yyyy-MM-dd）",
                            example = "2023-01-01", required = true)
            }
    )


    public ResponseEntity<List>getAmount(
            // 显式指定日期格式为 yyyy-MM-dd
            @DateTimeFormat(pattern = "yyyy-MM-dd") Date date


    )
    {
        try{
            //返回 station，population的List<Map>
            List<StationBikeDto>list=new ArrayList<>();
            list.addAll(bikeAmountService.findByDate(date));

            return ResponseEntity.ok(list);
        }

        catch(Exception e){
            return ResponseEntity.badRequest().body(Collections.singletonList(e.getMessage()));
        }
    }


    @GetMapping("/totals")
    @Operation(
            summary = "按日期查询所有单车量",
            description = "",
            parameters = {
                    @Parameter(name = "date", description = "查询日期（格式：yyyy-MM-dd）",
                            example = "2023-01-01", required = true)
            }
    )


    public ResponseEntity<List>getTotals(
            // 显式指定日期格式为 yyyy-MM-dd
            @DateTimeFormat(pattern = "yyyy-MM-dd") Date date


    )
    {
        try{
            //返回 station，population的List<Map>
            List<StationBikeDto>list=new ArrayList<>();
            list.addAll(bikeAmountService.findTotal(date));

            return ResponseEntity.ok(list);
        }

        catch(Exception e){
            return ResponseEntity.badRequest().body(Collections.singletonList(e.getMessage()));
        }
    }
    /**
     * 获取所有日期的总流量统计
     * @return 每日总客流量统计列表
     */
    @GetMapping("/station")
    @Operation(
            summary = "获取地铁口单车量变化",
            description = "",
            parameters = {
                    @Parameter(name = "station", description = "站点名称",
                            example = "北京西站", required = true),
            }

    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DateAmountDto.class))),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })

    public List<DateBikeDto> getByStation(String station)
    {
        //返回每日地铁上下总客流量：date，in_num，out_num的列表
        List<DateBikeDto> list=new ArrayList<>();
        list.addAll(bikeAmountService.findByStation(station));
        return  list;
    }


}
