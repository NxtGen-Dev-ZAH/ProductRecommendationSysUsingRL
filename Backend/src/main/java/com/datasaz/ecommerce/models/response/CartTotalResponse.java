package com.datasaz.ecommerce.models.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CartTotalResponse {
    private final BigDecimal total;
    private final BigDecimal discount;

}
