package com.datasaz.ecommerce.models.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemResponse {
    //InvoiceItem invoiceItem;
    private Long id;
    private int quantity;
    private String productName;
    private BigDecimal price;
    private Long orderId;
    private Long productId;
    private Long invoiceId;
}



