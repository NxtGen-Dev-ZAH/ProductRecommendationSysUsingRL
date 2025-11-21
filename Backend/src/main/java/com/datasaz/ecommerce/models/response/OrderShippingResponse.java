package com.datasaz.ecommerce.models.response;

import com.datasaz.ecommerce.repositories.entities.ShippingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrderShippingResponse {

    private Long id;
    private String shippingCarrier;
    private String shippingMethod;
    private String shippingMethodCurrency;
    private String shippingPrice;
    private String trackingUrl;
    private String trackingNumber;
    private String labelUrl;
    private String label;
    private Integer shippingQuantity;
    private String shippingWeight;
    private Boolean shippingDimensionRegularOrNot;
    private String shippingDimensionHeight;
    private String shippingDimensionWidth;
    private String shippingDimensionDepth;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private ShippingStatus status;
    private Long orderId;

}
