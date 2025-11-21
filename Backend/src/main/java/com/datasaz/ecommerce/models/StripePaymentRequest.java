package com.datasaz.ecommerce.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StripePaymentRequest {
    private Long clientId;
    private Double totalAmount;
    private String currency;
    private String successUrl;
    private String cancelUrl;
    private String address; // Adresse
    private String postalCode; // Code postal
}
