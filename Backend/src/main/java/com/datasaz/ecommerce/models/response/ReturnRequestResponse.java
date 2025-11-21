package com.datasaz.ecommerce.models.response;

import com.datasaz.ecommerce.repositories.entities.ReturnStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ReturnRequestResponse {
    private Long id;
    private String reason;
    private ReturnStatus status;
    private LocalDateTime requestDate;
    private BigDecimal refundAmount;
    private BigDecimal refundPercentage;

    private Long orderId;
    private List<Long> orderItemIds;
}