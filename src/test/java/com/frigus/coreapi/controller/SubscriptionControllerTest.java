package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.subscription.BillingJobResultDto;
import com.frigus.coreapi.dto.subscription.SubscriptionResponseDto;
import com.frigus.coreapi.dto.subscription.SubscriptionStatusUpdateDto;
import com.frigus.coreapi.enums.SubscriptionStatus;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.service.SubscriptionBillingJobService;
import com.frigus.coreapi.service.SubscriptionService;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private SubscriptionBillingJobService subscriptionBillingJobService;

    @InjectMocks
    private SubscriptionController subscriptionController;

    private User currentUser;
    private SubscriptionResponseDto sampleResponseDto;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(UUID.randomUUID())
                .name("Gabriel Controller Test")
                .email("gabriel@controller.com")
                .build();

        sampleResponseDto = SubscriptionResponseDto.builder()
                .id(UUID.randomUUID())
                .userId(currentUser.getId())
                .status(SubscriptionStatus.ACTIVE)
                .autoRenew(true)
                .build();
    }

    @Test
    @DisplayName("Deve retornar a assinatura do usuário atual no endpoint /me")
    void shouldGetMySubscription() {
        when(subscriptionService.getMySubscription(currentUser)).thenReturn(sampleResponseDto);

        SubscriptionResponseDto response = subscriptionController.getMySubscription(currentUser);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(sampleResponseDto.getId());
        verify(subscriptionService).getMySubscription(currentUser);
    }

    @Test
    @DisplayName("Deve cancelar a assinatura do usuário atual no endpoint /me/cancel")
    void shouldCancelMySubscription() {
        sampleResponseDto.setAutoRenew(false);
        when(subscriptionService.cancelMySubscription(currentUser)).thenReturn(sampleResponseDto);

        SubscriptionResponseDto response = subscriptionController.cancelMySubscription(currentUser);

        assertThat(response.getAutoRenew()).isFalse();
        verify(subscriptionService).cancelMySubscription(currentUser);
    }

    @Test
    @DisplayName("Deve reativar a assinatura do usuário atual no endpoint /me/reactivate")
    void shouldReactivateMySubscription() {
        sampleResponseDto.setAutoRenew(true);
        when(subscriptionService.reactivateMySubscription(currentUser)).thenReturn(sampleResponseDto);

        SubscriptionResponseDto response = subscriptionController.reactivateMySubscription(currentUser);

        assertThat(response.getAutoRenew()).isTrue();
        verify(subscriptionService).reactivateMySubscription(currentUser);
    }

    @Test
    @DisplayName("Deve listar assinaturas com filtros no endpoint administrativo")
    void shouldListSubscriptionsAdmin() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SubscriptionResponseDto> page = new PageImpl<>(List.of(sampleResponseDto), pageable, 1);

        when(subscriptionService.listSubscriptions(pageable, SubscriptionStatus.ACTIVE, 1)).thenReturn(page);

        Page<SubscriptionResponseDto> response = subscriptionController.listSubscriptions(pageable, SubscriptionStatus.ACTIVE, 1);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        verify(subscriptionService).listSubscriptions(pageable, SubscriptionStatus.ACTIVE, 1);
    }

    @Test
    @DisplayName("Deve buscar assinatura por ID no endpoint administrativo")
    void shouldGetSubscriptionByIdAdmin() {
        UUID subId = sampleResponseDto.getId();
        when(subscriptionService.getSubscriptionById(subId)).thenReturn(sampleResponseDto);

        SubscriptionResponseDto response = subscriptionController.getSubscriptionById(subId);

        assertThat(response.getId()).isEqualTo(subId);
        verify(subscriptionService).getSubscriptionById(subId);
    }

    @Test
    @DisplayName("Deve buscar assinatura por usuário no endpoint administrativo")
    void shouldGetSubscriptionByUserIdAdmin() {
        UUID userId = currentUser.getId();
        when(subscriptionService.getSubscriptionByUserId(userId)).thenReturn(sampleResponseDto);

        SubscriptionResponseDto response = subscriptionController.getSubscriptionByUserId(userId);

        assertThat(response.getUserId()).isEqualTo(userId);
        verify(subscriptionService).getSubscriptionByUserId(userId);
    }

    @Test
    @DisplayName("Deve atualizar status da assinatura no endpoint administrativo")
    void shouldUpdateSubscriptionStatusAdmin() {
        UUID subId = sampleResponseDto.getId();
        SubscriptionStatusUpdateDto dto = SubscriptionStatusUpdateDto.builder()
                .status(SubscriptionStatus.DELINQUENT)
                .build();

        sampleResponseDto.setStatus(SubscriptionStatus.DELINQUENT);
        when(subscriptionService.updateSubscriptionStatus(subId, SubscriptionStatus.DELINQUENT)).thenReturn(sampleResponseDto);

        SubscriptionResponseDto response = subscriptionController.updateSubscriptionStatus(subId, dto);

        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.DELINQUENT);
        verify(subscriptionService).updateSubscriptionStatus(subId, SubscriptionStatus.DELINQUENT);
    }

    @Test
    @DisplayName("Deve cancelar assinatura no endpoint administrativo")
    void shouldCancelSubscriptionByAdmin() {
        UUID subId = sampleResponseDto.getId();
        sampleResponseDto.setStatus(SubscriptionStatus.CANCELED);
        when(subscriptionService.cancelSubscriptionByAdmin(subId)).thenReturn(sampleResponseDto);

        SubscriptionResponseDto response = subscriptionController.cancelSubscriptionByAdmin(subId);

        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        verify(subscriptionService).cancelSubscriptionByAdmin(subId);
    }

    @Test
    @DisplayName("Deve disparar execução do job diário de cobrança no endpoint administrativo")
    void shouldRunBillingJobManuallyAdmin() {
        BillingJobResultDto resultDto = BillingJobResultDto.builder()
                .evaluatedCount(5)
                .transactionsGeneratedCount(3)
                .build();

        when(subscriptionBillingJobService.runDailyBillingJob()).thenReturn(resultDto);

        BillingJobResultDto response = subscriptionController.runBillingJobManually();

        assertThat(response.getEvaluatedCount()).isEqualTo(5);
        assertThat(response.getTransactionsGeneratedCount()).isEqualTo(3);
        verify(subscriptionBillingJobService).runDailyBillingJob();
    }
}
