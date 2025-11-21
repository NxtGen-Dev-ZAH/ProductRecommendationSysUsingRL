package com.datasaz.ecommerce.mappers;

import com.datasaz.ecommerce.models.request.OrderShippingRequest;
import com.datasaz.ecommerce.models.response.OrderShippingResponse;
import com.datasaz.ecommerce.repositories.entities.OrderShipping;
import com.datasaz.ecommerce.repositories.entities.ShippingStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderShippingMapper {
    public OrderShipping toEntity(OrderShippingRequest request) {
        log.info("Mapping OrderShippingRequest to OrderShipping");
        return OrderShipping.builder()
                .shippingCarrier(request.getShippingCarrier())
                .shippingMethod(request.getShippingMethod())
                .shippingMethodCurrency(request.getShippingMethodCurrency())
                .shippingPrice(request.getShippingPrice())
                .trackingUrl(request.getTrackingUrl())
                .trackingNumber(request.getTrackingNumber())
                .labelUrl(request.getLabelUrl())
                .label(request.getLabel())
                .shippingQuantity(request.getShippingQuantity())
                .shippingWeight(request.getShippingWeight())
                .shippingDimensionRegularOrNot(request.getShippingDimensionRegularOrNot())
                .shippingDimensionHeight(request.getShippingDimensionHeight())
                .shippingDimensionWidth(request.getShippingDimensionWidth())
                .shippingDimensionDepth(request.getShippingDimensionDepth())
                .status(ShippingStatus.PENDING)
                .build();
    }

    public OrderShippingResponse toResponse(OrderShipping entity) {
        log.info("Mapping OrderShipping to OrderShippingResponse");
        if (entity == null) {
            return null;
        }
        return OrderShippingResponse.builder()
                .id(entity.getId())
                .shippingCarrier(entity.getShippingCarrier())
                .shippingMethod(entity.getShippingMethod())
                .shippingMethodCurrency(entity.getShippingMethodCurrency())
                .shippingPrice(entity.getShippingPrice())
                .trackingUrl(entity.getTrackingUrl())
                .trackingNumber(entity.getTrackingNumber())
                .labelUrl(entity.getLabelUrl())
                .label(entity.getLabel())
                .shippingQuantity(entity.getShippingQuantity())
                .shippingWeight(entity.getShippingWeight())
                .shippingDimensionRegularOrNot(entity.getShippingDimensionRegularOrNot())
                .shippingDimensionHeight(entity.getShippingDimensionHeight())
                .shippingDimensionWidth(entity.getShippingDimensionWidth())
                .shippingDimensionDepth(entity.getShippingDimensionDepth())
                .shippedAt(entity.getShippedAt())
                .deliveredAt(entity.getDeliveredAt())
                .status(entity.getStatus())
                .orderId(entity.getOrder() != null ? entity.getOrder().getId() : null)
                .build();
    }
}