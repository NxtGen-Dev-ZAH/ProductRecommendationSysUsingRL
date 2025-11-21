package com.datasaz.ecommerce.repositories.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "coupon")
@Getter
@Setter
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CouponState state;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CouponCategory category;

    @Enumerated(EnumType.STRING)
    private CouponScope couponScope;

    @Enumerated(EnumType.STRING)
    private CouponType couponType;

    private BigDecimal minimumOrderAmount;
    private int maxUses;
    private int maxUsesPerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    private LocalDateTime startFrom;
    @Column(nullable = false)
    private LocalDateTime endAt;

    @Column
    private BigDecimal discountPercentage;
    @Column
    private BigDecimal discountFixedAmount;

//    @OneToMany(mappedBy = "coupon", targetEntity = CouponTracking.class, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    private Set<CouponTracking> couponTrackings;

    @OneToMany(mappedBy = "coupon", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<CouponTracking> couponTrackings; // = new HashSet<>();

    // Optimistic locking (default with @Version)
    @Version
    private Long version;

    @PrePersist
    @PreUpdate
    private void validateDiscount() {
        if (couponType == CouponType.PERCENTAGE && discountPercentage == null) {
            throw new IllegalStateException("Discount percentage must be set for PERCENTAGE coupon");
        }
        if (couponType == CouponType.FIXED && discountFixedAmount == null) {
            throw new IllegalStateException("Discount fixed amount must be set for FIXED coupon");
        }
    }
}
