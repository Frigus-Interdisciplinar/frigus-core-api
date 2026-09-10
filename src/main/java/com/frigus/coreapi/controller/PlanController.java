package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.plan.PlanCreateRequestDto;
import com.frigus.coreapi.dto.plan.PlanLimitsDto;
import com.frigus.coreapi.dto.plan.PlanResponseDto;
import com.frigus.coreapi.dto.plan.PlanUpdateRequestDto;
import com.frigus.coreapi.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/plans")
public class PlanController {

    private final PlanService planService;

    @GetMapping
    public Page<PlanResponseDto> listPlans(Pageable pageable) {
        return planService.listActivePlans(pageable);
    }

    @GetMapping("/active")
    public List<PlanResponseDto> listActivePlans() {
        return planService.listActivePlans();
    }

    @GetMapping("/{id}")
    public PlanResponseDto getPlanById(@PathVariable Integer id) {
        return planService.getPlanById(id);
    }

    @GetMapping("/code/{planCode}")
    public PlanResponseDto getPlanByCode(@PathVariable String planCode) {
        return planService.getPlanByCode(planCode);
    }

    @GetMapping("/{planCode}/limits")
    public PlanLimitsDto getPlanLimits(@PathVariable String planCode) {
        return planService.getPlanLimits(planCode);
    }

    @GetMapping("/me/limits")
    public PlanLimitsDto getMyPlanLimits() {
        return planService.getMyPlanLimits();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public PlanResponseDto createPlan(@Valid @RequestBody PlanCreateRequestDto dto) {
        return planService.createPlan(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PlanResponseDto updatePlan(@PathVariable Integer id, @Valid @RequestBody PlanUpdateRequestDto dto) {
        return planService.updatePlan(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deletePlan(@PathVariable Integer id) {
        planService.deletePlan(id);
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<PlanResponseDto> listAllPlansAdmin(
            Pageable pageable,
            @RequestParam(required = false, defaultValue = "false") Boolean includeDeleted) {
        return planService.listAllPlansAdmin(pageable, includeDeleted);
    }
}
