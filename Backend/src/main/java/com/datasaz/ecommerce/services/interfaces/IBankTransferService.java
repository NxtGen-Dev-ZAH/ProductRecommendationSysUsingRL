package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.request.PaymentRequest;
import com.datasaz.ecommerce.models.response.PaymentResponse;

public interface IBankTransferService {
    PaymentResponse processBankTransfer(PaymentRequest request);
}
