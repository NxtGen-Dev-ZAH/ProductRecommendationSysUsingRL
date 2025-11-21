package com.datasaz.ecommerce.models.response;

import com.datasaz.ecommerce.repositories.entities.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentMethodResponse {

    private Long id;
    private String cardHolderName;
    private String cardNumberMasked;
    private String cardCVVMasked;
    private String expiryMonth;
    private String expiryYear;
    private String cardBrand;
    private String cardLast4;
    private String cardCountry;
    private String cardCurrency;
    private boolean isDefault;
    private String token;
    private LocalDateTime savedAt;

    private User user;
}
