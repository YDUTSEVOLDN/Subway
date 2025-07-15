package com.subBike.server.service;

import com.subBike.server.mapper.BikeReqMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
@Service
public class BikeReqService implements IBikeReqService {
    @Autowired
    BikeReqMapper bikeReqMapper;
    @Override
    public Long getCount(Date date, String station){

        Long bikes= bikeReqMapper.get_num(date,station);
        Long pre_bikes= Math.round(bikeReqMapper.get_avg(date,station));

        return pre_bikes-bikes;
    }
}
