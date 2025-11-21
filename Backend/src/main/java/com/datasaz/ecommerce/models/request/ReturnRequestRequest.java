package com.datasaz.ecommerce.models.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ReturnRequestRequest {
    @NotBlank
    private String reason;
    @NotEmpty
    private List<Long> orderItemIds;
    private Long orderId;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private BigDecimal refundPercentage;
}