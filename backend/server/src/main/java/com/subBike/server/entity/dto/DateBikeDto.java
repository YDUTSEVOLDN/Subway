package com.subBike.server.entity.dto;



import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;

@Schema(description = "某站全部单车数据传输对象")
public class DateBikeDto {

    @Schema(description = "站点名", example = "西直门", required = true)
    private Date date;

    @Schema(description = "单车量", example = "123456", required = true)
    private Long number;//name identically



    public DateBikeDto(Date date, Long number) {
        this.date=date;
       this.number=number;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Long getNum_ber() {
        return number;
    }

    public void setNum_ber(Long num_ber) {
        this.number = num_ber;
    }
}
