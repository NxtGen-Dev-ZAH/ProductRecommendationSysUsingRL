package com.datasaz.ecommerce.models.response;

import com.datasaz.ecommerce.repositories.entities.OrderItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class InvoiceResponse {
    //Invoice invoice;
    private Long id;
    private String customerName;
    private String email;
    private LocalDateTime orderDate;
    private BigDecimal totalDiscount;
    private BigDecimal totalVAT;
    private BigDecimal totalAmount;
    private List<OrderItem> items;
}



