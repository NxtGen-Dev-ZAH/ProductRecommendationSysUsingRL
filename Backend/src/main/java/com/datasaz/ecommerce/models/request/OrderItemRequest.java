package com.datasaz.ecommerce.models.request;

import com.datasaz.ecommerce.repositories.entities.Invoice;
import com.datasaz.ecommerce.repositories.entities.Order;
import com.datasaz.ecommerce.repositories.entities.Product;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class OrderItemRequest {

    private int quantity;
    private Order order;
    private Product product;
    private Invoice invoice;
}
