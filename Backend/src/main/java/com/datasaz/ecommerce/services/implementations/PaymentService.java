package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.ResourceNotFoundException;
import com.datasaz.ecommerce.mappers.PaymentMapper;
import com.datasaz.ecommerce.models.StripePaymentResponse;
import com.datasaz.ecommerce.models.request.PaymentRequest;
import com.datasaz.ecommerce.models.response.PaymentResponse;
import com.datasaz.ecommerce.models.response.RefundResponse;
import com.datasaz.ecommerce.repositories.*;
import com.datasaz.ecommerce.repositories.entities.*;
import com.datasaz.ecommerce.services.interfaces.*;
import com.paypal.api.payments.Amount;
import com.paypal.api.payments.Sale;
import com.paypal.base.rest.APIContext;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.datasaz.ecommerce.configs.GroupConfig.CAT1_VAT_RATE;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService implements IPaymentService {
    private final OrderRepository orderRepository;
    private final OrderShippingRepository orderShippingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final IStripeService stripeService;
    private final IPayPalService payPalService;
    private final IBankTransferService bankTransferService;
    private final IAuditLogService auditLogService;
    private final IEmailService emailService;
    private final IPdfGenerator pdfGenerator;

    private final OrderRefundRepository orderRefundRepository;
    private final ReturnRequestRepository returnRequestRepository;

    @Value("${paypal.clientId}")
    private String paypalClientId;

    @Value("${paypal.clientSecret}")
    private String paypalClientSecret;

    @Value("${paypal.mode}")
    private String paypalMode;

    private static final BigDecimal VAT_RATE = CAT1_VAT_RATE;

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment for order ID: {}", request.getOrderId());

        // Validate order
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> {
                    log.error("Order not found for ID: {}", request.getOrderId());
                    return ResourceNotFoundException.builder().message("Order not found.").build();
                });

        // Validate user ownership
        String authenticatedUser = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!order.getBuyer().getEmailAddress().equals(authenticatedUser)) {
            log.error("User {} not authorized to process payment for order {}", authenticatedUser, request.getOrderId());
            throw new AccessDeniedException("Not authorized to process payment for this order.");
        }

        // Validate shipping details
        OrderShipping orderShipping = orderShippingRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> {
                    log.error("Shipping details not found for order ID: {}", request.getOrderId());
                    return ResourceNotFoundException.builder().message("Shipping details not found.").build();
                });

        // Calculate final amount
        BigDecimal shippingCost = new BigDecimal(orderShipping.getShippingPrice());
        BigDecimal subtotal = order.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal taxableAmount = subtotal.subtract(discount);
        BigDecimal totalVAT = taxableAmount.multiply(VAT_RATE);
        BigDecimal finalAmount = taxableAmount.add(totalVAT).add(shippingCost);

        if (!finalAmount.equals(request.getAmount())) {
            log.error("Requested amount {} does not match calculated amount {} for order ID: {}",
                    request.getAmount(), finalAmount, request.getOrderId());
            throw BadRequestException.builder().message("Requested payment amount does not match calculated amount.").build();
        }

        // Save initial payment record
        Payment payment = paymentMapper.toEntity(request);
        paymentRepository.save(payment);

        // Process payment based on method
        PaymentResponse response;
        switch (request.getMethod()) {
            case STRIPE:
                StripePaymentResponse stripeResponse = stripeService.createPaymentSession(request);
                response = paymentMapper.toResponse(payment, stripeResponse.getPaymentUrl());
                break;
            case PAYPAL:
                response = payPalService.createPaymentSession(request);
                break;
            case BANK_TRANSFER:
                response = bankTransferService.processBankTransfer(request);
                break;
            default:
                log.error("Unsupported payment method: {}", request.getMethod());
                throw BadRequestException.builder().message("Unsupported payment method.").build();
        }

        // Audit log
        auditLogService.logAction(
                order.getBuyer().getEmailAddress(),
                "PAYMENT_INITIATED",
                authenticatedUser,
                "Payment initiated for order ID: " + request.getOrderId() + " with method: " + request.getMethod()
        );

        return response;
    }

    @Override
    @Transactional
    public PaymentResponse finalizePayment(String transactionId, String paymentMethod) {
        log.info("Finalizing payment with transaction ID: {} and method: {}", transactionId, paymentMethod);
        PaymentMethods method;
        try {
            method = PaymentMethods.valueOf(paymentMethod.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid payment method: {}", paymentMethod);
            throw BadRequestException.builder().message("Invalid payment method: " + paymentMethod).build();
        }

        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> {
                    log.error("Payment not found for transaction ID: {}", transactionId);
                    return ResourceNotFoundException.builder().message("Payment not found.").build();
                });

        Order order;
        switch (method) {
            case STRIPE:
                order = stripeService.finalizeOrder(transactionId);
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setTransactionId(transactionId);
                break;
            case PAYPAL:
                order = payPalService.finalizeOrder(transactionId);
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setTransactionId(transactionId);
                break;
            case BANK_TRANSFER:
                order = payment.getOrder();
                payment.setStatus(PaymentStatus.PENDING); // Bank transfers remain PENDING until verified
                break;
            default:
                log.error("Unsupported payment method for finalization: {}", paymentMethod);
                throw BadRequestException.builder().message("Unsupported payment method.").build();
        }

        // Update order status
        order.setOrderStatus(PaymentStatus.COMPLETED.equals(payment.getStatus()) ? OrderStatus.PAID : OrderStatus.PAYMENT_FAILED);
        orderRepository.save(order);

        // Generate and link invoice
        OrderShipping orderShipping = orderShippingRepository.findByOrderId(order.getId())
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Shipping details not found.").build());
        BigDecimal shippingCost = new BigDecimal(orderShipping.getShippingPrice());
        BigDecimal subtotal = order.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal taxableAmount = subtotal.subtract(discount);
        BigDecimal totalVAT = taxableAmount.multiply(VAT_RATE);
        BigDecimal totalAmount = taxableAmount.add(totalVAT).add(shippingCost);

        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-" + System.currentTimeMillis())
                .issuedAt(LocalDateTime.now())
                .subtotal(subtotal)
                .discountAmount(discount)
                .shippingCost(shippingCost)
                .totalVAT(totalVAT)
                .totalAmount(totalAmount)
                .order(order)
                .build();

        payment.setInvoice(invoice);
        paymentRepository.save(payment);

        // Audit log
        auditLogService.logAction(
                order.getBuyer().getEmailAddress(),
                "PAYMENT_FINALIZED",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                "Payment finalized for order ID: " + order.getId() + " with transaction ID: " + transactionId
        );

        // Send email with invoice
        try {
            File invoiceFile = new File("invoice_" + order.getId() + ".pdf");
            pdfGenerator.generatePdf(invoice);
            emailService.sendInvoiceEmail(order.getBuyer().getEmailAddress(), invoiceFile);
            log.info("Sent invoice email to {} for order {}", order.getBuyer().getEmailAddress(), order.getId());
        } catch (Exception e) {
            log.error("Failed to send invoice email to {}: {}", order.getBuyer().getEmailAddress(), e.getMessage());
        }

        return paymentMapper.toResponse(payment, null);
    }

    @Override
    @Transactional
    public Order savePaymentAndUpdateOrder(PaymentRequest request, String transactionId, PaymentStatus status) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Order not found.").build());

        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseGet(() -> paymentMapper.toEntity(request));
        payment.setTransactionId(transactionId);
        payment.setStatus(status);
        paymentRepository.save(payment);

        order.setOrderStatus(status == PaymentStatus.COMPLETED ? OrderStatus.PAID : OrderStatus.PAYMENT_FAILED);
        orderRepository.save(order);

        // Audit log
        auditLogService.logAction(
                order.getBuyer().getEmailAddress(),
                "PAYMENT_SAVED",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                "Payment saved for order ID: " + order.getId() + " with status: " + status
        );

        return order;
    }

    @Transactional
    public RefundResponse processPartialRefund(Long returnRequestId) {
        log.info("Processing partial refund for return request ID: {}", returnRequestId);

        ReturnRequest returnRequest = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> {
                    log.error("Return request not found for ID: {}", returnRequestId);
                    return ResourceNotFoundException.builder().message("Return request not found.").build();
                });

        if (returnRequest.getStatus() != ReturnStatus.APPROVED) {
            log.error("Return request {} is not approved for refund", returnRequestId);
            throw BadRequestException.builder().message("Return request must be approved for refund.").build();
        }

        Payment payment = paymentRepository.findByOrderId(returnRequest.getOrder().getId())
                .orElseThrow(() -> {
                    log.error("Payment not found for order ID: {}", returnRequest.getOrder().getId());
                    return ResourceNotFoundException.builder().message("Payment not found.").build();
                });

        String transactionId = null;
        BigDecimal refundAmount = returnRequest.getRefundAmount(); // Already includes percentage
        PaymentMethods paymentMethod = payment.getMethod().getMethod();

        switch (paymentMethod) {
            case STRIPE:
                try {
                    RefundCreateParams params = RefundCreateParams.builder()
                            .setPaymentIntent(payment.getTransactionId())
                            .setAmount(refundAmount.multiply(BigDecimal.valueOf(100)).longValue()) // Convert to cents
                            .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                            .build();
                    Refund stripeRefund = Refund.create(params);
                    transactionId = stripeRefund.getId();
                } catch (StripeException e) {
                    log.error("Stripe refund error: {}", e.getMessage());
                    throw new RuntimeException("Stripe refund error: " + e.getMessage());
                }
                break;
            case PAYPAL:
                try {
                    APIContext apiContext = new APIContext(paypalClientId, paypalClientSecret, paypalMode);
                    com.paypal.api.payments.RefundRequest refundRequest = new com.paypal.api.payments.RefundRequest();
                    Amount amount = new Amount();
                    amount.setCurrency(payment.getOrder().getOrderShipping().getShippingMethodCurrency());
                    amount.setTotal(refundAmount.toString());
                    refundRequest.setAmount(amount);

                    Sale sale = Sale.get(apiContext, payment.getTransactionId());
                    com.paypal.api.payments.Refund refund = sale.refund(apiContext, refundRequest);
                    transactionId = refund.getId();
                } catch (com.paypal.base.rest.PayPalRESTException e) {
                    log.error("PayPal refund error: {}", e.getMessage());
                    throw new RuntimeException("PayPal refund error: " + e.getMessage());
                }
                break;
            case BANK_TRANSFER:
                transactionId = "MANUAL_REFUND_" + System.currentTimeMillis();
                break;
            default:
                log.error("Unsupported payment method for refund: {}", paymentMethod);
                throw BadRequestException.builder().message("Unsupported payment method for refund.").build();
        }

        OrderRefund orderRefund = OrderRefund.builder()
                .refundDate(LocalDateTime.now())
                .amount(refundAmount)
                .transactionId(transactionId)
                .reason("Return request ID: " + returnRequestId)
                .payment(payment)
                .returnRequest(returnRequest)
                .build();
        orderRefundRepository.save(orderRefund);

        // Update payment status
        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        // Update order status
        Order order = returnRequest.getOrder();
        order.setOrderStatus(OrderStatus.RETURN_COMPLETED);
        orderRepository.save(order);

        // Update return request status
        returnRequest.setStatus(ReturnStatus.COMPLETED);
        returnRequestRepository.save(returnRequest);

        // Audit log
        String authenticatedUser = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.logAction(
                order.getBuyer().getEmailAddress(),
                "REFUND_PROCESSED",
                authenticatedUser,
                "Refund processed for return request ID: " + returnRequestId + " with amount: " + refundAmount
        );

        // Send email notification
        try {
            File refundDocument = new File("refund_" + orderRefund.getId() + ".pdf");
            pdfGenerator.generateRefundDocument(orderRefund);
            emailService.sendRefundEmail(order.getBuyer().getEmailAddress(), refundDocument, refundAmount);
            log.info("Sent refund email to {} for return request {}", order.getBuyer().getEmailAddress(), returnRequestId);
        } catch (Exception e) {
            log.error("Failed to send refund email to {}: {}", order.getBuyer().getEmailAddress(), e.getMessage());
        }

        return RefundResponse.builder()
                .id(orderRefund.getId())
                .refundDate(orderRefund.getRefundDate())
                .amount(orderRefund.getAmount())
                .transactionId(orderRefund.getTransactionId())
                .reason(orderRefund.getReason())
                .paymentId(payment.getId())
                .returnRequestId(returnRequestId)
                .build();
    }
//    @Transactional
//    public RefundResponse processPartialRefund(Long returnRequestId) {
//        log.info("Processing partial refund for return request ID: {}", returnRequestId);
//
//        ReturnRequest returnRequest = returnRequestRepository.findById(returnRequestId)
//                .orElseThrow(() -> {
//                    log.error("Return request not found for ID: {}", returnRequestId);
//                    return ResourceNotFoundException.builder().message("Return request not found.").build();
//                });
//
//        if (returnRequest.getStatus() != ReturnStatus.APPROVED) {
//            log.error("Return request {} is not approved for refund", returnRequestId);
//            throw BadRequestException.builder().message("Return request must be approved for refund.").build();
//        }
//
//        Payment payment = paymentRepository.findByOrderId(returnRequest.getOrder().getId())
//                .orElseThrow(() -> {
//                    log.error("Payment not found for order ID: {}", returnRequest.getOrder().getId());
//                    return ResourceNotFoundException.builder().message("Payment not found.").build();
//                });
//
//        String transactionId = null;
//        BigDecimal refundAmount = returnRequest.getRefundAmount();
//        PaymentMethods paymentMethod = payment.getMethod().getMethod();
//
//        switch (paymentMethod) {
//            case STRIPE:
//                try {
//                    RefundCreateParams params = RefundCreateParams.builder()
//                            .setPaymentIntent(payment.getTransactionId())
//                            .setAmount(refundAmount.multiply(BigDecimal.valueOf(100)).longValue()) // Convert to cents
//                            .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
//                            .build();
//                    Refund stripeRefund = Refund.create(params);
//                    transactionId = stripeRefund.getId();
//                } catch (StripeException e) {
//                    log.error("Stripe refund error: {}", e.getMessage());
//                    throw BadRequestException.builder().message("Stripe refund error: " + e.getMessage()).build();
//                }
//                break;
//            case PAYPAL:
//                try {
//                    APIContext apiContext = new APIContext(paypalClientId, paypalClientSecret, paypalMode);
//                    com.paypal.api.payments.RefundRequest refundRequest = new com.paypal.api.payments.RefundRequest();
//                    Amount amount = new Amount();
//                    amount.setCurrency(payment.getOrder().getOrderShipping().getShippingMethodCurrency());
//                    amount.setTotal(refundAmount.toString());
//                    refundRequest.setAmount(amount);
//
//                    Sale sale = Sale.get(apiContext, payment.getTransactionId());
//                    com.paypal.api.payments.Refund refund = sale.refund(apiContext, refundRequest);
//                    transactionId = refund.getId();
//                } catch (com.paypal.base.rest.PayPalRESTException e) {
//                    log.error("PayPal refund error: {}", e.getMessage());
//                    throw BadRequestException.builder().message("PayPal refund error: " + e.getMessage()).build();
//                }
//                break;
//            case BANK_TRANSFER:
//                // Bank transfer refunds require manual processing
//                transactionId = "MANUAL_REFUND_" + System.currentTimeMillis();
//                break;
//            default:
//                log.error("Unsupported payment method for refund: {}", paymentMethod);
//                throw BadRequestException.builder().message("Unsupported payment method for refund.").build();
//        }
//
//        OrderRefund orderRefund = OrderRefund.builder()
//                .refundDate(LocalDateTime.now())
//                .amount(refundAmount)
//                .transactionId(transactionId)
//                .reason("Return request ID: " + returnRequestId)
//                .payment(payment)
//                .returnRequest(returnRequest)
//                .build();
//        orderRefundRepository.save(orderRefund);
//
//        // Update payment status
//        payment.setStatus(PaymentStatus.REFUNDED);
//        paymentRepository.save(payment);
//
//        // Update order status
//        Order order = returnRequest.getOrder();
//        order.setOrderStatus(OrderStatus.RETURN_COMPLETED);
//        orderRepository.save(order);
//
//        // Update return request status
//        returnRequest.setStatus(ReturnStatus.COMPLETED);
//        returnRequestRepository.save(returnRequest);
//
//        // Audit log
//        String authenticatedUser = SecurityContextHolder.getContext().getAuthentication().getName();
//        auditLogService.logAction(
//                order.getBuyer().getEmailAddress(),
//                "REFUND_PROCESSED",
//                authenticatedUser,
//                "Refund processed for return request ID: " + returnRequestId + " with amount: " + refundAmount
//        );
//
//        // Send email notification
//        try {
//            File refundDocument = new File("refund_" + orderRefund.getId() + ".pdf");
//            pdfGenerator.generateRefundDocument(orderRefund);
//            emailService.sendRefundEmail(order.getBuyer().getEmailAddress(), refundDocument, refundAmount);
//            log.info("Sent refund email to {} for return request {}", order.getBuyer().getEmailAddress(), returnRequestId);
//        } catch (Exception e) {
//            log.error("Failed to send refund email to {}: {}", order.getBuyer().getEmailAddress(), e.getMessage());
//        }
//
//        return RefundResponse.builder()
//                .id(orderRefund.getId())
//                .refundDate(orderRefund.getRefundDate())
//                .amount(orderRefund.getAmount())
//                .transactionId(orderRefund.getTransactionId())
//                .reason(orderRefund.getReason())
//                .paymentId(payment.getId())
//                .returnRequestId(returnRequestId)
//                .build();
//    }

}