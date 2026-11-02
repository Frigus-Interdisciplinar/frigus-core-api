package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.plan.PlanCreateRequestDto;
import com.frigus.coreapi.dto.plan.PlanLimitsDto;
import com.frigus.coreapi.dto.plan.PlanResponseDto;
import com.frigus.coreapi.dto.plan.PlanUpdateRequestDto;
import com.frigus.coreapi.enums.BillingInterval;
import com.frigus.coreapi.enums.PlanCode;
import com.frigus.coreapi.service.PlanService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanControllerTest {

    @Mock
    private PlanService planService;

    @InjectMocks
    private PlanController planController;

    @Test
    @DisplayName("Deve listar planos ativos de forma paginada")
    void shouldListPlansPaged() {
        Pageable pageable = PageRequest.of(0, 10);
        PlanResponseDto dto = PlanResponseDto.builder().id(1).planCode("PLUS").build();
        Page<PlanResponseDto> page = new PageImpl<>(List.of(dto), pageable, 1);

        when(planService.listActivePlans(pageable)).thenReturn(page);

        Page<PlanResponseDto> response = planController.listPlans(pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        verify(planService).listActivePlans(pageable);
    }

    @Test
    @DisplayName("Deve listar todos os planos ativos em lista")
    void shouldListActivePlans() {
        PlanResponseDto dto = PlanResponseDto.builder().id(1).planCode("PLUS").build();
        when(planService.listActivePlans()).thenReturn(List.of(dto));

        List<PlanResponseDto> response = planController.listActivePlans();

        assertThat(response).hasSize(1);
        verify(planService).listActivePlans();
    }

    @Test
    @DisplayName("Deve buscar plano por ID")
    void shouldGetPlanById() {
        PlanResponseDto dto = PlanResponseDto.builder().id(1).planCode("PLUS").build();
        when(planService.getPlanById(1)).thenReturn(dto);

        PlanResponseDto response = planController.getPlanById(1);

        assertThat(response.getId()).isEqualTo(1);
        verify(planService).getPlanById(1);
    }

    @Test
    @DisplayName("Deve buscar plano por código")
    void shouldGetPlanByCode() {
        PlanResponseDto dto = PlanResponseDto.builder().id(1).planCode("FAMILY").build();
        when(planService.getPlanByCode("FAMILY")).thenReturn(dto);

        PlanResponseDto response = planController.getPlanByCode("FAMILY");

        assertThat(response.getPlanCode()).isEqualTo("FAMILY");
        verify(planService).getPlanByCode("FAMILY");
    }

    @Test
    @DisplayName("Deve buscar limites de um plano por código")
    void shouldGetPlanLimits() {
        PlanLimitsDto limits = PlanLimitsDto.builder().planCode(PlanCode.PLUS).maxStocks(3).build();
        when(planService.getPlanLimits("PLUS")).thenReturn(limits);

        PlanLimitsDto response = planController.getPlanLimits("PLUS");

        assertThat(response.getMaxStocks()).isEqualTo(3);
        verify(planService).getPlanLimits("PLUS");
    }

    @Test
    @DisplayName("Deve buscar limites do plano do usuário autenticado")
    void shouldGetMyPlanLimits() {
        PlanLimitsDto limits = PlanLimitsDto.builder().planCode(PlanCode.FREE).maxStocks(1).build();
        when(planService.getMyPlanLimits()).thenReturn(limits);

        PlanLimitsDto response = planController.getMyPlanLimits();

        assertThat(response.getPlanCode()).isEqualTo(PlanCode.FREE);
        verify(planService).getMyPlanLimits();
    }

    @Test
    @DisplayName("Deve criar novo plano via controller")
    void shouldCreatePlan() {
        PlanCreateRequestDto createDto = PlanCreateRequestDto.builder()
                .planCode("ENTERPRISE")
                .name("Frigus Enterprise")
                .price(new BigDecimal("159.99"))
                .billingInterval(BillingInterval.MONTHLY)
                .build();

        PlanResponseDto expectedResponse = PlanResponseDto.builder()
                .id(5)
                .planCode("ENTERPRISE")
                .build();

        when(planService.createPlan(createDto)).thenReturn(expectedResponse);

        PlanResponseDto response = planController.createPlan(createDto);

        assertThat(response.getId()).isEqualTo(5);
        verify(planService).createPlan(createDto);
    }

    @Test
    @DisplayName("Deve atualizar plano via controller")
    void shouldUpdatePlan() {
        PlanUpdateRequestDto updateDto = PlanUpdateRequestDto.builder()
                .name("Updated Name")
                .build();

        PlanResponseDto expectedResponse = PlanResponseDto.builder()
                .id(1)
                .name("Updated Name")
                .build();

        when(planService.updatePlan(1, updateDto)).thenReturn(expectedResponse);

        PlanResponseDto response = planController.updatePlan(1, updateDto);

        assertThat(response.getName()).isEqualTo("Updated Name");
        verify(planService).updatePlan(1, updateDto);
    }

    @Test
    @DisplayName("Deve deletar plano via controller")
    void shouldDeletePlan() {
        planController.deletePlan(1);
        verify(planService).deletePlan(1);
    }

    @Test
    @DisplayName("Deve listar todos os planos no endpoint administrativo")
    void shouldListAllPlansAdmin() {
        Pageable pageable = PageRequest.of(0, 10);
        PlanResponseDto dto = PlanResponseDto.builder().id(1).planCode("PLUS").build();
        Page<PlanResponseDto> page = new PageImpl<>(List.of(dto), pageable, 1);

        when(planService.listAllPlansAdmin(pageable, true)).thenReturn(page);

        Page<PlanResponseDto> response = planController.listAllPlansAdmin(pageable, true);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        verify(planService).listAllPlansAdmin(pageable, true);
    }
}
