/*package com.datasaz.ecommerce.controller;

import com.datasaz.ecommerce.Request.CouponRequest;
import com.datasaz.ecommerce.service.CouponService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CouponController {

    @Autowired
    private CouponService couponService;

    /**
     * Appliquer un coupon
     * @param request Objet avec le code coupon
     * @return Montant de la réduction
     */
 /*   @PostMapping("/apply-coupon")
    public ResponseEntity<?> appliquerCoupon(@RequestBody CouponRequest request) {
        try {
            double discount = couponService.appliquerCoupon(request.getCouponCode());
            return ResponseEntity.ok().body("{\"discount\": " + discount + "}");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("{\"message\": \"" + e.getMessage() + "\"}");
        }
    }
}
*/

