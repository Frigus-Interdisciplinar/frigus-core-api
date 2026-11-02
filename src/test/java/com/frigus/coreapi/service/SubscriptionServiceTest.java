package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.subscription.SubscriptionResponseDto;
import com.frigus.coreapi.enums.BillingInterval;
import com.frigus.coreapi.enums.SubscriptionStatus;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.SubscriptionMapper;
import com.frigus.coreapi.model.Plan;
import com.frigus.coreapi.model.Subscription;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.PlanRepository;
import com.frigus.coreapi.repository.SubscriptionRepository;
import com.frigus.coreapi.repository.UserRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionMapper subscriptionMapper;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private User user;
    private Plan plusPlan;
    private Plan freePlan;
    private Subscription activeSubscription;
    private SubscriptionResponseDto subscriptionResponseDto;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .name("Gabriel User")
                .email("gabriel@test.com")
                .build();

        plusPlan = Plan.builder()
                .id(2)
                .planCode("PLUS")
                .name("Frigus Plus")
                .price(new BigDecimal("29.99"))
                .billingInterval(BillingInterval.MONTHLY)
                .active(true)
                .build();

        freePlan = Plan.builder()
                .id(1)
                .planCode("FREE")
                .name("Frigus Free")
                .price(BigDecimal.ZERO)
                .active(true)
                .build();

        activeSubscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(plusPlan)
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .currentPeriodStart(Instant.now().minus(10, ChronoUnit.DAYS))
                .currentPeriodEnd(Instant.now().plus(20, ChronoUnit.DAYS))
                .build();

        subscriptionResponseDto = SubscriptionResponseDto.builder()
                .id(activeSubscription.getId())
                .userId(user.getId())
                .status(SubscriptionStatus.ACTIVE)
                .autoRenew(true)
                .build();
    }

    @Test
    @DisplayName("Deve retornar assinatura do usuário autenticado quando existente")
    void shouldGetMySubscriptionWhenExists() {
        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(user.getId()))
                .thenReturn(Optional.of(activeSubscription));
        when(subscriptionMapper.toDto(activeSubscription)).thenReturn(subscriptionResponseDto);

        SubscriptionResponseDto result = subscriptionService.getMySubscription(user);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(activeSubscription.getId());
        verify(subscriptionRepository).findByUserIdAndDeletedAtIsNull(user.getId());
    }

    @Test
    @DisplayName("Deve criar e retornar assinatura FREE padrão quando usuário não possui assinatura")
    void shouldCreateDefaultFreeSubscriptionWhenNoneExists() {
        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(user.getId()))
                .thenReturn(Optional.empty());
        when(planRepository.findByPlanCodeAndDeletedAtIsNull("FREE"))
                .thenReturn(Optional.of(freePlan));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriptionMapper.toDto(any(Subscription.class))).thenReturn(
                SubscriptionResponseDto.builder().status(SubscriptionStatus.ACTIVE).autoRenew(true).build()
        );

        SubscriptionResponseDto result = subscriptionService.getMySubscription(user);

        assertThat(result).isNotNull();
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    @DisplayName("Deve cancelar assinatura ativa com sucesso definindo canceledAt")
    void shouldCancelMySubscriptionSuccessfully() {
        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(user.getId()))
                .thenReturn(Optional.of(activeSubscription));
        when(subscriptionRepository.save(activeSubscription)).thenReturn(activeSubscription);
        when(subscriptionMapper.toDto(activeSubscription)).thenReturn(
                SubscriptionResponseDto.builder().status(SubscriptionStatus.ACTIVE).autoRenew(false).build()
        );

        SubscriptionResponseDto result = subscriptionService.cancelMySubscription(user);

        assertThat(activeSubscription.getCanceledAt()).isNotNull();
        assertThat(result.getAutoRenew()).isFalse();
        verify(subscriptionRepository).save(activeSubscription);
    }

    @Test
    @DisplayName("Deve lançar BadRequestException ao tentar cancelar assinatura já cancelada")
    void shouldThrowBadRequestWhenAlreadyCanceled() {
        activeSubscription.setCanceledAt(Instant.now());
        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(user.getId()))
                .thenReturn(Optional.of(activeSubscription));

        assertThatThrownBy(() -> subscriptionService.cancelMySubscription(user))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Deve lançar BadRequestException ao tentar cancelar plano gratuito")
    void shouldThrowBadRequestWhenCancelingFreePlan() {
        activeSubscription.setPlan(freePlan);
        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(user.getId()))
                .thenReturn(Optional.of(activeSubscription));

        assertThatThrownBy(() -> subscriptionService.cancelMySubscription(user))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Deve reativar assinatura cancelada com sucesso antes do fim do período")
    void shouldReactivateMySubscriptionSuccessfully() {
        activeSubscription.setCanceledAt(Instant.now());
        activeSubscription.setCurrentPeriodEnd(Instant.now().plus(10, ChronoUnit.DAYS));

        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(user.getId()))
                .thenReturn(Optional.of(activeSubscription));
        when(subscriptionRepository.save(activeSubscription)).thenReturn(activeSubscription);
        when(subscriptionMapper.toDto(activeSubscription)).thenReturn(
                SubscriptionResponseDto.builder().status(SubscriptionStatus.ACTIVE).autoRenew(true).build()
        );

        SubscriptionResponseDto result = subscriptionService.reactivateMySubscription(user);

        assertThat(activeSubscription.getCanceledAt()).isNull();
        assertThat(activeSubscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(subscriptionRepository).save(activeSubscription);
    }

    @Test
    @DisplayName("Deve lançar BadRequestException ao tentar reativar assinatura não cancelada")
    void shouldThrowBadRequestWhenReactivatingNotCanceled() {
        activeSubscription.setCanceledAt(null);
        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(user.getId()))
                .thenReturn(Optional.of(activeSubscription));

        assertThatThrownBy(() -> subscriptionService.reactivateMySubscription(user))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Deve lançar BadRequestException ao tentar reativar assinatura cujo período já expirou")
    void shouldThrowBadRequestWhenReactivatingExpiredPeriod() {
        activeSubscription.setCanceledAt(Instant.now().minus(5, ChronoUnit.DAYS));
        activeSubscription.setCurrentPeriodEnd(Instant.now().minus(1, ChronoUnit.DAYS));

        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(user.getId()))
                .thenReturn(Optional.of(activeSubscription));

        assertThatThrownBy(() -> subscriptionService.reactivateMySubscription(user))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Deve buscar assinatura por ID com sucesso")
    void shouldGetSubscriptionById() {
        UUID subId = activeSubscription.getId();
        when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(activeSubscription));
        when(subscriptionMapper.toDto(activeSubscription)).thenReturn(subscriptionResponseDto);

        SubscriptionResponseDto result = subscriptionService.getSubscriptionById(subId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(subId);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao buscar assinatura por ID inexistente")
    void shouldThrowNotFoundWhenSubscriptionIdNotFound() {
        UUID randomId = UUID.randomUUID();
        when(subscriptionRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.getSubscriptionById(randomId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Deve listar assinaturas com paginação e filtros para admin")
    void shouldListSubscriptionsWithPagination() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Subscription> page = new PageImpl<>(List.of(activeSubscription), pageable, 1);

        when(subscriptionRepository.findByStatusAndPlanIdAndDeletedAtIsNull(SubscriptionStatus.ACTIVE, 2, pageable))
                .thenReturn(page);
        when(subscriptionMapper.toDto(activeSubscription)).thenReturn(subscriptionResponseDto);

        Page<SubscriptionResponseDto> result = subscriptionService.listSubscriptions(pageable, SubscriptionStatus.ACTIVE, 2);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Deve atualizar status da assinatura administrativamente")
    void shouldUpdateSubscriptionStatusByAdmin() {
        UUID subId = activeSubscription.getId();
        when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(activeSubscription));
        when(subscriptionRepository.save(activeSubscription)).thenReturn(activeSubscription);
        when(subscriptionMapper.toDto(activeSubscription)).thenReturn(
                SubscriptionResponseDto.builder().status(SubscriptionStatus.DELINQUENT).build()
        );

        SubscriptionResponseDto result = subscriptionService.updateSubscriptionStatus(subId, SubscriptionStatus.DELINQUENT);

        assertThat(activeSubscription.getStatus()).isEqualTo(SubscriptionStatus.DELINQUENT);
        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.DELINQUENT);
        verify(subscriptionRepository).save(activeSubscription);
    }

    @Test
    @DisplayName("Deve cancelar assinatura administrativamente com status CANCELED")
    void shouldCancelSubscriptionByAdmin() {
        UUID subId = activeSubscription.getId();
        when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(activeSubscription));
        when(subscriptionRepository.save(activeSubscription)).thenReturn(activeSubscription);
        when(subscriptionMapper.toDto(activeSubscription)).thenReturn(
                SubscriptionResponseDto.builder().status(SubscriptionStatus.CANCELED).build()
        );

        SubscriptionResponseDto result = subscriptionService.cancelSubscriptionByAdmin(subId);

        assertThat(activeSubscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(activeSubscription.getCanceledAt()).isNotNull();
        verify(subscriptionRepository).save(activeSubscription);
    }
}
