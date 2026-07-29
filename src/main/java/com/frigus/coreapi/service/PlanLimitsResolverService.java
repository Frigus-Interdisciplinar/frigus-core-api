package com.frigus.coreapi.service;

import com.frigus.coreapi.enums.PlanCode;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.model.Plan;
import com.frigus.coreapi.model.Subscription;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.UserRepository;
import com.frigus.coreapi.utils.ServiceUtils;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.frigus.coreapi.dto.plan.PlanLimitsDto;

@Service
@RequiredArgsConstructor
public class PlanLimitsResolverService {
    
    private final UserRepository userRepository;

    public PlanLimitsDto resolveLimitsForCurrentUser() {
        User user = ServiceUtils.getCurrentUser();
        if (user == null) {
            return getLimitsForPlan(PlanCode.FREE);
        }
        
        Subscription subscription = user.getSubscription();
        if (subscription == null || subscription.getPlan() == null) {
            return getLimitsForPlan(PlanCode.FREE);
        }
        
        Plan plan = subscription.getPlan();
        PlanCode planCode = PlanCode.valueOf(plan.getPlanCode()); 
        return getLimitsForPlan(planCode);
    }

    public PlanLimitsDto getLimitsForUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Usuário não encontrado", "Usuário"));
        
        Plan plan = user.getSubscription().getPlan();
        PlanCode planCode = PlanCode.valueOf(plan.getPlanCode()); 

        return getLimitsForPlan(planCode);
    }

    public PlanLimitsDto getLimitsForPlan(PlanCode planCode) {
        // default for frigus free plan
        int maxGroupMembers = 3;
        int maxStocks = 1;
        int maxProductsPerStock = 20;
        boolean allowOwnProducts = false;
        boolean allowSavedRecipes = false;
        boolean allowMoneySaving = false;
        boolean isEnterprise = false;
        BigDecimal costPerPublishedAd = BigDecimal.ZERO;
        boolean hasMonthlyReport = false;

        switch (planCode) {
            case FREE:
                break;
            case PLUS:
                maxGroupMembers = 5;
                maxStocks = 3;
                maxProductsPerStock = 50;
                allowOwnProducts = true;
                allowSavedRecipes = true;
                allowMoneySaving = true;
                break;
            case FAMILY:
                maxGroupMembers = 10;
                maxStocks = 5;
                maxProductsPerStock = 100;
                allowOwnProducts = true;
                allowSavedRecipes = true;
                allowMoneySaving = true;
                break;
            case COMMERCIAL:
                maxGroupMembers = 20;
                maxStocks = 10;
                maxProductsPerStock = 200;
                allowOwnProducts = true;
                allowSavedRecipes = true;
                allowMoneySaving = true;
                break;
            case ENTERPRISE:
                maxGroupMembers = Integer.MAX_VALUE;
                maxStocks = Integer.MAX_VALUE;
                maxProductsPerStock = Integer.MAX_VALUE;
                allowOwnProducts = true;
                allowSavedRecipes = true;
                allowMoneySaving = true;
                isEnterprise = true;
                costPerPublishedAd = new BigDecimal("0.99");
                hasMonthlyReport = true;
                break;
            default:
                throw new BadRequestException("O plano " + planCode + " não existe", "Plano inválido");
        }

        return PlanLimitsDto.builder()
                .planCode(planCode)
                .maxGroupMembers(maxGroupMembers)
                .maxStocks(maxStocks)
                .maxProductsPerStock(maxProductsPerStock)
                .allowOwnProducts(allowOwnProducts)
                .allowSavedRecipes(allowSavedRecipes)
                .allowMoneySaving(allowMoneySaving)
                .isEnterprise(isEnterprise)
                .costPerPublishedAd(costPerPublishedAd)
                .hasMonthlyReport(hasMonthlyReport)
                .build();
    }
}
