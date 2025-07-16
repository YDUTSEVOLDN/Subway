package com.subBike.server.service;

import com.subBike.server.entity.dto.AmountDto;
import com.subBike.server.entity.dto.DateAmountDto;
import com.subBike.server.entity.dto.TimeAmountDto;
import com.subBike.server.entity.dto.TimeSlotDto;
import com.subBike.server.mapper.SubAmountMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

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
    @Override
    public List<TimeSlotDto> getpredict(LocalDate date, String station){

        return subAmountmapper.getpredict(date,station);
    }



}

