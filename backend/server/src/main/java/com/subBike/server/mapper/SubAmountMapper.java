package com.subBike.server.mapper;
import com.subBike.server.entity.dto.AmountDto;
import com.subBike.server.entity.dto.DateAmountDto;
import com.subBike.server.entity.dto.TimeAmountDto;
import com.subBike.server.entity.id.SubAmountID;

import com.subBike.server.entity.SubAmount;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;


    // Projection接口 - 用于按日期统计总客流量

    @Repository
    public interface SubAmountMapper extends JpaRepository<SubAmount, SubAmountID> {

        @Query("SELECT new com.subBike.server.entity.dto.AmountDto(" +
                "s.station, SUM(s.inNum), SUM(s.outNum)) " +
                "FROM SubAmount s " +
                "WHERE s.date = :date " +
                "GROUP BY s.station " +
                "ORDER BY SUM(s.inNum + s.outNum) DESC " +
                "LIMIT 10")
        List<AmountDto> findByDate(@Param("date") Date date);

        @Query("SELECT new com.subBike.server.entity.dto.DateAmountDto(" +
                "s.date, SUM(s.inNum), SUM(s.outNum)) " +
                "FROM SubAmount s " +
                "GROUP BY s.date")
        List<DateAmountDto> findTotal();

        // 方案1：使用数据库函数（适用于 MySQL）
        @Query("SELECT new com.subBike.server.entity.dto.DateAmountDto(s.date, SUM(s.inNum), SUM(s.outNum)) " +
                "FROM SubAmount s " +
                "WHERE s.station = :station " +
                "AND s.date BETWEEN :startDate AND :endDate " +
                "GROUP BY s.date " +
                "ORDER BY s.date")
        List<DateAmountDto> getWeeklyTotals(
                @Param("station") String station,
                @Param("startDate") Date startDate,
                @Param("endDate") Date endDate);

        @Query("SELECT new com.subBike.server.entity.dto.AmountDto(" +
                "s.station, SUM(s.inNum), SUM(s.outNum)) " +
                "FROM SubAmount s " +
                "WHERE s.date = :date " +
                "GROUP BY s.station")
        List<AmountDto> getMap(@Param("date") Date date);

        @Query("SELECT new com.subBike.server.entity.dto.TimeAmountDto(" +
                "s.time, SUM(s.inNum), SUM(s.outNum)) " +
                "FROM SubAmount s " +
                "WHERE s.date = :date " +
                "GROUP BY s.time")
        List<TimeAmountDto> getTrend(@Param("date") Date date);
    }


