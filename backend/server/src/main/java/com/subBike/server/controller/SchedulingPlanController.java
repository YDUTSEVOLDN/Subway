package com.subBike.server.controller;

import com.subBike.server.entity.SchedulingPlan;
import com.subBike.server.entity.User;
import com.subBike.server.entity.dto.SchedulingPlanDto;
import com.subBike.server.mapper.UserRepository;
import com.subBike.server.security.services.UserDetailsImpl;
import com.subBike.server.service.ISchedulingPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/scheduling-plans")
public class SchedulingPlanController {

    @Autowired
    private ISchedulingPlanService schedulingPlanService;

    @Autowired
    private UserRepository userRepository;

    // Admin access to all plans
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<SchedulingPlanDto> getAllPlans() {
        return schedulingPlanService.getAllPlans().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // User access to their own plans
    @GetMapping("/my-plans")
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public List<SchedulingPlanDto> getCurrentUserPlans() {
        return schedulingPlanService.getPlansForCurrentUser().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<SchedulingPlanDto> createPlan(@RequestBody SchedulingPlanDto planDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with name: " + currentUsername));

        SchedulingPlan plan = convertToEntity(planDto);
        SchedulingPlan savedPlan = schedulingPlanService.savePlan(plan, currentUser.getId());
        return ResponseEntity.ok(convertToDto(savedPlan));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<SchedulingPlanDto> getPlanById(@PathVariable Long id) {
        // Add security check to ensure user can only access their own plan unless admin
        return schedulingPlanService.getPlanById(id)
                .map(plan -> ResponseEntity.ok(convertToDto(plan)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<SchedulingPlanDto> updatePlan(@PathVariable Long id, @RequestBody SchedulingPlanDto planDto) {
        // Add security check
        SchedulingPlan plan = convertToEntity(planDto);
        SchedulingPlan updatedPlan = schedulingPlanService.updatePlan(id, plan);
        return ResponseEntity.ok(convertToDto(updatedPlan));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deletePlan(@PathVariable Long id) {
        // Add security check
        schedulingPlanService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }

    // --- Helper Methods for DTO conversion ---
    private SchedulingPlanDto convertToDto(SchedulingPlan plan) {
        SchedulingPlanDto dto = new SchedulingPlanDto();
        dto.setId(plan.getId());
        dto.setName(plan.getName());
        dto.setDescription(plan.getDescription());
        dto.setSourceStationName(plan.getSourceStationName());
        dto.setTargetStationName(plan.getTargetStationName());
        dto.setBikeCount(plan.getBikeCount());
        dto.setScheduleTime(plan.getScheduleTime());
        dto.setPriority(plan.getPriority());
        dto.setPathData(plan.getPathData());
        dto.setStatus(plan.getStatus());
        if (plan.getCreatedBy() != null) {
            dto.setCreatedByUsername(plan.getCreatedBy().getUsername());
        }
        dto.setCreatedAt(plan.getCreatedAt());
        dto.setUpdatedAt(plan.getUpdatedAt());
        return dto;
    }

    private SchedulingPlan convertToEntity(SchedulingPlanDto dto) {
        SchedulingPlan plan = new SchedulingPlan();
        // Note: We don't map id, createdBy, createdAt, updatedAt from DTO
        plan.setName(dto.getName());
        plan.setDescription(dto.getDescription());
        plan.setSourceStationName(dto.getSourceStationName());
        plan.setTargetStationName(dto.getTargetStationName());
        plan.setBikeCount(dto.getBikeCount());
        plan.setScheduleTime(dto.getScheduleTime());
        plan.setPriority(dto.getPriority());
        plan.setPathData(dto.getPathData());
        plan.setStatus(dto.getStatus());
        return plan;
    }
} 