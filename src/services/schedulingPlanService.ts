import { api } from '../api';
import type { SchedulingPlanDto } from '../types';

export const schedulingPlanService = {
  // Get all plans for admin
  getAllPlans(): Promise<SchedulingPlanDto[]> {
    return api.get('/scheduling-plans');
  },

  // Get plans for the current logged-in user
  getMyPlans(): Promise<SchedulingPlanDto[]> {
    return api.get('/scheduling-plans/my-plans');
  },

  // Get a single plan by its ID
  getPlanById(id: number): Promise<SchedulingPlanDto> {
    return api.get(`/scheduling-plans/${id}`);
  },

  // Create a new scheduling plan
  createPlan(planData: Partial<SchedulingPlanDto>): Promise<SchedulingPlanDto> {
    return api.post('/scheduling-plans', planData);
  },

  // Update an existing plan
  updatePlan(id: number, planData: Partial<SchedulingPlanDto>): Promise<SchedulingPlanDto> {
    return api.put(`/scheduling-plans/${id}`, planData);
  },

  // Delete a plan by its ID
  deletePlan(id: number): Promise<void> {
    return api.delete(`/scheduling-plans/${id}`);
  },
}; 