package com.datasaz.ecommerce.models.request;

import com.datasaz.ecommerce.repositories.entities.OrderItem;
import jakarta.validation.constraints.Email;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
public class InvoiceRequest {

    private String customerName;
    @Email(message = "Invalid email format")
    private String email;
    private LocalDateTime orderDate;
    private BigDecimal totalDiscount;
    private BigDecimal totalVAT;
    private BigDecimal totalAmount;
    private List<OrderItem> items;
}
