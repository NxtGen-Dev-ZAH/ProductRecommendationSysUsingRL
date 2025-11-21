package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.request.PaymentRequest;
import com.datasaz.ecommerce.models.response.PaymentResponse;
import com.datasaz.ecommerce.models.response.RefundResponse;
import com.datasaz.ecommerce.repositories.entities.Order;
import com.datasaz.ecommerce.repositories.entities.PaymentStatus;

public interface IPaymentService {
    PaymentResponse processPayment(PaymentRequest request);

    PaymentResponse finalizePayment(String transactionId, String paymentMethod);

    Order savePaymentAndUpdateOrder(PaymentRequest request, String transactionId, PaymentStatus status);

    RefundResponse processPartialRefund(Long returnRequestId);
}
