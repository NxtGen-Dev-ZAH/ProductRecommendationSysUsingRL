package com.datasaz.ecommerce.models.response;

import com.datasaz.ecommerce.repositories.entities.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {

    private Long id;

    private OrderStatus orderStatus;
    private LocalDateTime orderDateTime;

    private Long buyerId;
    private Long buyerCompanyId;

    private String usedCouponIdentifier;

    private BigDecimal discountAmount;
    private BigDecimal totalVAT;
    private BigDecimal totalAmount;
    private List<OrderItemResponse> items;

    //private Payment payment;

}
