package com.subBike.server.service;

import com.subBike.server.entity.dto.*;

import java.util.Date;
import java.util.List;

public interface IBikeAmountService {
    public List<StationBikeDto> findByDate(Date date);

    public List<StationBikeDto> findTotal(Date date);
    public List<DateBikeDto> findByStation(String station);


}
