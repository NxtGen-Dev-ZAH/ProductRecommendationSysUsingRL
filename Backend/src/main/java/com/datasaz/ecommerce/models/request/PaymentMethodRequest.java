package com.datasaz.ecommerce.models.request;

import com.datasaz.ecommerce.repositories.entities.User;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentMethodRequest {

    @NotBlank
    private String cardHolderName;
    @NotBlank
    private String cardNumberMasked;
    @NotBlank
    private String cardCVVMasked;
    @NotBlank
    private String expiryMonth;
    @NotBlank
    private String expiryYear;
    private String cardBrand; // VISA, MasterCard, etc.
    private String cardLast4;
    private String cardCountry;
    private String cardCurrency;
    private boolean isDefault;
    private String token; // If using third-party tokenization (Stripe, etc.)
    private LocalDateTime savedAt;

    private User user;
}
