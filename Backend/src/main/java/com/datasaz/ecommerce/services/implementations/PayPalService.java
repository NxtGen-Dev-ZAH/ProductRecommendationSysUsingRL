package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.mappers.PaymentMapper;
import com.datasaz.ecommerce.models.request.PaymentRequest;
import com.datasaz.ecommerce.models.response.PaymentResponse;
import com.datasaz.ecommerce.repositories.entities.Order;
import com.datasaz.ecommerce.repositories.entities.PaymentMethods;
import com.datasaz.ecommerce.repositories.entities.PaymentStatus;
import com.datasaz.ecommerce.services.interfaces.IPayPalService;
import com.datasaz.ecommerce.utilities.ApplicationContextHolder;
import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayPalService implements IPayPalService {
    @Value("${paypal.clientId}")
    private String clientId;

    @Value("${paypal.clientSecret}")
    private String clientSecret;

    @Value("${paypal.mode}")
    private String mode;

    @Value("${payment.successUrl}")
    private String successUrl;
    @Value("${payment.cancelUrl}")
    private String cancelUrl;

    private final PaymentMapper paymentMapper;

    @Override
    public PaymentResponse createPaymentSession(PaymentRequest request) {
        log.info("Creating PayPal payment session for order ID: {}", request.getOrderId());
        APIContext apiContext = new APIContext(clientId, clientSecret, mode);

        try {
            Amount amount = new Amount();
            amount.setCurrency(request.getCurrency());
            amount.setTotal(request.getAmount().toString());

            Transaction transaction = new Transaction();
            transaction.setAmount(amount);
            transaction.setDescription("Order #" + request.getOrderId());

            List<Transaction> transactions = new ArrayList<>();
            transactions.add(transaction);

            Payer payer = new Payer();
            payer.setPaymentMethod("paypal");

            Payment payment = new Payment();
            payment.setIntent("sale");
            payment.setPayer(payer);
            payment.setTransactions(transactions);

            RedirectUrls redirectUrls = new RedirectUrls();
            redirectUrls.setReturnUrl(successUrl + "?order_id=" + request.getOrderId());
            redirectUrls.setCancelUrl(cancelUrl + "?order_id=" + request.getOrderId());
            payment.setRedirectUrls(redirectUrls);

            Payment createdPayment = payment.create(apiContext);
            String approvalUrl = createdPayment.getLinks().stream()
                    .filter(link -> link.getRel().equalsIgnoreCase("approval_url"))
                    .findFirst()
                    .map(Links::getHref)
                    .orElseThrow(() -> new RuntimeException("No approval URL found."));

            return PaymentResponse.builder()
                    .paymentDate(LocalDateTime.now())
                    .amount(request.getAmount())
                    .status(PaymentStatus.PENDING)
                    .method(PaymentMethods.PAYPAL)
                    .orderId(request.getOrderId())
                    .transactionId(createdPayment.getId())
                    .paymentUrl(approvalUrl)
                    .build();
        } catch (PayPalRESTException e) {
            log.error("PayPal error: {}", e.getMessage());
            throw new RuntimeException("PayPal error: " + e.getMessage());
        }
    }

    @Override
    public Order finalizeOrder(String paymentId) {
        log.info("Finalizing PayPal payment for payment ID: {}", paymentId);
        APIContext apiContext = new APIContext(clientId, clientSecret, mode);

        try {
            Payment payment = Payment.get(apiContext, paymentId);
            if (!payment.getState().equalsIgnoreCase("approved")) {
                log.error("Payment not approved for payment ID: {}", paymentId);
                throw BadRequestException.builder().message("Payment not approved.").build();
            }

            Long orderId = Long.parseLong(payment.getTransactions().get(0).getDescription().split("#")[1]);
            PaymentRequest request = PaymentRequest.builder()
                    .orderId(orderId)
                    .amount(new BigDecimal(payment.getTransactions().get(0).getAmount().getTotal()))
                    .currency(payment.getTransactions().get(0).getAmount().getCurrency())
                    .method(PaymentMethods.PAYPAL)
                    .build();

            return savePaymentAndUpdateOrder(request, paymentId, PaymentStatus.COMPLETED);
        } catch (PayPalRESTException e) {
            log.error("PayPal error: {}", e.getMessage());
            throw new RuntimeException("PayPal error: " + e.getMessage());
        }
    }

    private Order savePaymentAndUpdateOrder(PaymentRequest request, String transactionId, PaymentStatus status) {
        // Delegate to PaymentService to avoid circular dependency
        PaymentService paymentService = ApplicationContextHolder.getContext().getBean(PaymentService.class);
        return paymentService.savePaymentAndUpdateOrder(request, transactionId, status);
    }
}