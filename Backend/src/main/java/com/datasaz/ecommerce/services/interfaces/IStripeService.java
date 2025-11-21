package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.StripePaymentResponse;
import com.datasaz.ecommerce.models.request.PaymentRequest;
import com.datasaz.ecommerce.repositories.entities.Order;

public interface IStripeService {

    //StripePaymentResponse createPaymentSession(StripePaymentRequest request);

    //Order finalizeOrder(String sessionId) throws StripeException;

    StripePaymentResponse createPaymentSession(PaymentRequest request);

    Order finalizeOrder(String sessionId);
}
