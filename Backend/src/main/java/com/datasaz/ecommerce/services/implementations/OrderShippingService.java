package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.ResourceNotFoundException;
import com.datasaz.ecommerce.exceptions.UnauthorizedException;
import com.datasaz.ecommerce.mappers.OrderShippingMapper;
import com.datasaz.ecommerce.models.request.OrderShippingRequest;
import com.datasaz.ecommerce.models.response.OrderShippingResponse;
import com.datasaz.ecommerce.repositories.OrderRepository;
import com.datasaz.ecommerce.repositories.OrderShippingRepository;
import com.datasaz.ecommerce.repositories.entities.Order;
import com.datasaz.ecommerce.repositories.entities.OrderShipping;
import com.datasaz.ecommerce.services.interfaces.IAuditLogService;
import com.datasaz.ecommerce.services.interfaces.IEmailService;
import com.datasaz.ecommerce.services.interfaces.IOrderShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderShippingService implements IOrderShippingService {
    private final OrderShippingRepository orderShippingRepository;
    private final OrderRepository orderRepository;
    private final OrderShippingMapper orderShippingMapper;
    private final IAuditLogService auditLogService;
    private final IEmailService emailService;

    @Override
    @Transactional
    public OrderShippingResponse createOrderShipping(Long orderId, OrderShippingRequest request) {
        log.info("Creating shipping details for order ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("Order not found for ID: {}", orderId);
                    return ResourceNotFoundException.builder().message("Order not found.").build();
                });

        // Validate user ownership
        String authenticatedUser = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!order.getBuyer().getEmailAddress().equals(authenticatedUser)) {
            log.error("User {} not authorized to add shipping details for order {}", authenticatedUser, orderId);
            throw UnauthorizedException.builder().message("Not authorized to add shipping details for this order.").build();
        }

        // Check if shipping details already exist
        if (order.getOrderShipping() != null) {
            log.error("Shipping details already exist for order ID: {}", orderId);
            throw BadRequestException.builder().message("Shipping details already exist for this order.").build();
        }

        OrderShipping orderShipping = orderShippingMapper.toEntity(request);
        orderShipping.setOrder(order);
        orderShippingRepository.save(orderShipping);

        // Audit log
        auditLogService.logAction(
                order.getBuyer().getEmailAddress(),
                "SHIPPING_DETAILS_ADDED",
                authenticatedUser,
                "Shipping details added for order ID: " + orderId
        );

        // Send email notification
        try {
            emailService.sendEmail(
                    order.getBuyer().getEmailAddress(),
                    "Shipping Details Added",
                    String.format(
                            "<h2>Shipping Details Added</h2>" +
                                    "<p>Shipping details for order ID %d have been added successfully.</p>" +
                                    "<p>Carrier: %s</p><p>Method: %s</p><p>Price: %s %s</p>",
                            orderId, request.getShippingCarrier(), request.getShippingMethod(),
                            request.getShippingPrice(), request.getShippingMethodCurrency()
                    )
            );
            log.info("Sent shipping details email to {} for order {}", order.getBuyer().getEmailAddress(), orderId);
        } catch (Exception e) {
            log.error("Failed to send shipping details email to {}: {}", order.getBuyer().getEmailAddress(), e.getMessage());
        }

        return orderShippingMapper.toResponse(orderShipping);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderShippingResponse getOrderShipping(Long orderId) {
        log.info("Retrieving shipping details for order ID: {}", orderId);
        OrderShipping orderShipping = orderShippingRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.error("Shipping details not found for order ID: {}", orderId);
                    return ResourceNotFoundException.builder().message("Shipping details not found.").build();
                });

        // Validate user ownership
        String authenticatedUser = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!orderShipping.getOrder().getBuyer().getEmailAddress().equals(authenticatedUser)) {
            log.error("User {} not authorized to view shipping details for order {}", authenticatedUser, orderId);
            throw new AccessDeniedException("Not authorized to view shipping details for this order.");
        }

        return orderShippingMapper.toResponse(orderShipping);
    }

    @Override
    @Transactional
    public OrderShippingResponse updateOrderShipping(Long orderId, OrderShippingRequest request) {
        log.info("Updating shipping details for order ID: {}", orderId);
        OrderShipping orderShipping = orderShippingRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.error("Shipping details not found for order ID: {}", orderId);
                    return ResourceNotFoundException.builder().message("Shipping details not found.").build();
                });

        // Validate user ownership
        String authenticatedUser = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!orderShipping.getOrder().getBuyer().getEmailAddress().equals(authenticatedUser)) {
            log.error("User {} not authorized to update shipping details for order {}", authenticatedUser, orderId);
            throw new AccessDeniedException("Not authorized to update shipping details for this order.");
        }

        // Update fields
        orderShipping.setShippingCarrier(request.getShippingCarrier());
        orderShipping.setShippingMethod(request.getShippingMethod());
        orderShipping.setShippingMethodCurrency(request.getShippingMethodCurrency());
        orderShipping.setShippingPrice(request.getShippingPrice());
        orderShipping.setTrackingUrl(request.getTrackingUrl());
        orderShipping.setTrackingNumber(request.getTrackingNumber());
        orderShipping.setLabelUrl(request.getLabelUrl());
        orderShipping.setLabel(request.getLabel());
        orderShipping.setShippingQuantity(request.getShippingQuantity());
        orderShipping.setShippingWeight(request.getShippingWeight());
        orderShipping.setShippingDimensionRegularOrNot(request.getShippingDimensionRegularOrNot());
        orderShipping.setShippingDimensionHeight(request.getShippingDimensionHeight());
        orderShipping.setShippingDimensionWidth(request.getShippingDimensionWidth());
        orderShipping.setShippingDimensionDepth(request.getShippingDimensionDepth());

        orderShippingRepository.save(orderShipping);

        // Audit log
        auditLogService.logAction(
                orderShipping.getOrder().getBuyer().getEmailAddress(),
                "SHIPPING_DETAILS_UPDATED",
                authenticatedUser,
                "Shipping details updated for order ID: " + orderId
        );

        // Send email notification
        try {
            emailService.sendEmail(
                    orderShipping.getOrder().getBuyer().getEmailAddress(),
                    "Shipping Details Updated",
                    String.format(
                            "<h2>Shipping Details Updated</h2>" +
                                    "<p>Shipping details for order ID %d have been updated successfully.</p>" +
                                    "<p>Carrier: %s</p><p>Method: %s</p><p>Price: %s %s</p>",
                            orderId, request.getShippingCarrier(), request.getShippingMethod(),
                            request.getShippingPrice(), request.getShippingMethodCurrency()
                    )
            );
            log.info("Sent shipping details update email to {} for order {}", orderShipping.getOrder().getBuyer().getEmailAddress(), orderId);
        } catch (Exception e) {
            log.error("Failed to send shipping details update email to {}: {}", orderShipping.getOrder().getBuyer().getEmailAddress(), e.getMessage());
        }

        return orderShippingMapper.toResponse(orderShipping);
    }
}