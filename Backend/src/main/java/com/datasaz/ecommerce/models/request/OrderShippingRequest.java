package com.datasaz.ecommerce.models.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderShippingRequest {

    @NotBlank
    private String shippingCarrier;
    @NotBlank
    private String shippingMethod;
    @NotBlank
    private String shippingMethodCurrency;
    @NotBlank
    private String shippingPrice;
    private String trackingUrl;
    private String trackingNumber;
    private String labelUrl;
    private String label;
    @NotNull
    @Positive
    private Integer shippingQuantity;
    @NotBlank
    private String shippingWeight;
    private Boolean shippingDimensionRegularOrNot;
    private String shippingDimensionHeight;
    private String shippingDimensionWidth;
    private String shippingDimensionDepth;

}
