//package com.subBike.server.entity;
//import jakarta.persistence.*;
//
//import java.time.LocalDate;
//import java.time.LocalTime;
//import java.time.LocalDateTime;
//import java.math.BigDecimal;
//import jakarta.persistence.Entity;
//
//
//import java.time.LocalDate;
//import java.time.LocalTime;
//import java.time.LocalDateTime;
//import java.math.BigDecimal;
//
//@Entity
//@Table(name = "metro_historical_data",
//        indexes = {
//                @Index(name = "idx_date_station", columnList = "date, station"),
//                @Index(name = "idx_station_time", columnList = "station, time_slot"),
//                @Index(name = "idx_date_time", columnList = "date, time_slot")
//        })
//public class MetroHistoricalData {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(name = "date", nullable = false)
//    private LocalDate date;
//
//    @Column(name = "station", nullable = false, length = 100)
//    private String station;
//
//    @Column(name = "district", nullable = false, length = 50)
//    private String district;
//
//    @Column(name = "time_slot", nullable = false)
//    private LocalTime timeSlot;
//
//    @Column(name = "in_count", nullable = false)
//    private Integer inCount;
//
//    @Column(name = "out_count", nullable = false)
//    private Integer outCount;
//
//    @Column(name = "time_only")
//    private LocalTime timeOnly;
//
//    @Column(name = "is_transfer", nullable = false)
//    private Boolean isTransfer;
//
//    @Column(name = "temperature", precision = 10, scale = 6)
//    private BigDecimal temperature;
//
//    @Column(name = "humidity", precision = 10, scale = 6)
//    private BigDecimal humidity;
//
//    @Column(name = "wind_speed", precision = 10, scale = 6)
//    private BigDecimal windSpeed;
//
//    @Column(name = "created_at", nullable = false, updatable = false)
//    private LocalDateTime createdAt;
//
//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt;
//
//    // 数据来源标识
//    @Column(name = "data_source", length = 50, columnDefinition = "VARCHAR(50) DEFAULT 'manual'")
//    private String dataSource = "manual"; // manual, import, api
//
//    // 构造函数
//    public MetroHistoricalData() {
//        this.createdAt = LocalDateTime.now();
//        this.updatedAt = LocalDateTime.now();
//    }
//
//    public MetroHistoricalData(LocalDate date, String station, String district,
//                               LocalTime timeSlot, Integer inCount, Integer outCount,
//                               Boolean isTransfer, BigDecimal temperature,
//                               BigDecimal humidity, BigDecimal windSpeed) {
//        this();
//        this.date = date;
//        this.station = station;
//        this.district = district;
//        this.timeSlot = timeSlot;
//        this.inCount = inCount;
//        this.outCount = outCount;
//        this.timeOnly = timeSlot;
//        this.isTransfer = isTransfer;
//        this.temperature = temperature;
//        this.humidity = humidity;
//        this.windSpeed = windSpeed;
//    }
//
//    @PreUpdate
//    protected void onUpdate() {
//        this.updatedAt = LocalDateTime.now();
//    }
//
//    // Getter和Setter方法
//    public Long getId() { return id; }
//    public void setId(Long id) { this.id = id; }
//
//    public LocalDate getDate() { return date; }
//    public void setDate(LocalDate date) { this.date = date; }
//
//    public String getStation() { return station; }
//    public void setStation(String station) { this.station = station; }
//
//    public String getDistrict() { return district; }
//    public void setDistrict(String district) { this.district = district; }
//
//    public LocalTime getTimeSlot() { return timeSlot; }
//    public void setTimeSlot(LocalTime timeSlot) { this.timeSlot = timeSlot; }
//
//    public Integer getInCount() { return inCount; }
//    public void setInCount(Integer inCount) { this.inCount = inCount; }
//
//    public Integer getOutCount() { return outCount; }
//    public void setOutCount(Integer outCount) { this.outCount = outCount; }
//
//    public LocalTime getTimeOnly() { return timeOnly; }
//    public void setTimeOnly(LocalTime timeOnly) { this.timeOnly = timeOnly; }
//
//    public Boolean getIsTransfer() { return isTransfer; }
//    public void setIsTransfer(Boolean isTransfer) { this.isTransfer = isTransfer; }
//
//    public BigDecimal getTemperature() { return temperature; }
//    public void setTemperature(BigDecimal temperature) { this.temperature = temperature; }
//
//    public BigDecimal getHumidity() { return humidity; }
//    public void setHumidity(BigDecimal humidity) { this.humidity = humidity; }
//
//    public BigDecimal getWindSpeed() { return windSpeed; }
//    public void setWindSpeed(BigDecimal windSpeed) { this.windSpeed = windSpeed; }
//
//    public LocalDateTime getCreatedAt() { return createdAt; }
//    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
//
//    public LocalDateTime getUpdatedAt() { return updatedAt; }
//    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
//
//    public String getDataSource() { return dataSource; }
//    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
//}
//
