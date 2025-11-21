package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.mappers.PaymentMapper;
import com.datasaz.ecommerce.models.request.PaymentRequest;
import com.datasaz.ecommerce.models.response.PaymentResponse;
import com.datasaz.ecommerce.repositories.entities.Order;
import com.datasaz.ecommerce.repositories.entities.PaymentMethods;
import com.datasaz.ecommerce.repositories.entities.PaymentStatus;
import com.datasaz.ecommerce.services.interfaces.IBankTransferService;
import com.datasaz.ecommerce.utilities.ApplicationContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankTransferService implements IBankTransferService {
    private final PaymentMapper paymentMapper;

    @Override
    public PaymentResponse processBankTransfer(PaymentRequest request) {
        log.info("Processing bank transfer for order ID: {}", request.getOrderId());
        String transactionId = UUID.randomUUID().toString();

        // Bank transfer payments are marked as PENDING until manually verified
        PaymentService paymentService = ApplicationContextHolder.getContext().getBean(PaymentService.class);
        Order order = paymentService.savePaymentAndUpdateOrder(request, transactionId, PaymentStatus.PENDING);

        return PaymentResponse.builder()
                .paymentDate(LocalDateTime.now())
                .amount(request.getAmount())
                .status(PaymentStatus.PENDING)
                .method(PaymentMethods.BANK_TRANSFER)
                .orderId(request.getOrderId())
                .transactionId(transactionId)
                .paymentUrl(null)
                .build();
    }
}