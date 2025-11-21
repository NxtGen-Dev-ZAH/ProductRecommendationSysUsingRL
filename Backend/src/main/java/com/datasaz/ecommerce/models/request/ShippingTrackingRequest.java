package com.datasaz.ecommerce.models.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ShippingTrackingRequest {
    @NotBlank
    private String trackingNumber;
    @NotBlank
    private String carrierStatus;
    private LocalDateTime estimatedDeliveryDate;
}