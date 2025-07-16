package com.subBike.server.service;

import java.sql.Struct;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import com.subBike.server.entity.SubAmount;
import com.subBike.server.entity.dto.AmountDto;
import com.subBike.server.entity.dto.DateAmountDto;
import com.subBike.server.entity.dto.TimeAmountDto;
import com.subBike.server.entity.dto.TimeSlotDto;
import com.subBike.server.entity.id.SubAmountID;
import org.apache.ibatis.annotations.Param;

public interface ISubAmountService {
      public List<AmountDto> findByDate(Date date);

      public List<DateAmountDto> findTotal();
      public List<TimeAmountDto> getTrend( Date date);

      public List<DateAmountDto> getWeeklyTotals(String station, Date endDate);


      public List<AmountDto>getMap(Date date);
      public List<TimeSlotDto>getpredict(LocalDate date, String station);
}

