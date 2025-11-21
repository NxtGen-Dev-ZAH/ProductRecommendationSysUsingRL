package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.response.CartResponse;
import com.datasaz.ecommerce.repositories.entities.Cart;

public interface IBuyerCartService extends ICartService {


    CartResponse mergeCartOnLogin(String sessionId, Long userId);

    void updateCartTotals(Cart cart);

/*    CartResponse addToCartForUser(CartRequest cartRequest);

    CartResponse updateCartItemForUser(Long cartItemId, int quantity);

    CartResponse removeFromCartForUser(Long cartItemId);

    CartResponse clearCartForUser();

    CartResponse getCartByUsersId(Long userId);

    CartResponse getCartForUser(Pageable pageable);
    CartResponse mergeCartOnLogin(String sessionId);

    AppliedCouponResponse applyCouponForUser(String couponIdentifier);

    CartResponse removeCouponForUser();
    BigDecimal calculateTotalAmount(Long userId);
    BigDecimal calculateDiscount(Long userId);

//
//    BigDecimal calculateTotalAmount(Long userId);
//
//    BigDecimal calculateDiscount(Long userId);
//
//    CartResponse getCartByUsersId(Long userId);*/
}