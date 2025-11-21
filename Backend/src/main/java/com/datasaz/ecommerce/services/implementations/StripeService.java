package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.ResourceNotFoundException;
import com.datasaz.ecommerce.models.StripePaymentResponse;
import com.datasaz.ecommerce.models.request.PaymentRequest;
import com.datasaz.ecommerce.repositories.entities.Order;
import com.datasaz.ecommerce.services.interfaces.IOrderService;
import com.datasaz.ecommerce.services.interfaces.IStripeService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService implements IStripeService {
    @Value("${stripe.apiKey}")
    private String apiKey;
    @Value("${payment.successUrl}")
    private String successUrl;
    @Value("${payment.cancelUrl}")
    private String cancelUrl;

    private final IOrderService orderService;

    @Override
    public StripePaymentResponse createPaymentSession(PaymentRequest request) {
        log.info("Creating Stripe payment session for order ID: {}", request.getOrderId());
        Stripe.apiKey = apiKey;

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(request.getCurrency())
                                                    .setUnitAmount(request.getAmount().multiply(BigDecimal.valueOf(100))
                                                            .longValue())
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Order #" + request.getOrderId())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .putMetadata("order_id", String.valueOf(request.getOrderId()))
                    .build();

            Session session = Session.create(params);
            return StripePaymentResponse.builder()
                    .sessionId(session.getId())
                    .paymentUrl(session.getUrl())
                    .build();
        } catch (StripeException e) {
            log.error("Stripe error: {}", e.getMessage());
            throw new RuntimeException("Stripe error: " + e.getMessage());
        }
    }

    @Override
    public Order finalizeOrder(String sessionId) {
        log.info("Finalizing Stripe payment for session ID: {}", sessionId);
        Stripe.apiKey = apiKey;

        try {
            Session session = Session.retrieve(sessionId);
            if (!session.getPaymentStatus().equalsIgnoreCase("paid")) {
                log.error("Payment not completed for session ID: {}", sessionId);
                throw BadRequestException.builder().message("Payment not completed.").build();
            }

            Long orderId = Long.parseLong(session.getMetadata().get("order_id"));
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                log.error("Order not found for ID: {}", orderId);
                throw ResourceNotFoundException.builder().message("Order not found.").build();
            }

            return orderService.saveOrder(order);
        } catch (StripeException e) {
            log.error("Stripe error: {}", e.getMessage());
            throw new RuntimeException("Stripe error: " + e.getMessage());
        }
    }
}