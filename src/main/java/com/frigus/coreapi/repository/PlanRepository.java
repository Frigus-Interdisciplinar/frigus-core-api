package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.Plan;

import java.util.Optional;

public interface PlanRepository extends BaseRepository<Plan, Integer> {
    Optional<Plan> findByPlanCode(String planCode);
}
