package com.datasaz.ecommerce.models;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class StripePaymentResponse {
    private String sessionId;
    private String paymentUrl;
}
