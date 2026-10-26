package com.frigus.coreapi.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;
import java.math.BigDecimal;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name="user_preferences")
public class UserPreferences {
 @Id @Column(name="user_id") private UUID userId;
 @Builder.Default @Column(nullable=false) private boolean expirationAlertsEnabled=true;
 @Builder.Default @Column(nullable=false) private boolean lowStockAlertsEnabled=true;
 @Builder.Default @Column(nullable=false) private boolean shoppingRemindersEnabled=true;
 @Column(nullable=false) private boolean weeklySummaryEnabled;
 @Builder.Default @Column(nullable=false,length=16) private String theme="SYSTEM";
 @Builder.Default @Column(nullable=false) private Instant createdAt=Instant.now();
 @Builder.Default @Column(nullable=false) private Instant updatedAt=Instant.now();

}
