package com.frigus.coreapi.dto.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingJobResultDto {
    private int evaluatedCount;
    private int transactionsGeneratedCount;
    private int expiredCount;
    private int skippedCount;
    private int errorsCount;
    private List<String> errorMessages;
    private Instant executedAt;
    private long executionDurationMs;
}
