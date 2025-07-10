package com.subBike.server.service;

import java.util.Date;
import java.util.List;
import com.subBike.server.entity.SubAmount;
import com.subBike.server.entity.dto.AmountDto;
import com.subBike.server.entity.id.SubAmountID;

public interface ISubAmountService {
      public List<AmountDto> findByDate(Date date);
}
