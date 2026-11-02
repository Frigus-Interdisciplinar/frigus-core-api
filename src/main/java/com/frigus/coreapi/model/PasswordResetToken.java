package com.frigus.coreapi.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;
import java.math.BigDecimal;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name="password_reset_tokens")
public class PasswordResetToken {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id") private User user;
 @Column(nullable=false,length=64) private String tokenHash;
 @Column(nullable=false) private Instant expiresAt;
 private Instant consumedAt;
 @Builder.Default @Column(nullable=false) private Instant createdAt=Instant.now();
}
