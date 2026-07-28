package com.frigus.coreapi.model;

import com.frigus.coreapi.enums.AccountType;
import com.frigus.coreapi.enums.SubscriptionStatus;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "name", nullable = false, length = Integer.MAX_VALUE)
    private String name;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "account_type", columnDefinition = "account_type_enum not null")
    @Enumerated(EnumType.STRING)
    private AccountType accountType;

    @NotNull
    @Column(name = "email", nullable = false, length = Integer.MAX_VALUE)
    private String email;

    @NotNull
    @Column(name = "hash_password", nullable = false, length = Integer.MAX_VALUE)
    private String hashPassword;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Subscription subscription;


    public String getPlanNameFromUser() {
        Subscription subscription = this.getSubscription();

        if(subscription != null && subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            return subscription.getPlan().getName();
        }

        return "Frigus Free";
    }
}