package com.datasaz.ecommerce.models.request;

import com.datasaz.ecommerce.repositories.entities.CouponCategory;
import com.datasaz.ecommerce.repositories.entities.CouponScope;
import com.datasaz.ecommerce.repositories.entities.CouponState;
import com.datasaz.ecommerce.repositories.entities.CouponType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@Builder
public class CouponRequest {

    @NotBlank(message = "Coupon code cannot be blank")
    private String code;
    private String description;

    @Enumerated(EnumType.STRING)
    private CouponState state;

    @NotNull(message = "Coupon category cannot be null")
    @Enumerated(EnumType.STRING)
    private CouponCategory category;

    @Enumerated(EnumType.STRING)
    private CouponScope couponScope;

    @NotNull(message = "Coupon type cannot be null")
    @Enumerated(EnumType.STRING)
    private CouponType couponType;

    @DecimalMin(value = "0.0", inclusive = false, message = "Minimum order amount must be greater than 0")
    private BigDecimal minimumOrderAmount;
    @Min(value = 0, message = "Maximum uses must be >= 0")
    private int maxUses;
    @Min(value = 0, message = "Maximum uses per user must be >= 0")
    private int maxUsesPerUser;

    @NotNull(message = "Coupon start date cannot be null")
    private LocalDateTime startFrom;
    @NotNull(message = "Coupon end date cannot be null")
    private LocalDateTime endAt;

    @DecimalMin(value = "0.0", message = "Discount percentage must be >= 0")
    @DecimalMax(value = "100.0", message = "Discount percentage cannot exceed 100")
    private BigDecimal discountPercentage;

    @DecimalMin(value = "0.0", message = "Discount fixed amount must be >= 0")
    private BigDecimal discountFixedAmount;
}
