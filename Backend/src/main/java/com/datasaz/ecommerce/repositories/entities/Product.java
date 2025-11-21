package com.datasaz.ecommerce.repositories.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Table(name = "product")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    @PositiveOrZero
    private BigDecimal price;
    @Column(precision = 19, scale = 2)
    @PositiveOrZero
    private BigDecimal offerPrice;

    @Column(nullable = false)
    @PositiveOrZero
    private int quantity;

    @Column(precision = 19, scale = 2)
    @PositiveOrZero
    private BigDecimal shippingCost;

    @Column(precision = 19, scale = 2)
    @PositiveOrZero
    private BigDecimal eachAdditionalItemShippingCost;

    @Column(length = 100)
    private String inventoryLocation;
    @Column(length = 100)
    private String warranty;
    @Column(length = 50)
    private String brand;
    @Column(length = 50)
    private String productCode;
    @Column(length = 50)
    private String manufacturingPieceNumber;

    private LocalDate manufacturingDate;
    private LocalDate expirationDate;

    @Column(length = 50)
    private String EAN;

    @Column(length = 50)
    private String manufacturingPlace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus productStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductSellType productSellType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCondition productCondition;

    @Column(length = 100)
    private String productConditionComment;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean deleted;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductImage> images;// = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductImageAttach> imageAttaches; // = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ProductVariant> variants;// = new ArrayList<>();

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToMany(mappedBy = "favoriteProducts")
    private Set<User> usersFavourite;// = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

}
//    @ManyToMany(mappedBy = "products")
//    private Set<PublicityCompaign> promotions = new HashSet<>();


//    @OneToOne(mappedBy = "product")
//    private ProductDescription productDescription;
//
//    @OneToOne(mappedBy = "product")
//    private ProductStatus productStatus;
//
//    @OneToOne(mappedBy = "product")
//    private ProductStatistics productStatistics;
//
//    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<ProductImage> images = new ArrayList<>();
//
//    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<ProductCustomFields> customFields;

//    @OneToMany(mappedBy = "product")
//    private List<InvoiceItem> orderItems;
//
//    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
//    private Set<ProductReview> reviews = new HashSet<>();
