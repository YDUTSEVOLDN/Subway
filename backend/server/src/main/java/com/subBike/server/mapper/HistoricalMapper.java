//package com.subBike.server.mapper;
//
//import com.subBike.server.entity.MetroHistoricalData;
//import com.subBike.server.entity.dto.AmountDto;
//import com.subBike.server.entity.dto.DateAmountDto;
//import com.subBike.server.entity.dto.TimeAmountDto;
//import com.subBike.server.entity.id.SubAmountID;
//
//import com.subBike.server.entity.SubAmount;
//import org.apache.ibatis.annotations.Param;
//import org.apache.ibatis.annotations.Result;
//import org.apache.ibatis.annotations.Results;
//import org.apache.ibatis.annotations.Select;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.stereotype.Repository;
//
//import java.time.LocalDate;
//import java.util.Date;
//import java.util.List;
//
//
//// Projection接口 - 用于按日期统计总客流量
//
//@Repository
//public interface HistoricalMapper extends JpaRepository<SubAmount, SubAmountID> {
//
//
//    /**
//     * 根据日期范围查询历史数据
//     * @param startDate 开始日期
//     * @param endDate 结束日期
//     * @return 历史数据列表
//     */
//    @Query("SELECT s FROM MetroHistoricalData s WHERE s.date BETWEEN :startDate AND :endDate ORDER BY s.date ASC, s.timeSlot ASC")
//    List<MetroHistoricalData> findByDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
//
//}
//
//
