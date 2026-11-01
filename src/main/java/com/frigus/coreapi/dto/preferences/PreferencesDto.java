package com.frigus.coreapi.dto.preferences;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import com.frigus.coreapi.enums.*;
public record PreferencesDto(boolean expirationAlertsEnabled, boolean lowStockAlertsEnabled,
 boolean shoppingRemindersEnabled, boolean weeklySummaryEnabled,
 @NotNull @Pattern(regexp="LIGHT|DARK|SYSTEM") String theme) { }
