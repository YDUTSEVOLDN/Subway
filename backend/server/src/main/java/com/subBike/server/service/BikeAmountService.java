package com.subBike.server.service;

import com.subBike.server.entity.dto.*;
import com.subBike.server.mapper.BikeAmountMapper;
import com.subBike.server.mapper.SubAmountMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class BikeAmountService implements IBikeAmountService {
    @Autowired
    BikeAmountMapper bikeAmountmapper;
    @Override
    public List<StationBikeDto> findByDate(Date date){
        return bikeAmountmapper.findByDate(date);
    }
    @Override
    public List<StationBikeDto> findTotal(Date date){
        return bikeAmountmapper.findTotal(date);
    }
    @Override
    public List<DateBikeDto> findByStation(String station){
        return bikeAmountmapper.findByStation(station);
    }





}
