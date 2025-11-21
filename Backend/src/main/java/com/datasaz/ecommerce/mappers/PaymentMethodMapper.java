package com.datasaz.ecommerce.mappers;

import com.datasaz.ecommerce.models.request.PaymentMethodRequest;
import com.datasaz.ecommerce.models.response.PaymentMethodResponse;
import com.datasaz.ecommerce.repositories.entities.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentMethodMapper {
    public PaymentMethod toEntity(PaymentMethodRequest request) {
        log.info("Mapping PaymentMethodRequest to PaymentMethod");
        return PaymentMethod.builder()
                .cardHolderName(request.getCardHolderName())
                .cardNumberMasked(request.getCardNumberMasked())
                .cardCVVMasked(request.getCardCVVMasked())
                .expiryMonth(request.getExpiryMonth())
                .expiryYear(request.getExpiryYear())
                .cardBrand(request.getCardBrand())
                .cardLast4(request.getCardLast4())
                .cardCountry(request.getCardCountry())
                .cardCurrency(request.getCardCurrency())
                .isDefault(request.isDefault())
                .token(request.getToken())
                .savedAt(request.getSavedAt())
                .user(request.getUser())
                .build();
    }

    public PaymentMethodResponse toResponse(PaymentMethod entity) {
        log.info("Mapping PaymentMethod to PaymentMethodResponse");
        if (entity == null) {
            return null;
        }
        return PaymentMethodResponse.builder()
                .id(entity.getId())
                .cardHolderName(entity.getCardHolderName())
                .cardNumberMasked(entity.getCardNumberMasked())
                .cardCVVMasked(entity.getCardCVVMasked())
                .expiryMonth(entity.getExpiryMonth())
                .expiryYear(entity.getExpiryYear())
                .cardBrand(entity.getCardBrand())
                .cardLast4(entity.getCardLast4())
                .cardCountry(entity.getCardCountry())
                .cardCurrency(entity.getCardCurrency())
                .isDefault(entity.isDefault())
                .token(entity.getToken())
                .savedAt(entity.getSavedAt())
                .user(entity.getUser())
                .build();
    }
}