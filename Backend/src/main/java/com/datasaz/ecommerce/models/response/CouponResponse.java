package com.datasaz.ecommerce.models.response;

import com.datasaz.ecommerce.repositories.entities.*;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class CouponResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String description;

    @Enumerated(EnumType.STRING)
    private CouponState state;

    @Enumerated(EnumType.STRING)
    private CouponCategory category;

    @Enumerated(EnumType.STRING)
    private CouponScope couponScope;

    @Enumerated(EnumType.STRING)
    private CouponType couponType;

    private BigDecimal minimumOrderAmount;
    private int maxUses;
    private int maxUsesPerUser;

    private Long authorId;

    private LocalDateTime startFrom;
    private LocalDateTime endAt;

    private BigDecimal discountPercentage;
    private BigDecimal discountFixedAmount;

    private Set<CouponTracking> couponTrackings;
}



