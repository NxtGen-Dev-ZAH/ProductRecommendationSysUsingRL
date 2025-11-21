package com.datasaz.ecommerce.models.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CouponCodeRequest {
    @NotBlank(message = "Coupon code cannot be blank")
    private String code;
}
