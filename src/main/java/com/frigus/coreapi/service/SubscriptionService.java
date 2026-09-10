package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.subscription.SubscriptionResponseDto;
import com.frigus.coreapi.enums.PlanCode;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final SubscriptionMapper subscriptionMapper;

    @Transactional
    public SubscriptionResponseDto getMySubscription(User currentUser) {
        Subscription subscription = subscriptionRepository.findByUserIdAndDeletedAtIsNull(currentUser.getId())
                .orElse(null);

        if (subscription == null) {
            subscription = getOrCreateDefaultFreeSubscription(currentUser);
        } else {
            // se a assinatura foi cancelada e o período pago ja encerrou, marca como expirada e rebaixa para FREE
            if (subscription.getCanceledAt() != null 
                    && subscription.getCurrentPeriodEnd() != null 
                    && subscription.getCurrentPeriodEnd().isBefore(Instant.now())
                    && subscription.getStatus() == SubscriptionStatus.ACTIVE) {
                subscription.setStatus(SubscriptionStatus.EXPIRED);
                subscriptionRepository.save(subscription);
            }
        }

        return subscriptionMapper.toDto(subscription);
    }

    @Transactional
    public SubscriptionResponseDto cancelMySubscription(User currentUser) {
        Subscription subscription = subscriptionRepository.findByUserIdAndDeletedAtIsNull(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Assinatura não encontrada", "Nenhuma assinatura ativa encontrada para este usuário"));

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BadRequestException("Assinatura inativa", "Apenas assinaturas com status ACTIVE podem ser canceladas.");
        }

        if (subscription.getCanceledAt() != null) {
            throw new BadRequestException("Já cancelada", "A renovação automática da assinatura já se encontra cancelada.");
        }

        boolean isFreePlan = subscription.getPlan() == null 
                || subscription.getPlan().getPrice() == null 
                || subscription.getPlan().getPrice().compareTo(BigDecimal.ZERO) == 0;

        if (isFreePlan) {
            throw new BadRequestException("Plano gratuito", "O plano gratuito não possui cobrança recorrente para cancelamento.");
        }

        subscription.setCanceledAt(Instant.now());
        subscription.setUpdatedAt(Instant.now());
        subscription = subscriptionRepository.save(subscription);

        log.info("Assinatura id={} cancelada pelo usuário id={}. Acesso válido até {}", 
                subscription.getId(), currentUser.getId(), subscription.getCurrentPeriodEnd());

        return subscriptionMapper.toDto(subscription);
    }

    @Transactional
    public SubscriptionResponseDto reactivateMySubscription(User currentUser) {
        Subscription subscription = subscriptionRepository.findByUserIdAndDeletedAtIsNull(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Assinatura não encontrada", "Nenhuma assinatura encontrada para este usuário"));

        if (subscription.getCanceledAt() == null) {
            throw new BadRequestException("Assinatura não cancelada", "A assinatura já está com a renovação automática ativa.");
        }

        if (subscription.getCurrentPeriodEnd() != null && subscription.getCurrentPeriodEnd().isBefore(Instant.now())) {
            throw new BadRequestException("Período expirado", "O período vigente da assinatura já expirou. Realize um novo checkout para reativar o plano.");
        }

        subscription.setCanceledAt(null);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setUpdatedAt(Instant.now());
        subscription = subscriptionRepository.save(subscription);

        log.info("Assinatura id={} reativada pelo usuário id={}", subscription.getId(), currentUser.getId());

        return subscriptionMapper.toDto(subscription);
    }

    public SubscriptionResponseDto getSubscriptionById(UUID id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .filter(s -> s.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("Assinatura não encontrada", "Assinatura com o ID " + id + " não existe"));
        return subscriptionMapper.toDto(subscription);
    }

    public SubscriptionResponseDto getSubscriptionByUserId(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado", "Usuário com o ID " + userId + " não existe"));

        Subscription subscription = subscriptionRepository.findByUserIdAndDeletedAtIsNull(user.getId())
                .orElseGet(() -> getOrCreateDefaultFreeSubscription(user));

        return subscriptionMapper.toDto(subscription);
    }

    public Page<SubscriptionResponseDto> listSubscriptions(Pageable pageable, SubscriptionStatus status, Integer planId) {
        if (status != null && planId != null) {
            return subscriptionRepository.findByStatusAndPlanIdAndDeletedAtIsNull(status, planId, pageable)
                    .map(subscriptionMapper::toDto);
        } else if (status != null) {
            return subscriptionRepository.findByStatusAndDeletedAtIsNull(status, pageable)
                    .map(subscriptionMapper::toDto);
        } else if (planId != null) {
            return subscriptionRepository.findByPlanIdAndDeletedAtIsNull(planId, pageable)
                    .map(subscriptionMapper::toDto);
        }

        return subscriptionRepository.findByDeletedAtIsNull(pageable)
                .map(subscriptionMapper::toDto);
    }

    @Transactional
    public SubscriptionResponseDto updateSubscriptionStatus(UUID id, SubscriptionStatus status) {
        Subscription subscription = subscriptionRepository.findById(id)
                .filter(s -> s.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("Assinatura não encontrada", "Assinatura com o ID " + id + " não existe"));

        subscription.setStatus(status);
        subscription.setUpdatedAt(Instant.now());
        subscription = subscriptionRepository.save(subscription);

        log.info("Status da assinatura id={} alterado manualmente para {}", id, status);
        return subscriptionMapper.toDto(subscription);
    }

    @Transactional
    public SubscriptionResponseDto cancelSubscriptionByAdmin(UUID id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .filter(s -> s.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("Assinatura não encontrada", "Assinatura com o ID " + id + " não existe"));

        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscription.setCanceledAt(Instant.now());
        subscription.setUpdatedAt(Instant.now());
        subscription = subscriptionRepository.save(subscription);

        log.info("Assinatura id={} cancelada administrativamente", id);
        return subscriptionMapper.toDto(subscription);
    }

    private Subscription getOrCreateDefaultFreeSubscription(User user) {
        Plan freePlan = planRepository.findByPlanCodeAndDeletedAtIsNull("FREE")
                .orElseGet(() -> planRepository.findAll().stream()
                        .filter(p -> p.getPrice() == null || BigDecimal.ZERO.compareTo(p.getPrice()) == 0)
                        .findFirst()
                        .orElseGet(() -> planRepository.save(Plan.builder()
                                .planCode(PlanCode.FREE.name())
                                .name("Frigus Free")
                                .description("Plano gratuito básico")
                                .price(BigDecimal.ZERO)
                                .active(true)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build())));

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(freePlan)
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(user.getCreatedAt() != null ? user.getCreatedAt() : Instant.now())
                .currentPeriodStart(Instant.now())
                .currentPeriodEnd(null)
                .updatedAt(Instant.now())
                .build();

        return subscriptionRepository.save(subscription);
    }
}
