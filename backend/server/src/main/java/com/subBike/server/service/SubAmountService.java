package com.subBike.server.service;

import com.subBike.server.entity.SubAmount;
import com.subBike.server.entity.dto.AmountDto;
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






}

