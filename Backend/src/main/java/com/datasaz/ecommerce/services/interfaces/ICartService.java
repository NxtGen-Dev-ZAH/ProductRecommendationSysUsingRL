package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.request.CartItemRequest;
import com.datasaz.ecommerce.models.response.AppliedCouponResponse;
import com.datasaz.ecommerce.models.response.CartResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ICartService {

/*
    CartResponse addToCart(String sessionId, CartRequest cartRequest);
    CartResponse updateCartItem(String sessionId, Long cartItemId, int quantity);
    CartResponse removeFromCart(String sessionId, Long cartItemId);

    CartResponse getCart(String sessionId, Pageable pageable);

    CartResponse clearCart(String sessionId);

    BigDecimal calculateTotalAmount(String sessionId);
    BigDecimal calculateDiscount(String sessionId);

    AppliedCouponResponse applyCoupon(String sessionId, String couponIdentifier);

    CartResponse removeCoupon(String sessionId);

//    CartResponse applyGiftCard(String sessionId, String giftCardIdentifier);
//    CartResponse removeGiftCard(String sessionId);
*/

    CartResponse addToCart(String identifier, CartItemRequest cartItemRequest);

    CartResponse updateCartItem(String identifier, Long cartItemId, int quantity);

    CartResponse removeFromCart(String identifier, Long cartItemId);

    CartResponse getCart(String identifier, Pageable pageable);

    CartResponse clearCart(String identifier);

    AppliedCouponResponse applyCoupon(String identifier, String couponIdentifier);

    CartResponse removeCoupon(String identifier);

    BigDecimal calculateSubtotalPrice(String identifier);

    BigDecimal calculateTotalShippingCost(String identifier);
    BigDecimal calculateDiscount(String identifier);

    BigDecimal calculateTotalAmount(String identifier);



}
