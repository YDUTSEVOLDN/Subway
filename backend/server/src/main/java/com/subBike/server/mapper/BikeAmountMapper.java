package com.subBike.server.mapper;
import com.subBike.server.entity.dto.*;
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
public interface BikeAmountMapper extends JpaRepository<SubAmount, SubAmountID> {

    @Query("SELECT new com.subBike.server.entity.dto.StationBikeDto(b.station, b.numBer)" +
            "FROM BikeAmount b " +
            "WHERE b.date = :date "
            )

//    select station,num_ber from bike_amount where date='2019-05-06';
//    select date,num_ber from bike_amount where station='西直门';
    List<StationBikeDto> findTotal(@Param("date") Date date);


    @Query("SELECT new com.subBike.server.entity.dto.StationBikeDto(b.station, b.numBer)" +
            "FROM BikeAmount b " +
            "WHERE b.date = :date " +
            "ORDER BY b.numBer DESC " +
            "LIMIT 10")

    List<StationBikeDto> findByDate(@Param("date") Date date);

    @Query("SELECT new com.subBike.server.entity.dto.DateBikeDto(b.date,b.numBer)" +
            "FROM BikeAmount b " +
            "WHERE b.station = :station ")

    List<DateBikeDto> findByStation(@Param("station") String station);



}


