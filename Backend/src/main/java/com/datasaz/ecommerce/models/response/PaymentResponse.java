package com.datasaz.ecommerce.models.response;

import com.datasaz.ecommerce.repositories.entities.PaymentMethods;
import com.datasaz.ecommerce.repositories.entities.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {

    private Long id;
    private LocalDateTime paymentDate;
    private BigDecimal amount;
    private PaymentStatus status;
    private String transactionId;
    private PaymentMethods method;
    private Long orderId;
    private String paymentUrl; // For redirect-based payments (Stripe, PayPal)

//    private PaymentMethod method;

}
