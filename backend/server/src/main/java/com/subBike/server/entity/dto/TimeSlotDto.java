package com.subBike.server.entity.dto;



import io.swagger.v3.oas.annotations.media.Schema;

import java.sql.Date;
import java.time.LocalTime;

@Schema(description = "时刻流量数据传输对象")
public class TimeSlotDto {
    @Schema(description = "时间", example = "8", required = true)
    private LocalTime time;

    public TimeSlotDto( LocalTime time, Double inNum, Double outNum) {
        this.time = time;
        this.inNum = inNum;
        this.outNum = outNum;
    }

    @Schema(description = "进人数", example = "123456", required = true)
    private Double inNum;

    @Schema(description = "出人数", example = "123", required = true)
    private Double outNum;

    public  LocalTime getTime() {
        return time;
    }


    public void setTime( LocalTime time) {
        this.time = time;
    }

    public Double getInNum() {
        return inNum;
    }

    public void setInNum(Double inNum) {
        this.inNum = inNum;
    }

    public Double getOutNum() {
        return outNum;
    }

    public void setOutNum(Double outNum) {
        this.outNum = outNum;
    }
}
