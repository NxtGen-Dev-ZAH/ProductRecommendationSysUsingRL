package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.request.OrderCheckoutRequest;
import com.datasaz.ecommerce.models.response.OrderResponse;
import com.datasaz.ecommerce.repositories.entities.Order;

import java.util.List;

public interface IOrderService {

    Order saveOrder(Order order);

    OrderResponse createOrder(OrderCheckoutRequest orderRequest);

    OrderResponse getOrder(Long id);

    Order getOrderById(Long id);

    List<OrderResponse> getOrdersByUser(Long userId);

    OrderResponse updateOrderStatus(Long id, String status);
}
