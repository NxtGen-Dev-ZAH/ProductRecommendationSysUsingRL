package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.request.PaymentRequest;
import com.datasaz.ecommerce.models.response.PaymentResponse;
import com.datasaz.ecommerce.repositories.entities.Order;

public interface IPayPalService {

    PaymentResponse createPaymentSession(PaymentRequest request);

    Order finalizeOrder(String paymentId);
}
