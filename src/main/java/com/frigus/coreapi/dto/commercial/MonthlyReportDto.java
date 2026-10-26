package com.frigus.coreapi.dto.commercial;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import com.frigus.coreapi.enums.*;
public record MonthlyReportDto(UUID groupId, String month, BigDecimal totalExpenses, BigDecimal purchaseTotal, long completedPurchases, Map<String,BigDecimal> expensesByCategory) { }
