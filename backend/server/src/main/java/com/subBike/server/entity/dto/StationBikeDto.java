package com.subBike.server.entity.dto;



import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;

@Schema(description = "某天站点单车数据传输对象")
public class StationBikeDto {

    @Schema(description = "站点名", example = "西直门", required = true)
    private String stationName;

    @Schema(description = "单车", example = "123456", required = true)
    private  Long numBer;//name identically



    public StationBikeDto(String stationName, Long number) {
        this.stationName=stationName;
        this.numBer=number;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public Long getNumBer() {
        return numBer;
    }

    public void setNumBer(Long numBer) {
        this.numBer = numBer;
    }
}