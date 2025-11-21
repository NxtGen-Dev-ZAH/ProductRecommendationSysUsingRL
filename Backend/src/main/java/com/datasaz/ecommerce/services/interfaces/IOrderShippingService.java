package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.request.OrderShippingRequest;
import com.datasaz.ecommerce.models.response.OrderShippingResponse;

public interface IOrderShippingService {
    OrderShippingResponse createOrderShipping(Long orderId, OrderShippingRequest request);

    OrderShippingResponse getOrderShipping(Long orderId);

    OrderShippingResponse updateOrderShipping(Long orderId, OrderShippingRequest request);
}
