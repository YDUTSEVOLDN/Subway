package com.subBike.server.entity.dto;


import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "流量数据传输对象")
public class AmountDto {

    @Schema(description = "站点名", example = "西直门", required = true)
    private String station;

    @Schema(description = "进人数", example = "123456", required = true)
    private Long inNum;

    @Schema(description = "出人数", example = "123", required = true)
    private Long outNum;

    public AmountDto(String station, Long inNum, Long outNum) {
        this.station = station;
        this.inNum = inNum;
        this.outNum = outNum;
    }

    public Long getInNum() {
        return inNum;
    }

    public String getStation() {
        return station;
    }

    public Long getOutNum() {
        return outNum;
    }

    public void setInNum(Long inNum) {
        this.inNum = inNum;
    }

    public void setOutNum(Long outNum) {
        this.outNum = outNum;
    }

    public void setStation(String station) {
        this.station = station;
    }
}
