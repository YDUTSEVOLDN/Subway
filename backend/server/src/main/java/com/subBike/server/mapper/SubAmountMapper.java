package com.subBike.server.mapper;
import com.subBike.server.entity.dto.AmountDto;
import com.subBike.server.entity.id.SubAmountID;

import com.subBike.server.entity.SubAmount;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository //spring bean
public interface SubAmountMapper extends JpaRepository<SubAmount, SubAmountID> {
    @Select("SELECT station, SUM(in_num) AS `in`, SUM(out_num) AS `out`, " +
            "(SUM(in_num) + SUM(out_num)) AS total " +
            "FROM sub_amount " +
            "WHERE date = #{date} " +
            "GROUP BY station " +
            "ORDER BY total DESC " +
            "LIMIT 10")
    @Results({
            @Result(property = "station", column = "station"),
            @Result(property = "inNum", column = "in"),  // 假设 AmountDto 中字段名为 inNum
            @Result(property = "outNum", column = "out"),  // 假设 AmountDto 中字段名为 outNum
            @Result(property = "total", column = "total")
    })
    List<AmountDto> findByDate(Date date);


}
