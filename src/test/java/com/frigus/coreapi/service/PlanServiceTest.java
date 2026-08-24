package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.plan.PlanCreateRequestDto;
import com.frigus.coreapi.dto.plan.PlanLimitsDto;
import com.frigus.coreapi.dto.plan.PlanResponseDto;
import com.frigus.coreapi.dto.plan.PlanUpdateRequestDto;
import com.frigus.coreapi.enums.BillingInterval;
import com.frigus.coreapi.enums.PlanCode;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.ConflictException;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.PlanMapper;
import com.frigus.coreapi.model.Plan;
import com.frigus.coreapi.repository.PlanRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private PlanMapper planMapper;

    @Mock
    private PlanLimitsResolverService planLimitsResolverService;

    @InjectMocks
    private PlanService planService;

    private Plan samplePlan;
    private PlanResponseDto samplePlanDto;

    @BeforeEach
    void setUp() {
        samplePlan = Plan.builder()
                .id(1)
                .planCode("PLUS")
                .name("Frigus Plus")
                .description("Plano Plus")
                .price(new BigDecimal("29.99"))
                .billingInterval(BillingInterval.MONTHLY)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        samplePlanDto = PlanResponseDto.builder()
                .id(1)
                .planCode("PLUS")
                .name("Frigus Plus")
                .description("Plano Plus")
                .price(new BigDecimal("29.99"))
                .billingInterval(BillingInterval.MONTHLY)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Deve listar todos os planos ativos")
    void shouldListActivePlans() {
        when(planRepository.findByActiveTrueAndDeletedAtIsNull()).thenReturn(List.of(samplePlan));
        when(planMapper.toDto(samplePlan)).thenReturn(samplePlanDto);

        List<PlanResponseDto> result = planService.listActivePlans();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPlanCode()).isEqualTo("PLUS");
        verify(planRepository).findByActiveTrueAndDeletedAtIsNull();
    }

    @Test
    @DisplayName("Deve listar planos ativos de forma paginada")
    void shouldListActivePlansPaged() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Plan> page = new PageImpl<>(List.of(samplePlan), pageable, 1);

        when(planRepository.findByActiveTrueAndDeletedAtIsNull(pageable)).thenReturn(page);
        when(planMapper.toDto(samplePlan)).thenReturn(samplePlanDto);

        Page<PlanResponseDto> result = planService.listActivePlans(pageable);

        assertThat(result).hasSize(1);
        assertThat(result.getContent().get(0).getPlanCode()).isEqualTo("PLUS");
    }

    @Test
    @DisplayName("Deve listar todos os planos para admin incluindo inativos")
    void shouldListAllPlansAdmin() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Plan> page = new PageImpl<>(List.of(samplePlan), pageable, 1);

        when(planRepository.findByDeletedAtIsNull(pageable)).thenReturn(page);
        when(planMapper.toDto(samplePlan)).thenReturn(samplePlanDto);

        Page<PlanResponseDto> result = planService.listAllPlansAdmin(pageable, false);

        assertThat(result).hasSize(1);
        verify(planRepository).findByDeletedAtIsNull(pageable);
    }

    @Test
    @DisplayName("Deve buscar plano por ID com sucesso")
    void shouldGetPlanById() {
        when(planRepository.findById(1)).thenReturn(Optional.of(samplePlan));
        when(planMapper.toDto(samplePlan)).thenReturn(samplePlanDto);

        PlanResponseDto result = planService.getPlanById(1);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao buscar plano por ID inexistente")
    void shouldThrowNotFoundWhenPlanIdNotFound() {
        when(planRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.getPlanById(999))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Deve buscar plano por código com sucesso")
    void shouldGetPlanByCode() {
        when(planRepository.findByPlanCodeAndDeletedAtIsNull("PLUS")).thenReturn(Optional.of(samplePlan));
        when(planMapper.toDto(samplePlan)).thenReturn(samplePlanDto);

        PlanResponseDto result = planService.getPlanByCode("PLUS");

        assertThat(result).isNotNull();
        assertThat(result.getPlanCode()).isEqualTo("PLUS");
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao buscar plano por código inexistente")
    void shouldThrowNotFoundWhenPlanCodeNotFound() {
        when(planRepository.findByPlanCodeAndDeletedAtIsNull("NONEXISTENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.getPlanByCode("NONEXISTENT"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Deve resolver limites de um plano válido")
    void shouldGetPlanLimits() {
        PlanLimitsDto limitsDto = PlanLimitsDto.builder().planCode(PlanCode.PLUS).maxStocks(3).build();
        when(planLimitsResolverService.getLimitsForPlan(PlanCode.PLUS)).thenReturn(limitsDto);

        PlanLimitsDto result = planService.getPlanLimits("PLUS");

        assertThat(result).isNotNull();
        assertThat(result.getMaxStocks()).isEqualTo(3);
    }

    @Test
    @DisplayName("Deve lançar BadRequestException para código de plano inválido ao buscar limites")
    void shouldThrowBadRequestForInvalidPlanCodeLimits() {
        assertThatThrownBy(() -> planService.getPlanLimits("INVALID_CODE"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Deve resolver limites para usuário autenticado")
    void shouldGetMyPlanLimits() {
        PlanLimitsDto limitsDto = PlanLimitsDto.builder().planCode(PlanCode.FREE).maxStocks(1).build();
        when(planLimitsResolverService.resolveLimitsForCurrentUser()).thenReturn(limitsDto);

        PlanLimitsDto result = planService.getMyPlanLimits();

        assertThat(result).isNotNull();
        assertThat(result.getPlanCode()).isEqualTo(PlanCode.FREE);
    }

    @Test
    @DisplayName("Deve criar novo plano com sucesso quando planCode for único")
    void shouldCreatePlanSuccessfully() {
        PlanCreateRequestDto createDto = PlanCreateRequestDto.builder()
                .planCode("CUSTOM")
                .name("Plano Customizado")
                .description("Descrição do plano")
                .price(new BigDecimal("79.90"))
                .billingInterval(BillingInterval.MONTHLY)
                .active(true)
                .build();

        Plan entityToSave = Plan.builder()
                .planCode("CUSTOM")
                .name("Plano Customizado")
                .price(new BigDecimal("79.90"))
                .billingInterval(BillingInterval.MONTHLY)
                .active(true)
                .build();

        when(planRepository.existsByPlanCodeAndDeletedAtIsNull("CUSTOM")).thenReturn(false);
        when(planMapper.toEntity(createDto)).thenReturn(entityToSave);
        when(planRepository.save(any(Plan.class))).thenReturn(entityToSave);
        when(planMapper.toDto(entityToSave)).thenReturn(PlanResponseDto.builder().planCode("CUSTOM").build());

        PlanResponseDto result = planService.createPlan(createDto);

        assertThat(result).isNotNull();
        assertThat(result.getPlanCode()).isEqualTo("CUSTOM");
        verify(planRepository).save(any(Plan.class));
    }

    @Test
    @DisplayName("Deve lançar ConflictException ao tentar criar plano com código já existente")
    void shouldThrowConflictWhenPlanCodeExists() {
        PlanCreateRequestDto createDto = PlanCreateRequestDto.builder()
                .planCode("PLUS")
                .name("Outro Plus")
                .price(new BigDecimal("29.99"))
                .build();

        when(planRepository.existsByPlanCodeAndDeletedAtIsNull("PLUS")).thenReturn(true);

        assertThatThrownBy(() -> planService.createPlan(createDto))
                .isInstanceOf(ConflictException.class);

        verify(planRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve atualizar plano existente com sucesso")
    void shouldUpdatePlanSuccessfully() {
        PlanUpdateRequestDto updateDto = PlanUpdateRequestDto.builder()
                .name("Novo Nome Plus")
                .price(new BigDecimal("34.99"))
                .active(false)
                .build();

        when(planRepository.findById(1)).thenReturn(Optional.of(samplePlan));
        when(planRepository.save(samplePlan)).thenReturn(samplePlan);
        when(planMapper.toDto(samplePlan)).thenReturn(PlanResponseDto.builder()
                .name("Novo Nome Plus")
                .price(new BigDecimal("34.99"))
                .active(false)
                .build());

        PlanResponseDto result = planService.updatePlan(1, updateDto);

        assertThat(result.getName()).isEqualTo("Novo Nome Plus");
        assertThat(result.getPrice()).isEqualTo(new BigDecimal("34.99"));
        assertThat(result.getActive()).isFalse();
    }

    @Test
    @DisplayName("Deve realizar soft delete do plano")
    void shouldSoftDeletePlan() {
        when(planRepository.findById(1)).thenReturn(Optional.of(samplePlan));
        when(planRepository.save(samplePlan)).thenReturn(samplePlan);

        planService.deletePlan(1);

        assertThat(samplePlan.getActive()).isFalse();
        assertThat(samplePlan.getDeletedAt()).isNotNull();
        verify(planRepository).save(samplePlan);
    }
}
