package com.datasaz.ecommerce.mappers;


import com.datasaz.ecommerce.models.response.OrderItemResponse;
import com.datasaz.ecommerce.repositories.entities.OrderItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderItemMapper {

    public OrderItemResponse toResponse(OrderItem orderItem) {
        log.info("Mapping order item {} to OrderItemResponse", orderItem.getId());
        if (orderItem == null) {
            return null;
        }

        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .productId(orderItem.getProduct().getId())
                .quantity(orderItem.getQuantity())
                .productName(orderItem.getProductName())
                .price(orderItem.getPrice())
                .orderId(orderItem.getOrder() != null ? orderItem.getOrder().getId() : null)
                .invoiceId(orderItem.getInvoice() != null ? orderItem.getInvoice().getId() : null)
                .build();
    }

//    public Order toEntity(OrderRequest request) {
//        log.info("mapToOrder: convert order request to entity");
//        return Order.builder()
//
//                .build();
//    }
}
