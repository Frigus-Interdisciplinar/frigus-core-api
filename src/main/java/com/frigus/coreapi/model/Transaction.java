package com.frigus.coreapi.model;

import com.frigus.coreapi.enums.PaymentMethod;
import com.frigus.coreapi.enums.TransactionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "idempotency_key", length = 120, nullable = false, unique = true)
    private String idempotencyKey;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method", columnDefinition = "payment_method_enum not null")
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Size(max = 4)
    @Column(name = "fake_card_last4", length = 4)
    private String fakeCardLast4;

    @Column(name = "fake_pix_key", length = Integer.MAX_VALUE)
    private String fakePixKey;

    @ColumnDefault("'Pendente'")
    @Column(name = "status", columnDefinition = "transaction_status_enum not null")
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Column(name = "queue_job_id", length = Integer.MAX_VALUE)
    private String queueJobId;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "attempts", nullable = false)
    private Integer attempts;

    @NotNull
    @ColumnDefault("3")
    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts;

    @Column(name = "error_message", length = Integer.MAX_VALUE)
    private String errorMessage;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "queued_at")
    private Instant queuedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;


}