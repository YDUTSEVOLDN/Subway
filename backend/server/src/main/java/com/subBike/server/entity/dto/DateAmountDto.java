package com.subBike.server.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.sql.Date;

@Schema(description = "日期流量数据传输对象")
public class DateAmountDto {
    @Schema(description = "日期", example = "2019-05-03", required = true)
    private Date date;

    public DateAmountDto(Date date, Long inNum, Long outNum) {
        this.date = date;
        this.inNum = inNum;
        this.outNum = outNum;
    }

    @Schema(description = "进人数", example = "123456", required = true)
    private Long inNum;

    @Schema(description = "出人数", example = "123", required = true)
    private Long outNum;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
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
