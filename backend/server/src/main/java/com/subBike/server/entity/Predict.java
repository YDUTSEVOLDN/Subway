package com.subBike.server.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.subBike.server.entity.id.SubAmountID;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;

@Table(name="predict")
@Entity
public class Predict {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 预测日期
     */
    @Column(name = "date", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    /**
     * 地铁站名称
     */
    @Column(name = "station", nullable = false, length = 100)
    private String station;

    /**
     * 所属区域
     */
    @Column(name = "district", nullable = false, length = 50)
    private String district;

    /**
     * 预测时间点
     */
    @Column(name = "timeSlot", nullable = false)
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime timeSlot;

    /**
     * 预测进站人数
     */
    @Column(name = "in_count_pred", nullable = false)
    private Double inCountPred;

    /**
     * 预测出站人数
     */
    @Column(name = "outcount_pred", nullable = false)
    private Double outcountPred;

    public Predict(Long id, LocalDate date, String station, String district, LocalTime timeSlot, Double inCountPred, Double outcountPred) {
        this.id = id;
        this.date = date;
        this.station = station;
        this.district = district;
        this.timeSlot = timeSlot;
        this.inCountPred = inCountPred;
        this.outcountPred = outcountPred;
    }

    public Predict() {
    }

    /**
     * 创建时间
     */
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getStation() {
        return station;
    }

    public void setStation(String station) {
        this.station = station;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public LocalTime getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(LocalTime timeSlot) {
        this.timeSlot = timeSlot;
    }

    public Double getInCountPred() {
        return inCountPred;
    }

    public void setInCountPred(Double inCountPred) {
        this.inCountPred = inCountPred;
    }

    public Double getOutcountPred() {
        return outcountPred;
    }

    public void setOutcountPred(Double outcountPred) {
        this.outcountPred = outcountPred;
    }
//    @Column(name = "create_time")
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
//    private java.time.LocalDateTime createTime;
//
//    /**
//     * 更新时间
//     */
//    @Column(name = "update_time")
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
//    private java.time.LocalDateTime updateTime;
//
//    @PrePersist
//    protected void onCreate() {
//        createTime = java.time.LocalDateTime.now();
//        updateTime = java.time.LocalDateTime.now();
//    }
//
//    @PreUpdate
//    protected void onUpdate() {
//        updateTime = java.time.LocalDateTime.now();
//    }
}