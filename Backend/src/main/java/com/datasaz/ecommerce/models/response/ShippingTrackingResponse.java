package com.datasaz.ecommerce.models.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ShippingTrackingResponse {
    private Long id;
    private String trackingNumber;
    private String carrierStatus;
    private LocalDateTime estimatedDeliveryDate;
    private LocalDateTime lastUpdated;
    private Long orderId;
}