package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.StripePaymentRequest;
import com.datasaz.ecommerce.models.StripePaymentResponse;
import com.datasaz.ecommerce.repositories.entities.Order;
import com.stripe.exception.StripeException;

public interface IStripeServiceV1 {

    StripePaymentResponse createPaymentSession(StripePaymentRequest request);

    Order finalizeOrder(String sessionId) throws StripeException;

    //StripePaymentResponse createPaymentSession(PaymentRequest request);
    //Order finalizeOrder(String sessionId);
}
