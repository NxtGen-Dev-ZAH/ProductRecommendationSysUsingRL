package com.datasaz.ecommerce.models.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse {
    private Long id;
    private String name;
    private BigDecimal priceAdjustment;
    private int quantity;
}