package com.subBike.server.entity.dto;

import com.subBike.server.entity.PlanStatus;
import lombok.Data;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
public class SchedulingPlanDto {
    private Long id;
    private String name;
    private String description;
    private String sourceStationName;
    private String targetStationName;
    private Integer bikeCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduleTime;
    private Integer priority;
    private String pathData; // JSON string
    private PlanStatus status;
    private String createdByUsername;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}