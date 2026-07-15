package com.frigus.coreapi.model;

import com.frigus.coreapi.enums.Category;
import com.frigus.coreapi.enums.ProductStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stock_products")
public class StockProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @NotNull
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "minimal_quantity")
    private Integer minimalQuantity;

    @NotNull
    @Column(name = "expire_date", nullable = false)
    private LocalDate expireDate;

    @Column(name = "product_status", columnDefinition = "product_status_enum")
    @Enumerated(EnumType.STRING)
    private ProductStatus productStatus;

    @Column(name = "category", columnDefinition = "category_enum not null")
    @Enumerated(EnumType.STRING)
    private Category category;


}