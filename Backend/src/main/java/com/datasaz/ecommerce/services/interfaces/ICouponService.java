package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.repositories.entities.*;

import java.math.BigDecimal;
import java.util.List;

public interface ICouponService {

    //void applyCoupon(String codeCoupon);
    Coupon validateCoupon(String code, User user, BigDecimal orderTotal,
                          List<CartItem> cartItems);

    void trackCouponUsage(Coupon coupon, User user, Order order,
                          Product product, Category category);

    // used in updateCartTotals in BuyerCartService
    BigDecimal calculateDiscount(Coupon coupon, List<CartItem> cartItems,
                                 BigDecimal subtotal);

    //    boolean isCouponValidForUser(Coupon coupon, Users user);
    //    boolean isCouponValidForProducts(Coupon coupon, List<CartItem> cartItems);
    //    boolean isCouponValidForCategories(Coupon coupon, List<CartItem> cartItems);
    //    boolean isCouponValidForCompany(Coupon coupon, UserCompany company);
    boolean isItemEligibleForCoupon(Coupon coupon, CartItem item);

    Coupon validateCouponWithLock(String code, User user, BigDecimal orderTotal,
                                  List<CartItem> cartItems, Company company);

    //del: replaced by calculateDiscount method above
    BigDecimal applyCoupon(String couponIdentifier, String sessionId);

}
