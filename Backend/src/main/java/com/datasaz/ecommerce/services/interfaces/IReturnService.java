package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.request.ReturnRequestRequest;
import com.datasaz.ecommerce.models.response.ReturnRequestResponse;

import java.util.List;

public interface IReturnService {
    ReturnRequestResponse createReturnRequest(Long orderId, ReturnRequestRequest request);

    ReturnRequestResponse approveReturnRequest(Long returnRequestId);

    ReturnRequestResponse rejectReturnRequest(Long returnRequestId, String rejectionReason);

    List<ReturnRequestResponse> getReturnRequestsByOrder(Long orderId);
}