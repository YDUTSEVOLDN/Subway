package com.subBike.server.controller;

import com.subBike.server.entity.SubAmount;
import com.subBike.server.entity.dto.AmountDto;
import com.subBike.server.entity.dto.DateAmountDto;
import com.subBike.server.entity.dto.TimeAmountDto;
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
@CrossOrigin("*")
@RequestMapping("/api/subway")//用/user/**访问
@Tag(name = "地铁流量", description = "地铁流量的API接口")
public class SubAmountController {
    @Autowired
    ISubAmountService subService;

    @GetMapping("/date")
    @Operation(
            summary = "按日期查询流量",
            description = "根据指定日期查询地铁各站点的流量数据，返回进站、出站和总客流量",
            parameters = {
                    @Parameter(name = "date", description = "查询日期（格式：yyyy-MM-dd）",
                            example = "2023-01-01", required = true)
            }
    )


    public ResponseEntity<List>getAmount(
            // 显式指定日期格式为 yyyy-MM-dd
             @DateTimeFormat(pattern = "yyyy-MM-dd") Date date,
            //request 要求为in、out、total
            @RequestParam("request") String request

    )
    {
        try{
            //返回 station，population的List<Map>
            List<AmountDto>sublist=new ArrayList<>();
            sublist.addAll(subService.findByDate(date));



            List<Map<String,Long>> list=new ArrayList<>();
            if(request.equals("in")) {
                for(AmountDto dto:sublist){
                    Map<String, Long> innerMap = new HashMap<>();
                    innerMap.put(dto.getStation(),dto.getInNum());
                    list.add(innerMap);
                }
            }
            else if(request.equals("out")) {
                for(AmountDto dto:sublist){
                    Map<String, Long> innerMap = new HashMap<>();
                    innerMap.put(dto.getStation(),dto.getOutNum());
                    list.add(innerMap);
                }
            }
            else{
                for(AmountDto dto:sublist){
                    Map<String, Long> innerMap = new HashMap<>();
                    innerMap.put(dto.getStation(),dto.getInNum()+dto.getOutNum());
                    list.add(innerMap);
                }
            }


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
    @GetMapping("/totals")
    @Operation(
            summary = "获取每日总流量",
            description = "查询所有日期的总客流量统计数据，返回日期、进站总量和出站总量"

    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DateAmountDto.class))),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })

    public List<DateAmountDto> getAll()
    {
        //返回每日地铁上下总客流量：date，in_num，out_num的列表
        List<DateAmountDto> list=new ArrayList<>();
        list.addAll(subService.findTotal());
        return  list;
    }

    /**
     * 查询指定站点近7天的流量数据
     * @param station 站点名称
     * @param endDate 结束日期，格式：yyyy-MM-dd
     * @return 近7天的流量数据列表
     */
    @GetMapping("/weekly")
    @Operation(
            summary = "查询近7天流量",
            description = "获取指定站点近7天的客流量数据（包括当天）",
            parameters = {
                    @Parameter(name = "station", description = "站点名称",
                            example = "北京西站", required = true),
                    @Parameter(name = "endDate", description = "结束日期（格式：yyyy-MM-dd）",
                            example = "2023-01-07", required = true)
            }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DateAmountDto.class))),
            @ApiResponse(responseCode = "400", description = "无效的参数格式"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })

    public List<DateAmountDto> getWeeklyTotals(String station,  @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        List<DateAmountDto>list=new ArrayList<>();
        list.addAll(subService.getWeeklyTotals(station, endDate));
        return  list;
    }

    /**
     * 获取指定日期的地图流量数据
     * @param date 查询日期，格式：yyyy-MM-dd
     * @return 用于地图展示的流量数据列表
     */
    @GetMapping("/map")
    @Operation(
            summary = "获取地图流量数据",
            description = "查询指定日期的各站点流量数据，返回适合地图可视化的格式",
            parameters = {
                    @Parameter(name = "date", description = "查询日期（格式：yyyy-MM-dd）",
                            example = "2023-01-01", required = true)

            }

    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AmountDto.class))),
            @ApiResponse(responseCode = "400", description = "无效的日期格式"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })

    public ResponseEntity<List> getMap(  @DateTimeFormat(pattern = "yyyy-MM-dd") Date date, @RequestParam("request") String request){
        try{
            //返回 station，population的List<Map>
            List<AmountDto>sublist=new ArrayList<>();
            sublist.addAll(subService.getMap(date));



            List<Map<String,Long>> list=new ArrayList<>();
            if(request.equals("in")) {
                for(AmountDto dto:sublist){
                    Map<String, Long> innerMap = new HashMap<>();
                    innerMap.put(dto.getStation(),dto.getInNum());
                    list.add(innerMap);
                }
            }
            else if(request.equals("out")) {
                for(AmountDto dto:sublist){
                    Map<String, Long> innerMap = new HashMap<>();
                    innerMap.put(dto.getStation(),dto.getOutNum());
                    list.add(innerMap);
                }
            }
            else{
                for(AmountDto dto:sublist){
                    Map<String, Long> innerMap = new HashMap<>();
                    innerMap.put(dto.getStation(),dto.getInNum()+dto.getOutNum());
                    list.add(innerMap);
                }
            }


            return ResponseEntity.ok(list);
        }

        catch(Exception e){
            return ResponseEntity.badRequest().body(Collections.singletonList(e.getMessage()));
        }

}

    @GetMapping("/trend")
    @Operation(
            summary = "获取一天时段流量数据",
            description = "查询指定日期各时段数据绘制趋势曲线",
            parameters = {
                    @Parameter(name = "date", description = "查询日期（格式：yyyy-MM-dd）",
                            example = "2023-01-01", required = true)
            }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AmountDto.class))),
            @ApiResponse(responseCode = "400", description = "无效的日期格式"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public  List<TimeAmountDto>  getTrend( @DateTimeFormat(pattern = "yyyy-MM-dd") Date date)
    {
        List<TimeAmountDto> list=new ArrayList<>();
        list.addAll(subService.getTrend(date));
        return list;
    }


}
