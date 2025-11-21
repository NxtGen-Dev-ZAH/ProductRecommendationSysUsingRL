package com.datasaz.ecommerce.repositories.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;


@Table(name = "product_variant")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;   // variant name (e.g. size, or differences in color)

    @Column(precision = 19, scale = 2)
    private BigDecimal priceAdjustment;

    @Column(nullable = false)
    private int quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}