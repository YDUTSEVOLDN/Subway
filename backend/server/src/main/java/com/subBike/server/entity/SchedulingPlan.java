package com.subBike.server.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(name = "scheduling_plans")
@Data
public class SchedulingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "source_station_name")
    private String sourceStationName;
    
    @Column(name = "target_station_name")
    private String targetStationName;

    @Column(name = "bike_count")
    private Integer bikeCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduleTime;

    private Integer priority;

    @Lob
    @Column(name = "path_data", columnDefinition = "TEXT")
    private String pathData;

    @Enumerated(EnumType.STRING)
    private PlanStatus status;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User createdBy;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
} 