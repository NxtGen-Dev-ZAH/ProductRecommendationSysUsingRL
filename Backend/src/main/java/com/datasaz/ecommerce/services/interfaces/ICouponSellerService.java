package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.request.CouponRequest;
import com.datasaz.ecommerce.models.response.CouponResponse;

import java.util.List;

public interface ICouponSellerService {

    CouponResponse createCoupon(CouponRequest request);

    CouponResponse updateCoupon(Long couponId, CouponRequest request);

    void deleteCoupon(Long couponId);

    List<CouponResponse> getSellerCoupons();

}
