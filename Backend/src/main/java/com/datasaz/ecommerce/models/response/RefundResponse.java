package com.datasaz.ecommerce.models.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RefundResponse {
    private Long id;
    private LocalDateTime refundDate;
    private BigDecimal amount;
    private String transactionId;
    private String reason;
    private Long paymentId;
    private Long returnRequestId;
}