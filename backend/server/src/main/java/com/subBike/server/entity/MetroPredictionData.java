//package com.subBike.server.entity;
//import jakarta.persistence.*;
//
//import java.time.LocalDate;
//import java.time.LocalTime;
//import java.time.LocalDateTime;
//import java.math.BigDecimal;
//import jakarta.persistence.Entity;
//
//// 2. 预测地铁流量数据实体类
//@Entity
//@Table(name = "metro_prediction_data",
//        indexes = {
//                @Index(name = "idx_pred_date_station", columnList = "predicted_date, station"),
//                @Index(name = "idx_pred_batch", columnList = "prediction_batch_id"),
//                @Index(name = "idx_pred_time", columnList = "prediction_time"),
//                @Index(name = "idx_model_version", columnList = "model_version")
//        })
//public class MetroPredictionData {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    // 预测的目标日期和时间
//    @Column(name = "predicted_date", nullable = false)
//    private LocalDate predictedDate;
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
//    // 预测结果
//    @Column(name = "predicted_in_count", nullable = false)
//    private Integer predictedInCount;
//
//    @Column(name = "predicted_out_count", nullable = false)
//    private Integer outCount;
//
//    @Column(name = "is_transfer", nullable = false)
//    private Boolean isTransfer;
//
//    // 预测时使用的天气数据
//    @Column(name = "temperature", precision = 10, scale = 6)
//    private BigDecimal temperature;
//
//    @Column(name = "humidity", precision = 10, scale = 6)
//    private BigDecimal humidity;
//
//    @Column(name = "wind_speed", precision = 10, scale = 6)
//    private BigDecimal windSpeed;
//
//    // 预测元信息
//    @Column(name = "prediction_time", nullable = false)
//    private LocalDateTime predictionTime;
//
//    @Column(name = "model_version", nullable = false, length = 50)
//    private String modelVersion;
//
//    @Column(name = "confidence_score", precision = 5, scale = 4)
//    private BigDecimal confidenceScore;
//
//    @Column(name = "prediction_batch_id", nullable = false)
//    private String predictionBatchId; // 批次ID，便于管理同一次预测的所有数据
//
//    @Column(name = "algorithm_type", length = 50, columnDefinition = "VARCHAR(50) DEFAULT 'LSTM'")
//    private String algorithmType = "LSTM";
//
//    // 预测准确性验证（如果有真实数据对比）
//    @Column(name = "actual_in_count")
//    private Integer actualInCount;
//
//    @Column(name = "actual_out_count")
//    private Integer actualOutCount;
//
//    @Column(name = "accuracy_score", precision = 5, scale = 4)
//    private BigDecimal accuracyScore;
//
//    @Column(name = "verified_at")
//    private LocalDateTime verifiedAt;
//
//    // 预测状态
//    @Enumerated(EnumType.STRING)
//    @Column(name = "status", nullable = false)
//    private PredictionStatus status = PredictionStatus.ACTIVE;
//
//    @Column(name = "created_at", nullable = false, updatable = false)
//    private LocalDateTime createdAt;
//
//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt;
//
//    // 预测状态枚举
//    public enum PredictionStatus {
//        ACTIVE,     // 有效预测
//        EXPIRED,    // 已过期
//        VERIFIED,   // 已验证
//        DEPRECATED  // 已废弃（被新预测替代）
//    }
//
//    // 构造函数
//    public MetroPredictionData() {
//        this.createdAt = LocalDateTime.now();
//        this.updatedAt = LocalDateTime.now();
//        this.predictionTime = LocalDateTime.now();
//    }
//
//    public MetroPredictionData(LocalDate predictedDate, String station, String district,
//                               LocalTime timeSlot, Integer predictedInCount, Integer predictedOutCount,
//                               Boolean isTransfer, String modelVersion, BigDecimal confidenceScore,
//                               String predictionBatchId) {
//        this();
//        this.predictedDate = predictedDate;
//        this.station = station;
//        this.district = district;
//        this.timeSlot = timeSlot;
//        this.predictedInCount = predictedInCount;
//        this.outCount = predictedOutCount;
//        this.isTransfer = isTransfer;
//        this.modelVersion = modelVersion;
//        this.confidenceScore = confidenceScore;
//        this.predictionBatchId = predictionBatchId;
//    }
//
//    // 验证预测准确性
//    public void verifyPrediction(Integer actualInCount, Integer actualOutCount) {
//        this.actualInCount = actualInCount;
//        this.actualOutCount = actualOutCount;
//        this.verifiedAt = LocalDateTime.now();
//        this.status = PredictionStatus.VERIFIED;
//
//        // 计算准确性评分（示例：基于MAPE）
//        if (actualInCount != null && predictedInCount != null && actualInCount > 0) {
//            double inAccuracy = 1.0 - Math.abs((double)(actualInCount - predictedInCount)) / actualInCount;
//            double outAccuracy = 1.0 - Math.abs((double)(actualOutCount - outCount)) / actualOutCount;
//            this.accuracyScore = BigDecimal.valueOf((inAccuracy + outAccuracy) / 2.0);
//        }
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
//    public LocalDate getPredictedDate() { return predictedDate; }
//    public void setPredictedDate(LocalDate predictedDate) { this.predictedDate = predictedDate; }
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
//    public Integer getPredictedInCount() { return predictedInCount; }
//    public void setPredictedInCount(Integer predictedInCount) { this.predictedInCount = predictedInCount; }
//
//    public Integer getOutCount() { return outCount; }
//    public void setOutCount(Integer outCount) { this.outCount = outCount; }
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
//    public LocalDateTime getPredictionTime() { return predictionTime; }
//    public void setPredictionTime(LocalDateTime predictionTime) { this.predictionTime = predictionTime; }
//
//    public String getModelVersion() { return modelVersion; }
//    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
//
//    public BigDecimal getConfidenceScore() { return confidenceScore; }
//    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
//
//    public String getPredictionBatchId() { return predictionBatchId; }
//    public void setPredictionBatchId(String predictionBatchId) { this.predictionBatchId = predictionBatchId; }
//
//    public String getAlgorithmType() { return algorithmType; }
//    public void setAlgorithmType(String algorithmType) { this.algorithmType = algorithmType; }
//
//    public Integer getActualInCount() { return actualInCount; }
//    public void setActualInCount(Integer actualInCount) { this.actualInCount = actualInCount; }
//
//    public Integer getActualOutCount() { return actualOutCount; }
//    public void setActualOutCount(Integer actualOutCount) { this.actualOutCount = actualOutCount; }
//
//    public BigDecimal getAccuracyScore() { return accuracyScore; }
//    public void setAccuracyScore(BigDecimal accuracyScore) { this.accuracyScore = accuracyScore; }
//
//    public LocalDateTime getVerifiedAt() { return verifiedAt; }
//    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
//
//    public PredictionStatus getStatus() { return status; }
//    public void setStatus(PredictionStatus status) { this.status = status; }
//
//    public LocalDateTime getCreatedAt() { return createdAt; }
//    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
//
//    public LocalDateTime getUpdatedAt() { return updatedAt; }
//    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
//}
