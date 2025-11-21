package com.datasaz.ecommerce.models.request;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrderCheckoutRequest {

    //private String cartSessionId;

    // dealt via service
    //@NotNull(message = "Buyer ID is required")
    //private Long buyerId;

    private List<Long> selectedCartItemIds;

    private String usedCouponId;

    /*
    @NotNull(message = "Payment method is required")
    private String preferredPaymentMethod; // e.g., "STRIPE", "PAYPAL", "BANK_TRANSFER"

    private PaymentRequest paymentDetails;
*/
    //private Payment payment;

    private OrderShippingRequest shippingDetails;
    private Long shippingAddressId;
    private Long billingAddressId;

//    private OrderStatus orderStatus;
//    private LocalDateTime orderDateTime;
//
//    private Long buyerCompanyId;
//
//    private BigDecimal discountAmount;
//    private BigDecimal totalVAT;
//    private BigDecimal totalAmount;
}

