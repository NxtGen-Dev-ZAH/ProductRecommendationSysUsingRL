package com.datasaz.ecommerce.controllers.seller;

import com.datasaz.ecommerce.models.request.CouponRequest;
import com.datasaz.ecommerce.models.response.CouponResponse;
import com.datasaz.ecommerce.services.interfaces.ICouponSellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/seller/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class CouponSellerController {

    private final ICouponSellerService couponSellerService;

    @PostMapping
    public ResponseEntity<CouponResponse> createCoupon(@Valid @RequestBody CouponRequest request) {
        CouponResponse response = couponSellerService.createCoupon(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{couponId}")
    public ResponseEntity<CouponResponse> updateCoupon(@PathVariable Long couponId,
                                                       @Valid @RequestBody CouponRequest request) {
        CouponResponse response = couponSellerService.updateCoupon(couponId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{couponId}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long couponId) {
        couponSellerService.deleteCoupon(couponId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<CouponResponse>> getSellerCoupons() {
        List<CouponResponse> coupons = couponSellerService.getSellerCoupons();
        return ResponseEntity.ok(coupons);
    }
}
