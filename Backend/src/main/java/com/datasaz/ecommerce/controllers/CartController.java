package com.datasaz.ecommerce.controllers;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.models.request.CartItemRequest;
import com.datasaz.ecommerce.models.request.CouponCodeRequest;
import com.datasaz.ecommerce.models.response.AppliedCouponResponse;
import com.datasaz.ecommerce.models.response.CartResponse;
import com.datasaz.ecommerce.services.interfaces.ICartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final ICartService cartService;

    @Operation(summary = "Get cart by session ID", description = "Retrieves a paginated cart by session ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid cart session ID"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String effectiveSessionId = sessionId != null ? sessionId : cookieSessionId;
        log.info("GET /api/cart for sessionId: {}, page: {}, size: {}", effectiveSessionId, page, size);
        if (effectiveSessionId == null || effectiveSessionId.isEmpty()) {
            log.error("Missing or invalid cart_session_id");
            throw BadRequestException.builder().message("Missing or invalid cart session ID").build();
        }
        Pageable pageable = PageRequest.of(page, size);
        CartResponse cartResponse = cartService.getCart(effectiveSessionId, pageable);
        return ResponseEntity.ok(cartResponse);
    }

    @Operation(summary = "Add item to cart", description = "Adds an item to the cart, creating a new session ID if none exists")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item added to cart successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid cart request or session ID"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId,
            @Valid @RequestBody CartItemRequest cartItemRequest,
            HttpServletResponse response) {
        String effectiveSessionId = sessionId != null ? sessionId : cookieSessionId;
        log.info("POST /api/cart/add for sessionId: {}", effectiveSessionId);
        if (effectiveSessionId == null) {
            effectiveSessionId = UUID.randomUUID().toString();
            Cookie cookie = new Cookie("cart_session_id", effectiveSessionId);
            cookie.setPath("/");
            cookie.setMaxAge(12 * 30 * 24 * 60 * 60); // 1 year
            response.addCookie(cookie);
        }
        CartResponse cartResponse = cartService.addToCart(effectiveSessionId, cartItemRequest);
        return ResponseEntity.ok(cartResponse);
    }

    @Operation(summary = "Update cart item quantity", description = "Updates the quantity of a cart item")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart item updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid session ID or quantity"),
            @ApiResponse(responseCode = "404", description = "Cart or cart item not found")
    })
    @PutMapping("/update/{cartItemId}")
    public ResponseEntity<CartResponse> updateCartItem(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId,
            @PathVariable Long cartItemId,
            @RequestParam int quantity) {
        String effectiveSessionId = sessionId != null ? sessionId : cookieSessionId;
        log.info("PUT /api/cart/update/{} with quantity {} for sessionId: {}", cartItemId, quantity, effectiveSessionId);
        if (effectiveSessionId == null || effectiveSessionId.isEmpty()) {
            log.error("Missing or invalid cart_session_id");
            throw BadRequestException.builder().message("Missing or invalid cart session ID").build();
        }
        CartResponse cartResponse = cartService.updateCartItem(effectiveSessionId, cartItemId, quantity);
        return ResponseEntity.ok(cartResponse);
    }

    @Operation(summary = "Remove item from cart", description = "Removes an item from the cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item removed from cart successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid session ID"),
            @ApiResponse(responseCode = "404", description = "Cart or cart item not found")
    })
    @DeleteMapping("/remove/{cartItemId}")
    public ResponseEntity<CartResponse> removeFromCart(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId,
            @PathVariable Long cartItemId) {
        String effectiveSessionId = sessionId != null ? sessionId : cookieSessionId;
        log.info("DELETE /api/cart/remove/{} for sessionId: {}", cartItemId, effectiveSessionId);
        if (effectiveSessionId == null || effectiveSessionId.isEmpty()) {
            log.error("Missing or invalid cart_session_id");
            throw BadRequestException.builder().message("Missing or invalid cart session ID").build();
        }
        CartResponse cartResponse = cartService.removeFromCart(effectiveSessionId, cartItemId);
        return ResponseEntity.ok(cartResponse);
    }

    @Operation(summary = "Clear cart", description = "Clears all items from the cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart cleared successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid session ID"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @DeleteMapping("/clear")
    public ResponseEntity<CartResponse> clearCart(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId) {
        String effectiveSessionId = sessionId != null ? sessionId : cookieSessionId;
        log.info("DELETE /api/cart/clear for sessionId: {}", effectiveSessionId);
        if (effectiveSessionId == null || effectiveSessionId.isEmpty()) {
            log.error("Missing or invalid cart_session_id");
            throw BadRequestException.builder().message("Missing or invalid cart session ID").build();
        }
        CartResponse cartResponse = cartService.clearCart(effectiveSessionId);
        return ResponseEntity.ok(cartResponse);
    }

    @Operation(summary = "Apply coupon to cart", description = "Applies a coupon to the cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coupon applied successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid session ID or coupon code"),
            @ApiResponse(responseCode = "404", description = "Cart or coupon not found"),
            @ApiResponse(responseCode = "422", description = "Coupon is invalid, expired, or usage limit exceeded")
    })
    @PostMapping("/coupon/apply")
    public ResponseEntity<AppliedCouponResponse> applyCoupon(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId,
            @Valid @RequestBody CouponCodeRequest couponCodeRequest) {
        String effectiveSessionId = sessionId != null ? sessionId : cookieSessionId;
        log.info("POST /api/cart/coupon/apply for sessionId: {}", effectiveSessionId);
        if (effectiveSessionId == null || effectiveSessionId.isEmpty()) {
            log.error("Missing or invalid cart_session_id");
            throw BadRequestException.builder().message("Missing or invalid cart session ID").build();
        }
        AppliedCouponResponse appliedCouponResponse = cartService.applyCoupon(effectiveSessionId, couponCodeRequest.getCode());
        return ResponseEntity.ok(appliedCouponResponse);
    }

    @Operation(summary = "Remove coupon from cart", description = "Removes a coupon from the cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coupon removed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid session ID"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @DeleteMapping("/coupon/remove")
    public ResponseEntity<CartResponse> removeCoupon(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId) {
        String effectiveSessionId = sessionId != null ? sessionId : cookieSessionId;
        log.info("DELETE /api/cart/coupon/remove for sessionId: {}", effectiveSessionId);
        if (effectiveSessionId == null || effectiveSessionId.isEmpty()) {
            log.error("Missing or invalid cart_session_id");
            throw BadRequestException.builder().message("Missing or invalid cart session ID").build();
        }
        CartResponse cartResponse = cartService.removeCoupon(effectiveSessionId);
        return ResponseEntity.ok(cartResponse);
    }

    @Operation(summary = "Get cart subtotal", description = "Returns the subtotal price (items only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subtotal calculated"),
            @ApiResponse(responseCode = "400", description = "Missing session ID"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @GetMapping("/subtotal")
    public ResponseEntity<BigDecimal> calculateSubtotalPrice(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId) {
        log.info("GET /api/cart/subtotal for annonymous user");
        String effectiveSessionId = getEffectiveSessionId(sessionId, cookieSessionId);
        BigDecimal subtotal = cartService.calculateSubtotalPrice(effectiveSessionId);
        return ResponseEntity.ok(subtotal);
    }

    @Operation(summary = "Get cart shipping cost", description = "Returns total shipping cost")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipping calculated"),
            @ApiResponse(responseCode = "400", description = "Missing session ID"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @GetMapping("/shipping")
    public ResponseEntity<BigDecimal> calculateTotalShippingCost(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId) {
        log.info("GET /api/cart/shipping for annonymous user");
        String effectiveSessionId = getEffectiveSessionId(sessionId, cookieSessionId);
        BigDecimal shipping = cartService.calculateTotalShippingCost(effectiveSessionId);
        return ResponseEntity.ok(shipping);
    }

    @Operation(summary = "Get cart discount", description = "Returns applied discount (from coupon)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Discount calculated"),
            @ApiResponse(responseCode = "400", description = "Missing session ID"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @GetMapping("/discount")
    public ResponseEntity<BigDecimal> calculateDiscount(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId) {
        log.info("GET /api/cart/discount for annonymous user");
        String effectiveSessionId = getEffectiveSessionId(sessionId, cookieSessionId);
        BigDecimal discount = cartService.calculateDiscount(effectiveSessionId);
        return ResponseEntity.ok(discount);
    }

    @Operation(summary = "Get cart total", description = "Returns final total (subtotal + shipping - discount)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Total calculated"),
            @ApiResponse(responseCode = "400", description = "Missing session ID"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @GetMapping("/total")
    public ResponseEntity<BigDecimal> calculateTotalAmount(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId) {
        log.info("GET /api/cart/total for annonymous user");
        String effectiveSessionId = getEffectiveSessionId(sessionId, cookieSessionId);
        BigDecimal total = cartService.calculateTotalAmount(effectiveSessionId);
        return ResponseEntity.ok(total);
    }

    // === HELPER METHOD ===
    private String getEffectiveSessionId(String sessionId, String cookieSessionId) {
        String effective = sessionId != null ? sessionId : cookieSessionId;
        if (effective == null || effective.isEmpty()) {
            log.error("Missing cart_session_id");
            throw BadRequestException.builder().message("Missing or invalid cart session ID").build();
        }
        return effective;
    }

}

/*

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.models.request.CartRequest;
import com.datasaz.ecommerce.models.request.CouponCodeRequest;
import com.datasaz.ecommerce.models.response.AppliedCouponResponse;
import com.datasaz.ecommerce.models.response.CartResponse;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.services.interfaces.IAuthService;
import com.datasaz.ecommerce.services.interfaces.ICartService;
import com.datasaz.ecommerce.services.interfaces.ICouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final ICartService cartService;
    private final ICouponService couponService;
    private final IAuthService authService;
    private final UserRepository userRepository;

    @Operation(summary = "Get cart by session ID", description = "Retrieves a paginated cart by session ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid cart session ID"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String effectiveSessionId = sessionId != null ? sessionId : cookieSessionId;
        log.info("GET /api/cart for sessionId: {}, page: {}, size: {}", effectiveSessionId, page, size);
        if (effectiveSessionId == null || effectiveSessionId.isEmpty()) {
            log.error("Missing or invalid cart_session_id");
            throw BadRequestException.builder().message("Missing or invalid cart session ID").build();
        }
        Pageable pageable = PageRequest.of(page, size);
        CartResponse cartResponse = cartService.getCart(effectiveSessionId, pageable);
        return ResponseEntity.ok(cartResponse);
    }

    @Operation(summary = "Add item to cart", description = "Adds an item to the cart, creating a new session ID if none exists")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item added to cart successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid cart request or session ID"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId,
            @Valid @RequestBody CartRequest cartRequest,
            HttpServletResponse response) {
        String effectiveSessionId = sessionId != null ? sessionId : cookieSessionId;
        log.info("POST /api/cart/add for sessionId: {}", effectiveSessionId);
        if (effectiveSessionId == null) {
            effectiveSessionId = UUID.randomUUID().toString();
            Cookie cookie = new Cookie("cart_session_id", effectiveSessionId);
            cookie.setPath("/");
            cookie.setMaxAge(12 * 30 * 24 * 60 * 60); // 1 year
            // TODO:// should be refreshed on getCart
            //  -> cartRepository.deleteBySessionIdNotNullAndLastModifiedBefore(LocalDateTime.now().minusDays(365));
            response.addCookie(cookie);
        }
        CartResponse cartResponse = cartService.addToCart(effectiveSessionId, cartRequest);
        return ResponseEntity.ok(cartResponse);
    }

    @Operation(summary = "Update cart item quantity", description = "Updates the quantity of a cart item")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart item updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid session ID or quantity"),
            @ApiResponse(responseCode = "404", description = "Cart or cart item not found")
    })
    @PutMapping("/update/{cartItemId}")
    public ResponseEntity<CartResponse> updateCartItem(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId,
            @PathVariable Long cartItemId,
            @RequestParam int quantity) {
        String effectiveSessionId = sessionId != null ? sessionId : cookieSessionId;
        log.info("PUT /api/cart/update/{} with quantity {} for sessionId: {}", cartItemId, quantity, effectiveSessionId);
        if (effectiveSessionId == null || effectiveSessionId.isEmpty()) {
            log.error("Missing or invalid cart_session_id");
            throw BadRequestException.builder().message("Missing or invalid cart session ID").build();
        }
        CartResponse cartResponse = cartService.updateCartItem(effectiveSessionId, cartItemId, quantity);
        return ResponseEntity.ok(cartResponse);
    }

    @Operation(summary = "Remove item from cart", description = "Removes an item from the cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item removed from cart successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid session ID"),
            @ApiResponse(responseCode = "404", description = "Cart or cart item not found")
    })
    @DeleteMapping("/remove/{cartItemId}")
    public ResponseEntity<CartResponse> removeFromCart(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId,
            @PathVariable Long cartItemId) {
        String effectiveSessionId = sessionId != null ? sessionId : cookieSessionId;
        log.info("DELETE /api/cart/remove/{} for sessionId: {}", cartItemId, effectiveSessionId);
        if (effectiveSessionId == null || effectiveSessionId.isEmpty()) {
            log.error("Missing or invalid cart_session_id");
            throw BadRequestException.builder().message("Missing or invalid cart session ID").build();
        }
        CartResponse cartResponse = cartService.removeFromCart(effectiveSessionId, cartItemId);
        return ResponseEntity.ok(cartResponse);
    }

    @Operation(summary = "Clear cart", description = "Clears all items from the cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart cleared successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid session ID"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @DeleteMapping("/clear")
    public ResponseEntity<CartResponse> clearCart(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId) {
        String effectiveSessionId = sessionId != null ? sessionId : cookieSessionId;
        log.info("DELETE /api/cart/clear for sessionId: {}", effectiveSessionId);
        if (effectiveSessionId == null || effectiveSessionId.isEmpty()) {
            log.error("Missing or invalid cart_session_id");
            throw BadRequestException.builder().message("Missing or invalid cart session ID").build();
        }
        CartResponse cartResponse = cartService.clearCart(effectiveSessionId);
        return ResponseEntity.ok(cartResponse);
    }

    @Operation(summary = "Apply coupon to cart", description = "Applies a coupon to the cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coupon applied successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid session ID or coupon code"),
            @ApiResponse(responseCode = "404", description = "Cart or coupon not found"),
            @ApiResponse(responseCode = "422", description = "Coupon is invalid, expired, or usage limit exceeded")
    })
    @PostMapping("/coupon/apply")
    public ResponseEntity<AppliedCouponResponse> applyCoupon(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId,
            @RequestBody CouponCodeRequest couponCodeRequest) {
        String effectiveSessionId = sessionId != null ? sessionId : cookieSessionId;
        log.info("POST /api/cart/coupon/apply for sessionId: {}", effectiveSessionId);
        if (effectiveSessionId == null || effectiveSessionId.isEmpty()) {
            log.error("Missing or invalid cart_session_id");
            throw BadRequestException.builder().message("Missing or invalid cart session ID").build();
        }
        AppliedCouponResponse appliedCouponResponse = cartService.applyCoupon(effectiveSessionId, couponCodeRequest.getCode());
        return ResponseEntity.ok(appliedCouponResponse);
    }

    @Operation(summary = "Remove coupon from cart", description = "Removes a coupon from the cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coupon removed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid session ID"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @DeleteMapping("/coupon/remove")
    public ResponseEntity<CartResponse> removeCoupon(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId) {
        String effectiveSessionId = sessionId != null ? sessionId : cookieSessionId;
        log.info("DELETE /api/cart/coupon/remove for sessionId: {}", effectiveSessionId);
        if (effectiveSessionId == null || effectiveSessionId.isEmpty()) {
            log.error("Missing or invalid cart_session_id");
            throw BadRequestException.builder().message("Missing or invalid cart session ID").build();
        }
        CartResponse cartResponse = cartService.removeCoupon(effectiveSessionId);
        return ResponseEntity.ok(cartResponse);
    }

//    @Operation(summary = "Get cart total for authenticated user", description = "Retrieves total and discount for authenticated user's cart")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Cart total retrieved successfully"),
//            @ApiResponse(responseCode = "401", description = "User not authenticated"),
//            @ApiResponse(responseCode = "404", description = "User or cart not found")
//    })
//    @GetMapping("/total")
//    public ResponseEntity<CartTotalResponse> getCartTotal() {
//        log.info("GET /api/cart/total for authenticated user");
//        String email = SecurityContextHolder.getContext().getAuthentication().getName();
//        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
//                .orElseThrow(() -> {
//                    log.error("User not found with email: {}", email);
//                    return UserNotFoundException.builder()
//                            .message("User not found with email: " + email)
//                            .build();
//                });
//        BigDecimal total = cartService.calculateTotalAmount(user.getId());
//        BigDecimal discount = cartService.calculateDiscount(user.getId());
//        return ResponseEntity.ok(CartTotalResponse.builder()
//                .total(total)
//                .discount(discount)
//                .build());
//    }
}
*/


//package com.datasaz.ecommerce.controllers;
//
//import com.datasaz.ecommerce.exceptions.BadRequestException;
//import com.datasaz.ecommerce.exceptions.UserNotFoundException;
//import com.datasaz.ecommerce.models.request.CartRequest;
//import com.datasaz.ecommerce.models.request.CouponRequest;
//import com.datasaz.ecommerce.models.response.AppliedCouponResponse;
//import com.datasaz.ecommerce.models.response.CartResponse;
//import com.datasaz.ecommerce.models.response.CartTotalResponse;
//import com.datasaz.ecommerce.repositories.UserRepository;
//import com.datasaz.ecommerce.repositories.entities.User;
//import com.datasaz.ecommerce.services.interfaces.IAuthService;
//import com.datasaz.ecommerce.services.interfaces.ICartService;
//import com.datasaz.ecommerce.services.interfaces.ICouponService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.responses.ApiResponse;
//import io.swagger.v3.oas.annotations.responses.ApiResponses;
//import jakarta.servlet.http.Cookie;
//import jakarta.servlet.http.HttpServletResponse;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.web.bind.annotation.*;
//
//import java.math.BigDecimal;
//import java.util.UUID;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/cart")
//@RequiredArgsConstructor
//public class CartController {
//
//    private final ICartService cartService;
//    private final ICouponService couponService;
//    private final IAuthService authService;
//    private final UserRepository userRepository;
//
//
//    @Operation(summary = "Get cart by session ID", description = "Retrieves a paginated cart by session ID")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Cart retrieved successfully"),
//            @ApiResponse(responseCode = "400", description = "Missing or invalid cart session ID"),
//            @ApiResponse(responseCode = "404", description = "Cart not found")
//    })
//    @GetMapping
//    public ResponseEntity<CartResponse> getCart(
//            @CookieValue(value = "cart_session_id", required = false) String sessionId,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//        log.info("Retrieving cart for sessionId: {}, page: {}, size: {}", sessionId, page, size);
//        if (sessionId == null || sessionId.isEmpty()) {
//            log.error("Missing or invalid cart_session_id cookie");
//            throw BadRequestException.builder().message("Missing or invalid cart session ID").build();
//        }
//        Pageable pageable = PageRequest.of(page, size);
//        CartResponse cartResponse = cartService.getCart(sessionId, pageable);
//        return ResponseEntity.ok(cartResponse);
//    }
//
/// /    @Operation(summary = "Get cart by session ID")
/// /    @ApiResponses({
/// /            @ApiResponse(responseCode = "200", description = "Cart retrieved successfully"),
/// /            @ApiResponse(responseCode = "400", description = "Missing or invalid cart session ID"),
/// /            @ApiResponse(responseCode = "404", description = "Cart not found")
/// /    })
/// /    @GetMapping
/// /    public ResponseEntity<CartResponse> getCart(
/// /            @CookieValue(value = "cart_session_id", required = false) String sessionId) {
/// /        log.info("Retrieving cart for sessionId: {}", sessionId);
/// /        if (sessionId == null || sessionId.isEmpty()) {
/// /            log.error("Missing or invalid cart_session_id cookie");
/// /            throw BadRequestException.builder().message("Missing or invalid cart session ID").build();
/// /        }
/// /        CartResponse cartResponse = cartService.getCart(sessionId);
/// /        return ResponseEntity.ok(cartResponse);
/// /    }
//
//
//    @Operation(summary = "Add item to cart")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Item added to cart successfully"),
//            @ApiResponse(responseCode = "400", description = "Invalid cart request or session ID"),
//            @ApiResponse(responseCode = "404", description = "Product not found")
//    })
//    @PostMapping("/add")
//    public ResponseEntity<CartResponse> addToCart(
//            @CookieValue(value = "cart_session_id", required = false) String sessionId,
//            @Valid @RequestBody CartRequest cartRequest,
//            HttpServletResponse response) {
//        log.info("Adding item to cart for sessionId: {}", sessionId);
//        if (sessionId == null) {
//            sessionId = UUID.randomUUID().toString();
//            Cookie cookie = new Cookie("cart_session_id", sessionId);
//            cookie.setPath("/");
//            cookie.setMaxAge(12 * 30 * 24 * 60 * 60); // 1 year
//            //cookie.setHttpOnly(true);
//            //cookie.setSecure(true); // TODO: Enable in production
//            response.addCookie(cookie);
//        }
//        CartResponse cartResponse = cartService.addToCart(sessionId, cartRequest);
//        return ResponseEntity.ok(cartResponse);
//    }
//
//
//    @Operation(summary = "Update cart item quantity")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Cart item updated successfully"),
//            @ApiResponse(responseCode = "400", description = "Invalid session ID or quantity"),
//            @ApiResponse(responseCode = "404", description = "Cart or cart item not found")
//    })
//    @PutMapping("/update/{cartItemId}")
//    public ResponseEntity<CartResponse> updateCartItem(
//            @CookieValue(value = "cart_session_id", required = false) String sessionId,
//            @PathVariable Long cartItemId,
//            @RequestParam int quantity) {
//        log.info("Updating cart item {} with quantity {} for sessionId: {}", cartItemId, quantity, sessionId);
//        if (sessionId == null || sessionId.isEmpty()) {
//            log.error("Missing or invalid cart_session_id cookie");
//            throw BadRequestException.builder().message("Missing or invalid cart session ID").build();
//        }
//        CartResponse cartResponse = cartService.updateCart(sessionId, cartItemId, quantity);
//        return ResponseEntity.ok(cartResponse);
//    }
//
//
//    @Operation(summary = "Remove item from cart")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Item removed from cart successfully"),
//            @ApiResponse(responseCode = "400", description = "Invalid session ID"),
//            @ApiResponse(responseCode = "404", description = "Cart or cart item not found")
//    })
//    @DeleteMapping("/remove/{cartItemId}")
//    public ResponseEntity<CartResponse> removeFromCart(
//            @CookieValue(value = "cart_session_id", required = false) String sessionId,
//            @PathVariable Long cartItemId) {
//        log.info("Removing cart item {} for sessionId: {}", cartItemId, sessionId);
//        if (sessionId == null || sessionId.isEmpty()) {
//            log.error("Missing or invalid cart_session_id cookie");
//            throw BadRequestException.builder().message("Missing or invalid cart session ID").build();
//        }
//        CartResponse cartResponse = cartService.removeFromCart(sessionId, cartItemId);
//        return ResponseEntity.ok(cartResponse);
//    }
//
//    @Operation(summary = "Clear cart")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Cart cleared successfully"),
//            @ApiResponse(responseCode = "400", description = "Invalid session ID"),
//            @ApiResponse(responseCode = "404", description = "Cart not found")
//    })
//    @DeleteMapping("/clear")
//    public ResponseEntity<Void> clearCart(
//            @CookieValue(value = "cart_session_id", required = false) String sessionId) {
//        log.info("Clearing cart for sessionId: {}", sessionId);
//        if (sessionId == null || sessionId.isEmpty()) {
//            log.error("Missing or invalid cart_session_id cookie");
//            throw BadRequestException.builder().message("Missing or invalid cart session ID").build();
//        }
//        cartService.clearCart(sessionId);
//        return ResponseEntity.ok().build();
//    }
//
//    @Operation(summary = "Apply coupon to cart")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Coupon applied successfully"),
//            @ApiResponse(responseCode = "400", description = "Invalid session ID or coupon code"),
//            @ApiResponse(responseCode = "404", description = "Cart or coupon not found"),
//            @ApiResponse(responseCode = "422", description = "Coupon is invalid, expired, or usage limit exceeded")
//    })
//    @PostMapping("/coupon/apply")
//    public ResponseEntity<AppliedCouponResponse> applyCoupon(
//            @CookieValue(value = "cart_session_id", required = false) String sessionId,
//            @Valid @RequestBody CouponRequest couponRequest) {
//        log.info("Applying coupon {} to cart for sessionId: {}", couponRequest.getCode(), sessionId);
//        if (sessionId == null || sessionId.isEmpty()) {
//            log.error("Missing or invalid cart_session_id cookie");
//            throw BadRequestException.builder().message("Missing or invalid cart session ID").build();
//        }
//        if (couponRequest.getCode() == null || couponRequest.getCode().isEmpty()) {
//            log.error("Invalid coupon code");
//            throw BadRequestException.builder().message("Invalid coupon code").build();
//        }
//        AppliedCouponResponse appliedCouponResponse = cartService.applyCoupon(sessionId, couponRequest.getCode());
//        return ResponseEntity.ok(appliedCouponResponse);
//    }
//
/// /    @Operation(summary = "Apply coupon to cart")
/// /    @ApiResponses({
/// /            @ApiResponse(responseCode = "200", description = "Coupon applied successfully"),
/// /            @ApiResponse(responseCode = "400", description = "Invalid session ID or coupon code"),
/// /            @ApiResponse(responseCode = "404", description = "Cart or coupon not found")
/// /    })
/// /    @PostMapping("/coupon/apply")
/// /    public ResponseEntity<CouponResponse> applyCouponV1(
/// /            @CookieValue(value = "cart_session_id", required = false) String sessionId,
/// /            @Valid @RequestBody CouponRequest couponRequest) {
/// /        log.info("Applying coupon {} to cart for sessionId: {}", couponRequest.getCode(), sessionId);
/// /        if (sessionId == null || sessionId.isEmpty()) {
/// /            log.error("Missing or invalid cart_session_id cookie");
/// /            throw BadRequestException.builder().message("Missing or invalid cart session ID").build();
/// /        }
/// /        if (couponRequest.getCode() == null || couponRequest.getCode().isEmpty()) {
/// /            log.error("Invalid coupon code");
/// /            throw BadRequestException.builder().message("Invalid coupon code").build();
/// /        }
/// /        BigDecimal discount = couponService.applyCoupon(couponRequest.getCode(), sessionId);
/// /        CartResponse cartResponse = cartService.getCart(sessionId);
/// /        return ResponseEntity.ok(CouponResponse.builder()
/// /                .discount(discount)
/// /                .cartResponse(cartResponse).build());
/// /    }
//
//
////    /* need to be finished and tested */
////    @PostMapping("/coupon/apply")
////    public ResponseEntity<?> applyCoupon(@RequestBody CouponRequest couponRequest) {
////        BigDecimal discount = BigDecimal.valueOf(0.0);
////        Map<String, Object> response = new HashMap<>();
////        if (couponRequest.getCode() != null && !couponRequest.getCode().isEmpty()) {
////            // discount = couponService.applyCoupon(couponRequest.getCouponCode());
////        }
////        response.put("discount", discount);
////
////        return ResponseEntity.ok().body(response);
////    }
//
//    @Operation(summary = "Remove coupon from cart")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Coupon removed successfully"),
//            @ApiResponse(responseCode = "400", description = "Invalid session ID"),
//            @ApiResponse(responseCode = "404", description = "Cart not found")
//    })
//    @DeleteMapping("/coupon/remove")
//    public ResponseEntity<CartResponse> removeCoupon(
//            @CookieValue(value = "cart_session_id", required = false) String sessionId) {
//        log.info("Removing coupon from cart for sessionId: {}", sessionId);
//        if (sessionId == null || sessionId.isEmpty()) {
//            log.error("Missing or invalid cart_session_id cookie");
//            throw BadRequestException.builder().message("Missing or invalid cart session ID").build();
//        }
//        CartResponse cartResponse = cartService.removeCoupon(sessionId);
//        return ResponseEntity.ok(cartResponse);
//    }
//
//    @Operation(summary = "Get cart total for authenticated user")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Cart total retrieved successfully"),
//            @ApiResponse(responseCode = "401", description = "User not authenticated"),
//            @ApiResponse(responseCode = "404", description = "User or cart not found")
//    })
//    @GetMapping("/total")
//    public ResponseEntity<CartTotalResponse> getCartTotal() {
//        log.info("Retrieving cart total for authenticated user");
//        String email = SecurityContextHolder.getContext().getAuthentication().getName();
//        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
//                .orElseThrow(() -> UserNotFoundException.builder()
//                        .message("User not found with email: " + email)
//                        .build());
//        BigDecimal total = cartService.calculateTotalAmount(user.getId());
//        BigDecimal discount = cartService.calculateDiscount(user.getId());
//        return ResponseEntity.ok(CartTotalResponse.builder()
//                .total(total)
//                .discount(discount).build());
//    }
//
//
/// /    @GetMapping("/total/{userId}")
/// /    public ResponseEntity<?> getTotalCart(@PathVariable Long userId) {
/// /        BigDecimal total = cartService.calculateTotalAmount(userId);
/// /        Map<String, Object> response = new HashMap<>();
/// /        response.put("total", total);
/// /        return ResponseEntity.ok().body(response);
/// /    }
//
////    @GetMapping("/discount/{userId}")
////    public ResponseEntity<?> getDiscount(@PathVariable Long userId) {
////        BigDecimal discount = cartService.calculateDiscount(userId);
////        Map<String, Object> response = new HashMap<>();
////        response.put("discount", discount);
////        return ResponseEntity.ok().body(response);
////    }
//
//
////    @DeleteMapping("/remove/{cartItemId}")
////    public ResponseEntity<Void> removeFromCart(String sessionId, @PathVariable Long cartItemId) {
////        cartService.removeFromCart(sessionId, cartItemId);
////        return ResponseEntity.noContent().build();
////    }
//
//
////    @Operation(summary = "Merge anonymous cart on user login")
////    @ApiResponses({
////            @ApiResponse(responseCode = "200", description = "Cart merged successfully"),
////            @ApiResponse(responseCode = "400", description = "Invalid request or missing session ID"),
////            @ApiResponse(responseCode = "401", description = "Authentication failed"),
////            @ApiResponse(responseCode = "404", description = "User not found")
////    })
////    @PostMapping("/merge")
////    public ResponseEntity<MergeCartResponse> mergeCartOnLogin(
////            @CookieValue(value = "cart_session_id", required = false) String sessionId,
////            @Valid @RequestBody LoginEmailRequest loginEmailRequest) {
////        log.info("Merging cart for sessionId: {} and email: {}", sessionId, loginEmailRequest.getEmailAddress());
////
////        if (sessionId == null || sessionId.isEmpty()) {
////            log.error("Missing or invalid cart_session_id cookie");
////            throw BadRequestException.builder().message("Missing or invalid cart session ID").build();
////        }
////
////        try {
////            // Authenticate user and get AuthResponse
////            AuthResponse authResponse = authService.loginUser(loginEmailRequest);
////
////            // Retrieve user ID from email
////            Long userId = userRepository.findByEmailAddressAndDeletedFalse(authResponse.getEmail())
////                    .orElseThrow(() -> UserNotFoundException.builder()
////                            .message("User not found with email: " + authResponse.getEmail())
////                            .build())
////                    .getId();
////
////            // Merge cart
////            CartResponse cartResponse = cartService.mergeCartOnLogin(sessionId, userId);
////
////            // Return combined response
////            return ResponseEntity.ok(new MergeCartResponse(authResponse, cartResponse));
////        } catch (AuthenticationException e) {
////            log.error("Authentication failed for email: {}", loginEmailRequest.getEmailAddress());
////            throw BadRequestException.builder().message("Authentication failed: Invalid credentials").build();
////        }
////    }
////
////    // Combined response class
////    public static class MergeCartResponse {
////        private final AuthResponse authResponse;
////        private final CartResponse cartResponse;
////
////        public MergeCartResponse(AuthResponse authResponse, CartResponse cartResponse) {
////            this.authResponse = authResponse;
////            this.cartResponse = cartResponse;
////        }
////
////        public AuthResponse getAuthResponse() {
////            return authResponse;
////        }
////
////        public CartResponse getCartResponse() {
////            return cartResponse;
////        }
////    }
//
//
//}
