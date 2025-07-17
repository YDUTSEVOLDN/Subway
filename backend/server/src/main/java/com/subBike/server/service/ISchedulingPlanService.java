package com.subBike.server.service;

import com.subBike.server.entity.SchedulingPlan;
import java.util.List;
import java.util.Optional;

public interface ISchedulingPlanService {
    List<SchedulingPlan> getAllPlans();
    Optional<SchedulingPlan> getPlanById(Long id);
    SchedulingPlan savePlan(SchedulingPlan plan, Long userId); // Include userId for ownership
    SchedulingPlan updatePlan(Long id, SchedulingPlan planDetails);
    void deletePlan(Long id);
    List<SchedulingPlan> getPlansForCurrentUser();
} 