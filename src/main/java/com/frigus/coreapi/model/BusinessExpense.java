package com.frigus.coreapi.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;
import java.math.BigDecimal;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name="business_expenses")
public class BusinessExpense {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="group_id") private Group group;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="created_by") private User createdBy;
 @Column(nullable=false,length=255) private String description;
 @Column(nullable=false,length=120) private String category;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal amount;
 @Column(nullable=false) private LocalDate expenseDate;
 @Column(length=255) private String supplier;
 @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="shopping_list_id") private ShoppingList shoppingList;
 @Builder.Default @Column(nullable=false) private Instant createdAt=Instant.now();
 @Builder.Default @Column(nullable=false) private Instant updatedAt=Instant.now();
}
