package com.frigus.coreapi.model;

import com.frigus.coreapi.enums.AdvertisementStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "advertisements")
public class Advertisement {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "advertiser_id", nullable = false)
    private User advertiser;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "product_name", nullable = false, length = 160)
    private String productName;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;

    @Column(name = "destination_url", length = 2048)
    private String destinationUrl;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private AdvertisementStatus status = AdvertisementStatus.DRAFT;

    @Column(nullable = false)
    @Builder.Default
    private long impressions = 0;

    @Column(nullable = false)
    @Builder.Default
    private long clicks = 0;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
