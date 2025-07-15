package com.subBike.server.service;

import com.subBike.server.entity.dto.DateBikeDto;
import com.subBike.server.entity.dto.StationBikeDto;

import java.util.Date;
import java.util.List;




public interface IBikeReqService  {
    public Long getCount(Date date,String station);



}
