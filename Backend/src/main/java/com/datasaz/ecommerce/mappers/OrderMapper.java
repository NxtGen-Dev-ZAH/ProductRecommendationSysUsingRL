package com.datasaz.ecommerce.mappers;


import com.datasaz.ecommerce.models.response.OrderItemResponse;
import com.datasaz.ecommerce.models.response.OrderResponse;
import com.datasaz.ecommerce.repositories.entities.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMapper {
    private final OrderItemMapper orderItemMapper;

    public OrderResponse toResponse(Order order) {
        log.info("Mapping order {} to OrderResponse", order.getId());
        if (order == null) {
            return null;
        }

        List<OrderItemResponse> itemResponses = order.getItems() != null
                ? order.getItems().stream()
                .map(orderItemMapper::toResponse)
                .collect(Collectors.toList())
                : Collections.emptyList();

        return OrderResponse.builder()
                .id(order.getId())
                .orderStatus(order.getOrderStatus())
                .orderDateTime(order.getOrderDateTime())
                .buyerId(order.getBuyer() != null ? order.getBuyer().getId() : null)
                .usedCouponIdentifier(order.getUsedCoupon() != null ? order.getUsedCoupon().getCode() : null)
                .discountAmount(order.getDiscountAmount())
                .totalVAT(order.getTotalVAT())
                .totalAmount(order.getTotalAmount())
                .items(itemResponses)
                .build();
    }

//    public Order toEntity(OrderRequest request) {
//        log.info("mapToOrder: convert order request to entity");
//        return Order.builder()
//
//                .build();
//    }
}
