package com.frigus.coreapi.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;
import java.math.BigDecimal;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name="group_invitations")
public class GroupInvitation {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="group_id") private Group group;
 @Column(nullable=false,length=255) private String email;
 @Column(length=255) private String name;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private com.frigus.coreapi.enums.MemberRole memberRole;
 @Column(nullable=false,length=64) private String tokenHash;
 @Column(nullable=false) private Instant expiresAt;
 private Instant acceptedAt;
 private Instant revokedAt;
 @Builder.Default @Column(nullable=false) private Instant createdAt=Instant.now();
}
