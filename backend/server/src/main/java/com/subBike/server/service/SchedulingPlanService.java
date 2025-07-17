package com.subBike.server.service;

import com.subBike.server.entity.PlanStatus;
import com.subBike.server.entity.SchedulingPlan;
import com.subBike.server.entity.User;
import com.subBike.server.repository.SchedulingPlanRepository;
import com.subBike.server.mapper.UserRepository;
import com.subBike.server.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class SchedulingPlanService implements ISchedulingPlanService {

    @Autowired
    private SchedulingPlanRepository schedulingPlanRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<SchedulingPlan> getAllPlans() {
        // In a real app, you'd likely want pagination here.
        return schedulingPlanRepository.findAll();
    }

    @Override
    public Optional<SchedulingPlan> getPlanById(Long id) {
        SchedulingPlan plan = schedulingPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Plan not found."));
        validateUserAccess(plan.getCreatedBy().getId());
        return Optional.of(plan);
    }

    @Override
    public SchedulingPlan savePlan(SchedulingPlan plan, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Error: User not found."));
        plan.setCreatedBy(user);
        plan.setStatus(PlanStatus.PENDING);
        return schedulingPlanRepository.save(plan);
    }

    @Override
    public SchedulingPlan updatePlan(Long id, SchedulingPlan planDetails) {
        SchedulingPlan existingPlan = schedulingPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Plan not found."));

        validateUserAccess(existingPlan.getCreatedBy().getId());

        existingPlan.setName(planDetails.getName());
        existingPlan.setDescription(planDetails.getDescription());
        existingPlan.setScheduleTime(planDetails.getScheduleTime());
        existingPlan.setPriority(planDetails.getPriority());
        existingPlan.setPathData(planDetails.getPathData());
        existingPlan.setStatus(planDetails.getStatus());
        
        return schedulingPlanRepository.save(existingPlan);
    }

    @Override
    public void deletePlan(Long id) {
        SchedulingPlan existingPlan = schedulingPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Plan not found."));
        
        validateUserAccess(existingPlan.getCreatedBy().getId());

        schedulingPlanRepository.deleteById(id);
    }

    @Override
    public List<SchedulingPlan> getPlansForCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetailsImpl) {
            Long userId = ((UserDetailsImpl) principal).getId();
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                return schedulingPlanRepository.findByCreatedBy(user);
            }
        }
        return Collections.emptyList();
    }
    
    private void validateUserAccess(Long ownerId) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetailsImpl) {
            UserDetailsImpl currentUser = (UserDetailsImpl) principal;
            Long currentUserId = currentUser.getId();
            
            boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin && !currentUserId.equals(ownerId)) {
                throw new AccessDeniedException("You do not have permission to access this plan.");
            }
        } else {
            // Should not happen for secured endpoints
            throw new AccessDeniedException("User not authenticated.");
        }
    }
} 