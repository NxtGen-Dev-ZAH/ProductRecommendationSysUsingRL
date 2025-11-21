package com.datasaz.ecommerce.models.request;

import com.datasaz.ecommerce.repositories.entities.PaymentMethods;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentRequest {

    @NotNull
    private Long orderId;
    @NotNull
    private PaymentMethods method;
    private String paymentMethodToken; // For saved payment methods
    private String cardHolderName; // For new card payments
    private String cardNumber; // Full card number (not stored)
    private String cardCVV; // CVV (not stored)
    private String expiryMonth;
    private String expiryYear;
    @NotNull
    @Positive
    private BigDecimal amount;
    @NotNull
    private String currency;
//    private String successUrl;
//    private String cancelUrl;

//    private LocalDateTime paymentDate;
//    @Enumerated(EnumType.STRING)
//    private PaymentStatus status;
//
//    private String transactionId;

//    private PaymentMethod method;

}
