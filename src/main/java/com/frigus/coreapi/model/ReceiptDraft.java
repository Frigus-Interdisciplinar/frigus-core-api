package com.frigus.coreapi.model;

import com.frigus.coreapi.enums.ReceiptStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="receipt_drafts") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReceiptDraft {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="stock_id",nullable=false) private Stock stock;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="created_by",nullable=false) private User createdBy;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private ReceiptStatus status;
    @Column(name="access_key",length=44) private String accessKey;
    @Column(name="issuer_name",length=255) private String issuerName;
    @Column(name="issued_at") private Instant issuedAt;
    @Column(name="total_amount",precision=12,scale=2) private java.math.BigDecimal totalAmount;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb",nullable=false) private List<String> warnings;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="imported_at") private Instant importedAt;
    @PrePersist void initialize() { if (id == null) id=UUID.randomUUID(); if (createdAt==null) createdAt=Instant.now(); }
}
