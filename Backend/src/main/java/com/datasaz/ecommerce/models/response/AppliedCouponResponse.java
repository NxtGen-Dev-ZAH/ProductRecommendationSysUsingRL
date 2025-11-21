package com.datasaz.ecommerce.models.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AppliedCouponResponse {

    String code;
    BigDecimal discount;
    CartResponse cartResponse;
}



