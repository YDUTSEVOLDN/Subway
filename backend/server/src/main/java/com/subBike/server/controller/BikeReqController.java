package com.subBike.server.controller;

import com.subBike.server.service.BikeReqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController//接口返回对象，转换成json文本
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/bikeReq")//用/user/**访问
@Tag(name = "地铁流量", description = "共享单车的API接口")
public class BikeReqController {
    @Autowired
    BikeReqService bikeReqService;

    @GetMapping("/amount")
    @Operation(
            summary = "按日期查询单车量",
            description = "",
            parameters = {
                    @Parameter(name = "date", description = "查询日期（格式：yyyy-MM-dd）",
                            example = "2023-01-01", required = true),
                    @Parameter(name = "station", description = "station",
                    example = "北京站", required = true)

            }
    )

    public ResponseEntity<Object> getAmount(
            @DateTimeFormat(pattern = "yyyy-MM-dd") Date date,
            String station
//            ,@RequestParam(defaultValue = "false") boolean countOnly
           ) {

        try {
//            if (countOnly) {
                // 只返回总数
                Long count = 0L;
                count= bikeReqService.getCount(date,station);
                return ResponseEntity.ok(count);
//            } else {
//                // 返回列表数据
//                List<StationBikeDto> list = new ArrayList<>();
//                list.addAll(bikeAmountService.findByDate(date));
//                return ResponseEntity.ok(list);
//            }

        } catch (Exception e) {
            Map<String, String> error = Map.of("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }



}
