package com.subBike.server.entity.dto;

import io.swagger.models.auth.In;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "流量数据传输对象")
public class AmountDto {

    @Schema(description = "站点名", example = "西直门", required = true)
    private String station;

    @Schema(description = "进人数", example = "123456", required = true)
    private Integer inNum;

    @Schema(description = "出人数", example = "123", required = true)
    private Integer outNum;
    @Schema(description = "总人数", example = "123579", required = true)
    private Integer total;

}
