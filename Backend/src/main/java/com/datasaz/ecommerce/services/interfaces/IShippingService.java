package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.request.ShippingTrackingRequest;
import com.datasaz.ecommerce.models.response.ShippingTrackingResponse;

public interface IShippingService {
    ShippingTrackingResponse updateTracking(Long orderId, ShippingTrackingRequest request);

    ShippingTrackingResponse getTracking(Long orderId);
}