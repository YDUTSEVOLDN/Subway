package com.subBike.server.mapper;
import com.subBike.server.entity.id.SubAmountID;

import com.subBike.server.entity.SubAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository //spring bean
public interface SubAmountMapper extends JpaRepository<SubAmount, SubAmountID> {
}
