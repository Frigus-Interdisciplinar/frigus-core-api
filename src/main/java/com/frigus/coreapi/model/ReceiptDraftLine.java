package com.frigus.coreapi.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import com.frigus.coreapi.enums.Category;
import com.frigus.coreapi.enums.StoragePlace;
import com.frigus.coreapi.enums.UnitOfMeasure;

@Entity @Table(name="receipt_draft_lines") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReceiptDraftLine {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="receipt_id",nullable=false) private ReceiptDraft receipt;
    @Column(nullable=false,length=500) private String description;
    @Column(name="quantity",nullable=false) private Integer quantity;
    @Column(length=30) private String unit;
    @Column(name="unit_price",precision=12,scale=2) private BigDecimal unitPrice;
    @Column(name="total_price",precision=12,scale=2) private BigDecimal totalPrice;
    @Column private Double confidence;
    @Column(length=1000) private String evidence;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="matched_product_id") private Product matchedProduct;
    @Column(name="expire_date") private LocalDate expireDate;
    @Column(name="normalized_product_name",length=255) private String normalizedProductName;
    @Column(length=120) private String brand;
    @Enumerated(EnumType.STRING) @Column(length=30) private Category category;
    @Enumerated(EnumType.STRING) @Column(name="storage_place",length=30) private StoragePlace storagePlace;
    @Enumerated(EnumType.STRING) @Column(name="unit_of_measure",length=30) private UnitOfMeasure unitOfMeasure;
    @PrePersist void initialize() { if (id == null) id=UUID.randomUUID(); }
}
