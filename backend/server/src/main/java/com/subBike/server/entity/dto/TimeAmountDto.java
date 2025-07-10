package com.subBike.server.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.sql.Date;
@Schema(description = "时刻流量数据传输对象")
public class TimeAmountDto {
    @Schema(description = "时间", example = "8", required = true)
    private Integer time;

    public TimeAmountDto(Integer time, Long inNum, Long outNum) {
        this.time = time;
        this.inNum = inNum;
        this.outNum = outNum;
    }

    @Schema(description = "进人数", example = "123456", required = true)
    private Long inNum;

    @Schema(description = "出人数", example = "123", required = true)
    private Long outNum;

    public Integer getTime() {
        return time;
    }


    public void setTime(Integer time) {
        this.time = time;
    }

    public Long getInNum() {
        return inNum;
    }

    public void setInNum(Long inNum) {
        this.inNum = inNum;
    }

    public Long getOutNum() {
        return outNum;
    }

    public void setOutNum(Long outNum) {
        this.outNum = outNum;
    }
}
