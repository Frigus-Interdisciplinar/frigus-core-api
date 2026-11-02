package com.frigus.coreapi.model;

import com.frigus.coreapi.enums.SupportRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "support_requests")
public class SupportRequest {
    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true, length = 16)
    private String protocol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private SupportRequestStatus status;

    @Column(nullable = false, length = 120)
    private String subject;

    @Column(nullable = false, length = 5000)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void assignIdentityAndTimestamps() {
        if (id == null) id = UUID.randomUUID();
        if (protocol == null) protocol = "FRG-" + id.toString().substring(0, 8).toUpperCase();
        if (status == null) status = SupportRequestStatus.OPEN;
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = createdAt;
    }
}
