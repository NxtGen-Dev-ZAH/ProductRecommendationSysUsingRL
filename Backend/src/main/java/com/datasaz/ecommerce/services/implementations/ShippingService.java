package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.ResourceNotFoundException;
import com.datasaz.ecommerce.models.request.ShippingTrackingRequest;
import com.datasaz.ecommerce.models.response.ShippingTrackingResponse;
import com.datasaz.ecommerce.repositories.OrderRepository;
import com.datasaz.ecommerce.repositories.OrderShippingRepository;
import com.datasaz.ecommerce.repositories.ShippingTrackingRepository;
import com.datasaz.ecommerce.repositories.entities.Order;
import com.datasaz.ecommerce.repositories.entities.OrderShipping;
import com.datasaz.ecommerce.repositories.entities.OrderStatus;
import com.datasaz.ecommerce.repositories.entities.ShippingTracking;
import com.datasaz.ecommerce.services.interfaces.IAuditLogService;
import com.datasaz.ecommerce.services.interfaces.IEmailService;
import com.datasaz.ecommerce.services.interfaces.IShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingService implements IShippingService {
    private final OrderRepository orderRepository;
    private final OrderShippingRepository orderShippingRepository;
    private final ShippingTrackingRepository shippingTrackingRepository;
    private final IEmailService emailService;
    private final IAuditLogService auditLogService;

    @Override
    @Transactional
    public ShippingTrackingResponse updateTracking(Long orderId, ShippingTrackingRequest request) {
        log.info("Updating tracking information for order ID: {}", orderId);

        // Validate order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("Order not found for ID: {}", orderId);
                    return ResourceNotFoundException.builder().message("Order not found.").build();
                });

        // Validate shipping
        OrderShipping orderShipping = orderShippingRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.error("Shipping details not found for order ID: {}", orderId);
                    return ResourceNotFoundException.builder().message("Shipping details not found.").build();
                });

        // Update or create tracking
        ShippingTracking tracking = shippingTrackingRepository.findByOrderShippingId(orderShipping.getId())
                .orElse(ShippingTracking.builder().orderShipping(orderShipping).build());

        tracking.setTrackingNumber(request.getTrackingNumber());
        tracking.setCarrierStatus(request.getCarrierStatus());
        tracking.setEstimatedDeliveryDate(request.getEstimatedDeliveryDate());
        tracking.setLastUpdated(LocalDateTime.now());
        shippingTrackingRepository.save(tracking);

        // Update order status
        order.setOrderStatus(OrderStatus.SHIPPED);
        orderRepository.save(order);

        // Audit log
        String authenticatedUser = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.logAction(
                order.getBuyer().getEmailAddress(),
                "SHIPPING_TRACKING_UPDATED",
                authenticatedUser,
                "Tracking updated for order ID: " + orderId + " with tracking number: " + request.getTrackingNumber()
        );

        // Send email notification
        try {
            emailService.sendTrackingUpdateEmail(
                    order.getBuyer().getEmailAddress(),
                    orderId,
                    request.getTrackingNumber(),
                    request.getCarrierStatus()
            );
            log.info("Sent tracking update email to {} for order {}", order.getBuyer().getEmailAddress(), orderId);
        } catch (Exception e) {
            log.error("Failed to send tracking update email to {}: {}", order.getBuyer().getEmailAddress(), e.getMessage());
        }

        return ShippingTrackingResponse.builder()
                .id(tracking.getId())
                .trackingNumber(tracking.getTrackingNumber())
                .carrierStatus(tracking.getCarrierStatus())
                .estimatedDeliveryDate(tracking.getEstimatedDeliveryDate())
                .lastUpdated(tracking.getLastUpdated())
                .orderId(orderId)
                .build();
    }

    @Override
    public ShippingTrackingResponse getTracking(Long orderId) {
        log.info("Retrieving tracking information for order ID: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Order not found.").build());

        OrderShipping orderShipping = orderShippingRepository.findByOrderId(orderId)
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Shipping details not found.").build());

        ShippingTracking tracking = shippingTrackingRepository.findByOrderShippingId(orderShipping.getId())
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Tracking information not found.").build());

        return ShippingTrackingResponse.builder()
                .id(tracking.getId())
                .trackingNumber(tracking.getTrackingNumber())
                .carrierStatus(tracking.getCarrierStatus())
                .estimatedDeliveryDate(tracking.getEstimatedDeliveryDate())
                .lastUpdated(tracking.getLastUpdated())
                .orderId(orderId)
                .build();
    }
}