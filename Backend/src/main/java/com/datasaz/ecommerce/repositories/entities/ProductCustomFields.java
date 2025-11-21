package com.datasaz.ecommerce.repositories.entities;

import jakarta.persistence.*;
import lombok.*;

@Table(name = "product_custom_fields")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCustomFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fieldKey;

    @Column(nullable = false, length = 250)
    private String fieldValue;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, referencedColumnName = "id")
    private Product product;
}
