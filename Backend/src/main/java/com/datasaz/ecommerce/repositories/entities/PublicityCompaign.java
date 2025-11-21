package com.datasaz.ecommerce.repositories.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;


@Table(name = "publicity_compaign")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicityCompaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private String title;
    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    private CompaignStrategy compaignStrategy; // discount Strategy

    @Enumerated(EnumType.STRING)
    private CompaignType compaignType; // publicity Type


    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean active;

    private BigDecimal minimumPriceRange;
    private BigDecimal maximumPriceRange;

    private String brand;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToMany
    @JoinTable(
            name = "publicity_product",
            joinColumns = @JoinColumn(name = "publicity_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private Set<Product> products;// = new HashSet<>();
}
