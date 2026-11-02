package com.frigus.coreapi.service;
import com.frigus.coreapi.dto.preferences.PreferencesDto;
import com.frigus.coreapi.model.UserPreferences;
import com.frigus.coreapi.repository.UserPreferencesRepository;
import com.frigus.coreapi.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Service @RequiredArgsConstructor
public class PreferencesService {
 private final UserPreferencesRepository repository;
 private final GroupAccessService access;
 @Transactional(readOnly=true) public PreferencesDto get() {return toDto(getForUser(access.requireCurrentUser().getId()));}
 @Transactional public PreferencesDto update(PreferencesDto dto) {
  var p=getForUser(access.requireCurrentUser().getId());
  p.setExpirationAlertsEnabled(dto.expirationAlertsEnabled());p.setLowStockAlertsEnabled(dto.lowStockAlertsEnabled());
  p.setShoppingRemindersEnabled(dto.shoppingRemindersEnabled());p.setWeeklySummaryEnabled(dto.weeklySummaryEnabled());
  p.setTheme(dto.theme());p.setUpdatedAt(java.time.Instant.now());return toDto(repository.save(p));
 }
 public UserPreferences getForUser(UUID userId) {return repository.findById(userId).orElseGet(() -> UserPreferences.builder().userId(userId).build());}
 public boolean allows(UUID userId,NotificationType type) {
  var p=getForUser(userId);
  return switch(type) {
   case PRODUCT_NEAR_EXPIRATION -> p.isExpirationAlertsEnabled();
   case LOW_STOCK,OUT_OF_STOCK -> p.isLowStockAlertsEnabled();
   case SHOPPING_LIST_REMINDER -> p.isShoppingRemindersEnabled();
   case WEEKLY_SUMMARY -> p.isWeeklySummaryEnabled();
   default -> true;
  };
 }
 private PreferencesDto toDto(UserPreferences p) {return new PreferencesDto(p.isExpirationAlertsEnabled(),p.isLowStockAlertsEnabled(),p.isShoppingRemindersEnabled(),p.isWeeklySummaryEnabled(),p.getTheme());}
}
