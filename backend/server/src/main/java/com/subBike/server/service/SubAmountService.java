package com.subBike.server.service;

import com.subBike.server.mapper.SubAmountMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class SubAmountService implements ISubAmountService{
    @Autowired
    SubAmountMapper subAmountMapper;
//    @Override
//    @Override
//   Map<String,Integer>[] getStationPopulation(Date date,String request){
//
//    }
}

