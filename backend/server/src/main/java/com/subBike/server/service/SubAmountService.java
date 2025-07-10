package com.subBike.server.service;

import com.subBike.server.entity.SubAmount;
import com.subBike.server.entity.dto.AmountDto;
import com.subBike.server.entity.dto.DateAmountDto;
import com.subBike.server.entity.dto.TimeAmountDto;
import com.subBike.server.entity.id.SubAmountID;
import com.subBike.server.mapper.SubAmountMapper;
import io.swagger.v3.oas.annotations.Parameter;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SubAmountService implements ISubAmountService{
    @Autowired
    SubAmountMapper subAmountmapper;
    @Override
    public List<AmountDto> findByDate(Date date){

        List<AmountDto> Sublist =subAmountmapper.findByDate(date);

        return Sublist;
    }
   @Override
   public List<DateAmountDto> findTotal(){

        return subAmountmapper.findTotal();
   }

    @Override
    public List<DateAmountDto> getWeeklyTotals(String station, Date endDate) {
        // 计算 endDate 减去 6 天的起始日期
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(endDate);
        calendar.add(Calendar.DAY_OF_YEAR, -6); // 减去 6 天
        Date startDate = calendar.getTime();
        return subAmountmapper.getWeeklyTotals(station, startDate, endDate);
    }

    @Override
    public List<AmountDto>getMap(Date date){

        return subAmountmapper.getMap(date);
    }
    @Override
    public List<TimeAmountDto> getTrend(Date date){

        return subAmountmapper.getTrend(date);
    }



}

