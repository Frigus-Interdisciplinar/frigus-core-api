package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.Plan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PlanRepository extends BaseRepository<Plan, Integer> {
    Optional<Plan> findByPlanCode(String planCode);
    Optional<Plan> findByPlanCodeAndDeletedAtIsNull(String planCode);
    Optional<Plan> findByPlanCodeAndActiveTrueAndDeletedAtIsNull(String planCode);
    List<Plan> findByActiveTrueAndDeletedAtIsNull();
    Page<Plan> findByActiveTrueAndDeletedAtIsNull(Pageable pageable);
    Page<Plan> findByDeletedAtIsNull(Pageable pageable);
    boolean existsByPlanCodeAndDeletedAtIsNull(String planCode);
}

