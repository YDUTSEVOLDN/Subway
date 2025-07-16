package com.subBike.server.repository;

import com.subBike.server.entity.PlanStatus;
import com.subBike.server.entity.SchedulingPlan;
import com.subBike.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchedulingPlanRepository extends JpaRepository<SchedulingPlan, Long> {

    List<SchedulingPlan> findByCreatedBy(User user);

    List<SchedulingPlan> findByStatus(PlanStatus status);
} 