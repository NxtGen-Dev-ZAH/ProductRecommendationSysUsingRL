package com.datasaz.ecommerce.mappers;

import com.datasaz.ecommerce.exceptions.ResourceNotFoundException;
import com.datasaz.ecommerce.models.request.PaymentRequest;
import com.datasaz.ecommerce.models.response.PaymentResponse;
import com.datasaz.ecommerce.repositories.OrderRepository;
import com.datasaz.ecommerce.repositories.PaymentMethodRepository;
import com.datasaz.ecommerce.repositories.entities.Order;
import com.datasaz.ecommerce.repositories.entities.Payment;
import com.datasaz.ecommerce.repositories.entities.PaymentMethod;
import com.datasaz.ecommerce.repositories.entities.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMapper {
    private final OrderRepository orderRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    public Payment toEntity(PaymentRequest request) {
        log.info("Mapping PaymentRequest to Payment for order ID: {}", request.getOrderId());

        // Fetch Order
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> {
                    log.error("Order not found for ID: {}", request.getOrderId());
                    return ResourceNotFoundException.builder().message("Order not found.").build();
                });

        // Fetch PaymentMethod if paymentMethodToken is provided
        PaymentMethod paymentMethod = null;
        if (request.getPaymentMethodToken() != null) {
            paymentMethod = paymentMethodRepository.findByTokenAndUserId(request.getPaymentMethodToken(), order.getBuyer().getId())
                    .orElseThrow(() -> {
                        log.error("Payment method not found for token: {} and user ID: {}",
                                request.getPaymentMethodToken(), order.getBuyer().getId());
                        return ResourceNotFoundException.builder().message("Payment method not found.").build();
                    });
        }

        return Payment.builder()
                .paymentDate(LocalDateTime.now())
                .amount(request.getAmount())
                .status(PaymentStatus.PENDING)
                .transactionId(null) // Will be set during payment processing
                .method(paymentMethod)
                .order(order)
                .build();
    }

    public PaymentResponse toResponse(Payment entity, String paymentUrl) {
        log.info("Mapping Payment to PaymentResponse");
        if (entity == null) {
            return null;
        }
        return PaymentResponse.builder()
                .id(entity.getId())
                .paymentDate(entity.getPaymentDate())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .transactionId(entity.getTransactionId())
                .method(entity.getMethod() != null ? entity.getMethod().getMethod() : null)
                .orderId(entity.getOrder() != null ? entity.getOrder().getId() : null)
                .paymentUrl(paymentUrl)
                .build();
    }
}