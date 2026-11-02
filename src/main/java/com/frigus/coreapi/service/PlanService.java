package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.plan.PlanCreateRequestDto;
import com.frigus.coreapi.dto.plan.PlanLimitsDto;
import com.frigus.coreapi.dto.plan.PlanResponseDto;
import com.frigus.coreapi.dto.plan.PlanUpdateRequestDto;
import com.frigus.coreapi.enums.PlanCode;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.ConflictException;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.PlanMapper;
import com.frigus.coreapi.model.Plan;
import com.frigus.coreapi.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final PlanMapper planMapper;
    private final PlanLimitsResolverService planLimitsResolverService;

    public List<PlanResponseDto> listActivePlans() {
        return planRepository.findByActiveTrueAndDeletedAtIsNull().stream()
                .map(planMapper::toDto)
                .toList();
    }

    public Page<PlanResponseDto> listActivePlans(Pageable pageable) {
        return planRepository.findByActiveTrueAndDeletedAtIsNull(pageable)
                .map(planMapper::toDto);
    }

    public Page<PlanResponseDto> listAllPlansAdmin(Pageable pageable, Boolean includeDeleted) {
        if (Boolean.TRUE.equals(includeDeleted)) {
            return planRepository.findAll(pageable).map(planMapper::toDto);
        }
        return planRepository.findByDeletedAtIsNull(pageable).map(planMapper::toDto);
    }

    public PlanResponseDto getPlanById(Integer id) {
        Plan plan = planRepository.findById(id)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("Plano não encontrado", "Plano com o ID " + id + " não existe"));
        return planMapper.toDto(plan);
    }

    public PlanResponseDto getPlanByCode(String planCode) {
        Plan plan = planRepository.findByPlanCodeAndDeletedAtIsNull(planCode.toUpperCase())
                .orElseThrow(() -> new NotFoundException("Plano não encontrado", "Plano com o código " + planCode + " não existe"));
        return planMapper.toDto(plan);
    }

    public PlanLimitsDto getPlanLimits(String planCode) {
        try {
            PlanCode code = PlanCode.valueOf(planCode.toUpperCase());
            return planLimitsResolverService.getLimitsForPlan(code);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Código de plano inválido", "O código " + planCode + " não corresponde a um plano válido");
        }
    }

    public PlanLimitsDto getMyPlanLimits() {
        return planLimitsResolverService.resolveLimitsForCurrentUser();
    }

    @Transactional
    public PlanResponseDto createPlan(PlanCreateRequestDto dto) {
        String planCode = dto.getPlanCode().trim().toUpperCase();
        if (planRepository.existsByPlanCodeAndDeletedAtIsNull(planCode)) {
            throw new ConflictException("Código de plano em uso", "Já existe um plano ativo cadastrado com o código " + planCode);
        }

        Plan plan = planMapper.toEntity(dto);
        plan.setCreatedAt(Instant.now());
        plan.setUpdatedAt(Instant.now());
        plan = planRepository.save(plan);

        return planMapper.toDto(plan);
    }

    @Transactional
    public PlanResponseDto updatePlan(Integer id, PlanUpdateRequestDto dto) {
        Plan plan = planRepository.findById(id)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("Plano não encontrado", "Plano com o ID " + id + " não existe"));

        if (dto.getName() != null && !dto.getName().isBlank()) {
            plan.setName(dto.getName().trim());
        }
        if (dto.getDescription() != null) {
            plan.setDescription(dto.getDescription());
        }
        if (dto.getPrice() != null) {
            plan.setPrice(dto.getPrice());
        }
        if (dto.getBillingInterval() != null) {
            plan.setBillingInterval(dto.getBillingInterval());
        }
        if (dto.getActive() != null) {
            plan.setActive(dto.getActive());
        }

        plan.setUpdatedAt(Instant.now());
        plan = planRepository.save(plan);

        return planMapper.toDto(plan);
    }

    @Transactional
    public void deletePlan(Integer id) {
        Plan plan = planRepository.findById(id)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("Plano não encontrado", "Plano com o ID " + id + " não existe"));

        plan.setDeletedAt(Instant.now());
        plan.setActive(false);
        plan.setUpdatedAt(Instant.now());
        planRepository.save(plan);
    }
}
