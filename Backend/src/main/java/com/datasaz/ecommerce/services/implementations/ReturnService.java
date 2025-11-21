package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.ResourceNotFoundException;
import com.datasaz.ecommerce.exceptions.UnauthorizedException;
import com.datasaz.ecommerce.models.request.ReturnRequestRequest;
import com.datasaz.ecommerce.models.response.ReturnRequestResponse;
import com.datasaz.ecommerce.repositories.OrderItemRepository;
import com.datasaz.ecommerce.repositories.OrderRepository;
import com.datasaz.ecommerce.repositories.ReturnRequestRepository;
import com.datasaz.ecommerce.repositories.entities.*;
import com.datasaz.ecommerce.services.interfaces.IAuditLogService;
import com.datasaz.ecommerce.services.interfaces.IEmailService;
import com.datasaz.ecommerce.services.interfaces.IReturnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnService implements IReturnService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final IAuditLogService auditLogService;
    private final IEmailService emailService;

    @Override
    @Transactional
    public ReturnRequestResponse createReturnRequest(Long orderId, ReturnRequestRequest request) {
        log.info("Creating return request for order ID: {}", orderId);

        // Validate order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("Order not found for ID: {}", orderId);
                    return ResourceNotFoundException.builder().message("Order not found.").build();
                });

        // Validate user ownership
        String authenticatedUser = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!order.getBuyer().getEmailAddress().equals(authenticatedUser)) {
            log.error("User {} not authorized to create return for order {}", authenticatedUser, orderId);
            throw UnauthorizedException.builder().message("Not authorized to create return for this order.").build();
        }

        // Validate order status
        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            log.error("Order {} is not delivered, cannot create return request", orderId);
            throw BadRequestException.builder().message("Order must be delivered to request a return.").build();
        }

        // Validate order items
        List<OrderItem> items = orderItemRepository.findByIdIn(request.getOrderItemIds());
        if (items.isEmpty() || items.size() != request.getOrderItemIds().size()) {
            log.error("Invalid order items for return request: {}", request.getOrderItemIds());
            throw BadRequestException.builder().message("Invalid order items for return.").build();
        }

        // Additional validation: Ensure order items belong to the order
        boolean allItemsValid = items.stream().allMatch(item -> item.getOrder().getId().equals(orderId));
        if (!allItemsValid) {
            log.error("Some order items do not belong to order ID: {}", orderId);
            throw BadRequestException.builder().message("Some order items do not belong to this order.").build();
        }

        // Calculate refund amount
        BigDecimal totalItemAmount = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Apply refund percentage (default to 1.0 for full refund if not specified)
        BigDecimal refundPercentage = request.getRefundPercentage() != null ? request.getRefundPercentage() : BigDecimal.ONE;
        if (refundPercentage.compareTo(BigDecimal.ZERO) < 0 || refundPercentage.compareTo(BigDecimal.ONE) > 0) {
            log.error("Invalid refund percentage: {}", refundPercentage);
            throw BadRequestException.builder().message("Refund percentage must be between 0 and 1.").build();
        }
        BigDecimal refundAmount = totalItemAmount.multiply(refundPercentage);

        ReturnRequest returnRequest = ReturnRequest.builder()
                .reason(request.getReason())
                .status(ReturnStatus.PENDING)
                .requestDate(LocalDateTime.now())
                .refundAmount(refundAmount)
                .refundPercentage(refundPercentage)
                .order(order)
                .items(items)
                .build();

        returnRequestRepository.save(returnRequest);

        // Update order status
        order.setOrderStatus(OrderStatus.RETURN_REQUESTED);
        orderRepository.save(order);

        // Audit log
        auditLogService.logAction(
                order.getBuyer().getEmailAddress(),
                "RETURN_REQUEST_CREATED",
                authenticatedUser,
                "Return request created for order ID: " + orderId + " with refund percentage: " + refundPercentage
        );

        // Send email notification
        try {
            emailService.sendReturnRequestEmail(
                    order.getBuyer().getEmailAddress(),
                    orderId,
                    returnRequest.getId(),
                    request.getReason()
            );
            log.info("Sent return request email to {} for order {}", order.getBuyer().getEmailAddress(), orderId);
        } catch (Exception e) {
            log.error("Failed to send return request email to {}: {}", order.getBuyer().getEmailAddress(), e.getMessage());
        }

        return ReturnRequestResponse.builder()
                .id(returnRequest.getId())
                .reason(returnRequest.getReason())
                .status(returnRequest.getStatus())
                .requestDate(returnRequest.getRequestDate())
                .refundAmount(returnRequest.getRefundAmount())
                .orderId(orderId)
                .orderItemIds(items.stream().map(OrderItem::getId).collect(Collectors.toList()))
                .build();
    }
//    @Override
//    @Transactional
//    public ReturnRequestResponse createReturnRequest(Long orderId, ReturnRequestRequest request) {
//        log.info("Creating return request for order ID: {}", orderId);
//
//        // Validate order
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> {
//                    log.error("Order not found for ID: {}", orderId);
//                    return ResourceNotFoundException.builder().message("Order not found.").build();
//                });
//
//        // Validate user ownership
//        String authenticatedUser = SecurityContextHolder.getContext().getAuthentication().getName();
//        if (!order.getBuyer().getEmailAddress().equals(authenticatedUser)) {
//            log.error("User {} not authorized to create return for order {}", authenticatedUser, orderId);
//            throw UnauthorizedException.builder().message("Not authorized to create return for this order.").build();
//        }
//
//        // Validate order status
//        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
//            log.error("Order {} is not delivered, cannot create return request", orderId);
//            throw BadRequestException.builder().message("Order must be delivered to request a return.").build();
//        }
//
//        // Validate order items
//        List<OrderItem> items = orderItemRepository.findByIdIn(request.getOrderItemIds());
//        if (items.isEmpty() || items.size() != request.getOrderItemIds().size()) {
//            log.error("Invalid order items for return request: {}", request.getOrderItemIds());
//            throw BadRequestException.builder().message("Invalid order items for return.").build();
//        }
//
//        // Additional validation: Ensure order items belong to the order
//        boolean allItemsValid = items.stream().allMatch(item -> item.getOrder().getId().equals(orderId));
//        if (!allItemsValid) {
//            log.error("Some order items do not belong to order ID: {}", orderId);
//            throw BadRequestException.builder().message("Some order items do not belong to this order.").build();
//        }
//
//        // Calculate refund amount
//        BigDecimal refundAmount = items.stream()
//                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        ReturnRequest returnRequest = ReturnRequest.builder()
//                .reason(request.getReason())
//                .status(ReturnStatus.PENDING)
//                .requestDate(LocalDateTime.now())
//                .refundAmount(refundAmount)
//                .order(order)
//                .items(items)
//                .build();
//
//        returnRequestRepository.save(returnRequest);
//
//        // Update order status
//        order.setOrderStatus(OrderStatus.RETURN_REQUESTED);
//        orderRepository.save(order);
//
//        // Audit log
//        auditLogService.logAction(
//                order.getBuyer().getEmailAddress(),
//                "RETURN_REQUEST_CREATED",
//                authenticatedUser,
//                "Return request created for order ID: " + orderId
//        );
//
//        // Send email notification
//        try {
//            emailService.sendReturnRequestEmail(
//                    order.getBuyer().getEmailAddress(),
//                    orderId,
//                    returnRequest.getId(),
//                    request.getReason()
//            );
//            log.info("Sent return request email to {} for order {}", order.getBuyer().getEmailAddress(), orderId);
//        } catch (Exception e) {
//            log.error("Failed to send return request email to {}: {}", order.getBuyer().getEmailAddress(), e.getMessage());
//        }
//
//        return ReturnRequestResponse.builder()
//                .id(returnRequest.getId())
//                .reason(returnRequest.getReason())
//                .status(returnRequest.getStatus())
//                .requestDate(returnRequest.getRequestDate())
//                .refundAmount(returnRequest.getRefundAmount())
//                .orderId(orderId)
//                .orderItemIds(items.stream().map(OrderItem::getId).collect(Collectors.toList()))
//                .build();
//    }

//    @Override
//    @Transactional
//    public ReturnRequestResponse createReturnRequest(Long orderId, ReturnRequestRequest request) {
//        log.info("Creating return request for order ID: {}", orderId);
//
//        // Validate order
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> {
//                    log.error("Order not found for ID: {}", orderId);
//                    return ResourceNotFoundException.builder().message("Order not found.").build();
//                });
//
//        // Validate user ownership
//        String authenticatedUser = SecurityContextHolder.getContext().getAuthentication().getName();
//        if (!order.getBuyer().getEmailAddress().equals(authenticatedUser)) {
//            log.error("User {} not authorized to create return for order {}", authenticatedUser, orderId);
//            throw new AccessDeniedException("Not authorized to create return for this order.");
//        }
//
//        // Validate order status
//        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
//            log.error("Order {} is not delivered, cannot create return request", orderId);
//            throw BadRequestException.builder().message("Order must be delivered to request a return.").build();
//        }
//
//        // Validate order items
//        List<OrderItem> items = orderItemRepository.findByIdIn(request.getOrderItemIds());
//        if (items.isEmpty() || items.size() != request.getOrderItemIds().size()) {
//            log.error("Invalid order items for return request: {}", request.getOrderItemIds());
//            throw BadRequestException.builder().message("Invalid order items for return.").build();
//        }
//
//        // Calculate refund amount
//        BigDecimal refundAmount = items.stream()
//                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        ReturnRequest returnRequest = ReturnRequest.builder()
//                .reason(request.getReason())
//                .status(ReturnStatus.PENDING)
//                .requestDate(LocalDateTime.now())
//                .refundAmount(refundAmount)
//                .order(order)
//                .items(items)
//                .build();
//
//        returnRequestRepository.save(returnRequest);
//
//        // Update order status
//        order.setOrderStatus(OrderStatus.RETURN_REQUESTED);
//        orderRepository.save(order);
//
//        // Audit log
//        auditLogService.logAction(
//                order.getBuyer().getEmailAddress(),
//                "RETURN_REQUEST_CREATED",
//                authenticatedUser,
//                "Return request created for order ID: " + orderId
//        );
//
//        // Send email notification
//        try {
//            emailService.sendReturnRequestEmail(
//                    order.getBuyer().getEmailAddress(),
//                    orderId,
//                    returnRequest.getId(),
//                    request.getReason()
//            );
//            log.info("Sent return request email to {} for order {}", order.getBuyer().getEmailAddress(), orderId);
//        } catch (Exception e) {
//            log.error("Failed to send return request email to {}: {}", order.getBuyer().getEmailAddress(), e.getMessage());
//        }
//
//        return ReturnRequestResponse.builder()
//                .id(returnRequest.getId())
//                .reason(returnRequest.getReason())
//                .status(returnRequest.getStatus())
//                .requestDate(returnRequest.getRequestDate())
//                .refundAmount(returnRequest.getRefundAmount())
//                .orderId(orderId)
//                .orderItemIds(items.stream().map(OrderItem::getId).collect(Collectors.toList()))
//                .build();
//    }

    @Override
    @Transactional
    public ReturnRequestResponse approveReturnRequest(Long returnRequestId) {
        log.info("Approving return request ID: {}", returnRequestId);

        ReturnRequest returnRequest = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> {
                    log.error("Return request not found for ID: {}", returnRequestId);
                    return ResourceNotFoundException.builder().message("Return request not found.").build();
                });

        returnRequest.setStatus(ReturnStatus.APPROVED);
        returnRequestRepository.save(returnRequest);

        Order order = returnRequest.getOrder();
        order.setOrderStatus(OrderStatus.RETURN_APPROVED);
        orderRepository.save(order);

        // Audit log
        String authenticatedUser = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.logAction(
                order.getBuyer().getEmailAddress(),
                "RETURN_REQUEST_APPROVED",
                authenticatedUser,
                "Return request approved for ID: " + returnRequestId
        );

        // Send email notification
        try {
            emailService.sendReturnStatusEmail(
                    order.getBuyer().getEmailAddress(),
                    order.getId(),
                    returnRequestId,
                    ReturnStatus.APPROVED
            );
            log.info("Sent return approval email to {} for order {}", order.getBuyer().getEmailAddress(), order.getId());
        } catch (Exception e) {
            log.error("Failed to send return approval email to {}: {}", order.getBuyer().getEmailAddress(), e.getMessage());
        }

        return ReturnRequestResponse.builder()
                .id(returnRequest.getId())
                .reason(returnRequest.getReason())
                .status(returnRequest.getStatus())
                .refundAmount(returnRequest.getRefundAmount())
                .orderId(order.getId())
                .orderItemIds(returnRequest.getItems().stream().map(OrderItem::getId).collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional
    public ReturnRequestResponse rejectReturnRequest(Long returnRequestId, String rejectionReason) {
        log.info("Rejecting return request ID: {}", returnRequestId);

        ReturnRequest returnRequest = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> {
                    log.error("Return request not found for ID: {}", returnRequestId);
                    return ResourceNotFoundException.builder().message("Return request not found.").build();
                });

        returnRequest.setStatus(ReturnStatus.REJECTED);
        returnRequest.setReason(returnRequest.getReason() + " [Rejection: " + rejectionReason + "]");
        returnRequestRepository.save(returnRequest);

        Order order = returnRequest.getOrder();
        order.setOrderStatus(OrderStatus.RETURN_REJECTED);
        orderRepository.save(order);

        // Audit log
        String authenticatedUser = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.logAction(
                order.getBuyer().getEmailAddress(),
                "RETURN_REQUEST_REJECTED",
                authenticatedUser,
                "Return request rejected for ID: " + returnRequestId + " with reason: " + rejectionReason
        );

        // Send email notification
        try {
            emailService.sendReturnStatusEmail(
                    order.getBuyer().getEmailAddress(),
                    order.getId(),
                    returnRequestId,
                    ReturnStatus.REJECTED
            );
            log.info("Sent return rejection email to {} for order {}", order.getBuyer().getEmailAddress(), order.getId());
        } catch (Exception e) {
            log.error("Failed to send return rejection email to {}: {}", order.getBuyer().getEmailAddress(), e.getMessage());
        }

        return ReturnRequestResponse.builder()
                .id(returnRequest.getId())
                .reason(returnRequest.getReason())
                .status(returnRequest.getStatus())
                .requestDate(returnRequest.getRequestDate())
                .refundAmount(returnRequest.getRefundAmount())
                .orderId(order.getId())
                .orderItemIds(returnRequest.getItems().stream().map(OrderItem::getId).collect(Collectors.toList()))
                .build();
    }

    @Override
    public List<ReturnRequestResponse> getReturnRequestsByOrder(Long orderId) {
        log.info("Retrieving return requests for order ID: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Order not found.").build());

        String authenticatedUser = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!order.getBuyer().getEmailAddress().equals(authenticatedUser)) {
            log.error("User {} not authorized to view return requests for order {}", authenticatedUser, orderId);
            throw UnauthorizedException.builder().message("Not authorized to view return requests for this order.").build();
        }

        List<ReturnRequest> returnRequests = returnRequestRepository.findByOrderId(orderId);
        return returnRequests.stream()
                .map(request -> ReturnRequestResponse.builder()
                        .id(request.getId())
                        .reason(request.getReason())
                        .status(request.getStatus())
                        .requestDate(request.getRequestDate())
                        .refundAmount(request.getRefundAmount())
                        .orderId(orderId)
                        .orderItemIds(request.getItems().stream().map(OrderItem::getId).collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }
//    @Override
//    @Transactional
//    public ReturnRequestResponse approveReturnRequest(Long returnRequestId) {
//        log.info("Approving return request ID: {}", returnRequestId);
//
//        ReturnRequest returnRequest = returnRequestRepository.findById(returnRequestId)
//                .orElseThrow(() -> {
//                    log.error("Return request not found for ID: {}", returnRequestId);
//                    return ResourceNotFoundException.builder().message("Return request not found.").build();
//                });
//
//        returnRequest.setStatus(ReturnStatus.APPROVED);
//        returnRequestRepository.save(returnRequest);
//
//        Order order = returnRequest.getOrder();
//        order.setOrderStatus(OrderStatus.RETURN_APPROVED);
//        orderRepository.save(order);
//
//        // Audit log
//        String authenticatedUser = SecurityContextHolder.getContext().getAuthentication().getName();
//        auditLogService.logAction(
//                order.getBuyer().getEmailAddress(),
//                "RETURN_REQUEST_APPROVED",
//                authenticatedUser,
//                "Return request approved for ID: " + returnRequestId
//        );
//
//        // Send email notification
//        try {
//            emailService.sendReturnStatusEmail(
//                    order.getBuyer().getEmailAddress(),
//                    order.getId(),
//                    returnRequestId,
//                    ReturnStatus.APPROVED
//            );
//            log.info("Sent return approval email to {} for order {}", order.getBuyer().getEmailAddress(), order.getId());
//        } catch (Exception e) {
//            log.error("Failed to send return approval email to {}: {}", order.getBuyer().getEmailAddress(), e.getMessage());
//        }
//
//        return ReturnRequestResponse.builder()
//                .id(returnRequest.getId())
//                .reason(returnRequest.getReason())
//                .status(returnRequest.getStatus())
//                .requestDate(returnRequest.getRequestDate())
//                .refundAmount(returnRequest.getRefundAmount())
//                .orderId(order.getId())
//                .orderItemIds(returnRequest.getItems().stream().map(OrderItem::getId).collect(Collectors.toList()))
//                .build();
//    }
//
//
//    @Override
//    @Transactional
//    public ReturnRequestResponse rejectReturnRequest(Long returnRequestId, String rejectionReason) {
//        log.info("Rejecting return request ID: {}", returnRequestId);
//
//        ReturnRequest returnRequest = returnRequestRepository.findById(returnRequestId)
//                .orElseThrow(() -> {
//                    log.error("Return request not found for ID: {}", returnRequestId);
//                    return ResourceNotFoundException.builder().message("Return request not found.").build();
//                });
//
//        returnRequest.setStatus(ReturnStatus.REJECTED);
//        returnRequest.setReason(returnRequest.getReason() + " [Rejection: " + rejectionReason + "]");
//        returnRequestRepository.save(returnRequest);
//
//        Order order = returnRequest.getOrder();
//        order.setOrderStatus(OrderStatus.RETURN_REJECTED);
//        orderRepository.save(order);
//
//        // Audit log
//        String authenticatedUser = SecurityContextHolder.getContext().getAuthentication().getName();
//        auditLogService.logAction(
//                order.getBuyer().getEmailAddress(),
//                "RETURN_REQUEST_REJECTED",
//                authenticatedUser,
//                "Return request rejected for ID: " + returnRequestId + " with reason: " + rejectionReason
//        );
//
//        // Send email notification
//        try {
//            emailService.sendReturnStatusEmail(
//                    order.getBuyer().getEmailAddress(),
//                    order.getId(),
//                    returnRequestId,
//                    ReturnStatus.REJECTED
//            );
//            log.info("Sent return rejection email to {} for order {}", order.getBuyer().getEmailAddress(), order.getId());
//        } catch (Exception e) {
//            log.error("Failed to send return rejection email to {}: {}", order.getBuyer().getEmailAddress(), e.getMessage());
//        }
//
//        return ReturnRequestResponse.builder()
//                .id(returnRequest.getId())
//                .reason(returnRequest.getReason())
//                .status(returnRequest.getStatus())
//                .requestDate(returnRequest.getRequestDate())
//                .refundAmount(returnRequest.getRefundAmount())
//                .orderId(order.getId())
//                .orderItemIds(returnRequest.getItems().stream().map(OrderItem::getId).collect(Collectors.toList()))
//                .build();
//    }
//
//    @Override
//    public List<ReturnRequestResponse> getReturnRequestsByOrder(Long orderId) {
//        log.info("Retrieving return requests for order ID: {}", orderId);
//
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> ResourceNotFoundException.builder().message("Order not found.").build());
//
//        String authenticatedUser = SecurityContextHolder.getContext().getAuthentication().getName();
//        if (!order.getBuyer().getEmailAddress().equals(authenticatedUser)) {
//            log.error("User {} not authorized to view return requests for order {}", authenticatedUser, orderId);
//            throw UnauthorizedException.builder().message("Not authorized to view return requests for this order.").build();
//        }
//
//        List<ReturnRequest> returnRequests = returnRequestRepository.findByOrderId(orderId);
//        return returnRequests.stream()
//                .map(request -> ReturnRequestResponse.builder()
//                        .id(request.getId())
//                        .reason(request.getReason())
//                        .status(request.getStatus())
//                        .requestDate(request.getRequestDate())
//                        .refundAmount(request.getRefundAmount())
//                        .orderId(orderId)
//                        .orderItemIds(request.getItems().stream().map(OrderItem::getId).collect(Collectors.toList()))
//                        .build())
//                .collect(Collectors.toList());
//    }

}