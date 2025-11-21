package com.datasaz.ecommerce.models.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CartResponse {

    private Long id;

    private String sessionId;
    private Long userId;

    private List<CartItemResponse> items;

    private CouponResponse couponResponse;

    private BigDecimal subtotalPrice;
    private BigDecimal totalShippingCost;
    private BigDecimal totalDiscount;
    private BigDecimal totalAmount;

}
