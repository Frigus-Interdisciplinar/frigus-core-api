package com.frigus.coreapi.mapper;

import com.frigus.coreapi.dto.plan.PlanCreateRequestDto;
import com.frigus.coreapi.dto.plan.PlanLimitsDto;
import com.frigus.coreapi.dto.plan.PlanResponseDto;
import com.frigus.coreapi.enums.PlanCode;
import com.frigus.coreapi.model.Plan;
import com.frigus.coreapi.service.PlanLimitsResolverService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlanMapper implements BaseMapper<Plan, PlanResponseDto, PlanCreateRequestDto> {

    private final PlanLimitsResolverService planLimitsResolverService;

    @Override
    public PlanResponseDto toDto(Plan model) {
        if (model == null) {
            return null;
        }

        PlanLimitsDto limits = null;
        if (model.getPlanCode() != null) {
            try {
                PlanCode planCode = PlanCode.valueOf(model.getPlanCode().toUpperCase());
                limits = planLimitsResolverService.getLimitsForPlan(planCode);
            } catch (Exception ignored) {
                
            }
        }

        return PlanResponseDto.builder()
                .id(model.getId())
                .planCode(model.getPlanCode())
                .name(model.getName())
                .description(model.getDescription())
                .price(model.getPrice())
                .billingInterval(model.getBillingInterval())
                .active(model.getActive())
                .limits(limits)
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }

    @Override
    public Plan toEntity(PlanCreateRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return Plan.builder()
                .planCode(dto.getPlanCode().trim().toUpperCase())
                .name(dto.getName().trim())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .billingInterval(dto.getBillingInterval())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();
    }
}
