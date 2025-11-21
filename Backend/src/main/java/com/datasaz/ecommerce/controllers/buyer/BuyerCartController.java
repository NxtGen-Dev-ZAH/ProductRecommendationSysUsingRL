package com.datasaz.ecommerce.controllers.buyer;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.models.request.CartItemRequest;
import com.datasaz.ecommerce.models.request.CouponCodeRequest;
import com.datasaz.ecommerce.models.response.AppliedCouponResponse;
import com.datasaz.ecommerce.models.response.CartResponse;
import com.datasaz.ecommerce.services.interfaces.IBuyerCartService;
import com.datasaz.ecommerce.utilities.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/buyer/v1/cart")
@RequiredArgsConstructor
public class BuyerCartController {

    private final IBuyerCartService buyerCartService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "Add item to cart", description = "Adds an item to the cart for authenticated users")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Item added to cart successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid cart request"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartResponse> addToCart(
            @Valid @RequestBody CartItemRequest cartItemRequest) {
        log.info("POST /buyer/v1/cart for authenticated user");
        Long userId = currentUserService.getCurrentUser().getId();
        CartResponse response = buyerCartService.addToCart(String.valueOf(userId), cartItemRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Update cart item quantity", description = "Updates the quantity of a cart item for authenticated users")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart item updated successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Cart or cart item not found")
    })
    @PutMapping("/items/{cartItemId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartResponse> updateCartItem(
            @PathVariable Long cartItemId,
            @RequestParam int quantity) {
        log.info("PUT /buyer/v1/cart/items/{} for authenticated user", cartItemId);
        Long userId = currentUserService.getCurrentUser().getId();
        CartResponse response = buyerCartService.updateCartItem(String.valueOf(userId), cartItemId, quantity);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove item from cart", description = "Removes an item from the cart for authenticated users")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item removed from cart successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Cart or cart item not found")
    })
    @DeleteMapping("/items/{cartItemId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartResponse> removeFromCart(
            @PathVariable Long cartItemId) {
        log.info("DELETE /buyer/v1/cart/items/{} for authenticated user", cartItemId);
        Long userId = currentUserService.getCurrentUser().getId();
        CartResponse response = buyerCartService.removeFromCart(String.valueOf(userId), cartItemId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Clear cart", description = "Clears all items from the cart for authenticated users")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart cleared successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @DeleteMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartResponse> clearCart() {
        log.info("DELETE /buyer/v1/cart for authenticated user");
        Long userId = currentUserService.getCurrentUser().getId();
        CartResponse response = buyerCartService.clearCart(String.valueOf(userId));
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Merge anonymous cart on login", description = "Merges an anonymous cart into the authenticated user's cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart merged successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid session ID"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Cart or user not found")
    })
    @PostMapping("/merge")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartResponse> mergeCartOnLogin(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId) {
        String effectiveSessionId = sessionId != null ? sessionId : cookieSessionId;
        log.info("POST /buyer/v1/cart/merge for sessionId: {}", effectiveSessionId);
        if (effectiveSessionId == null || effectiveSessionId.isEmpty()) {
            log.error("Missing or invalid cart_session_id");
            throw BadRequestException.builder().message("Missing or invalid cart session ID").build();
        }
        Long userId = currentUserService.getCurrentUser().getId();
        CartResponse response = buyerCartService.mergeCartOnLogin(effectiveSessionId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get cart", description = "Retrieves a paginated cart for authenticated users")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @GetMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartResponse> getCart(
            @PageableDefault(size = 10) Pageable pageable) {
        log.info("GET /buyer/v1/cart for authenticated user");
        Long userId = currentUserService.getCurrentUser().getId();
        CartResponse response = buyerCartService.getCart(String.valueOf(userId), pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Apply coupon to cart", description = "Applies a coupon to the cart for authenticated users")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coupon applied successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid coupon code"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Cart or coupon not found"),
            @ApiResponse(responseCode = "422", description = "Coupon is invalid, expired, or usage limit exceeded")
    })
    @PostMapping("/coupon")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<AppliedCouponResponse> applyCoupon(
            @Valid @RequestBody CouponCodeRequest couponCodeRequest) {
        log.info("POST /buyer/v1/cart/coupon for authenticated user");
        Long userId = currentUserService.getCurrentUser().getId();
        AppliedCouponResponse response = buyerCartService.applyCoupon(String.valueOf(userId), couponCodeRequest.getCode());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove coupon from cart", description = "Removes a coupon from the cart for authenticated users")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coupon removed successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @DeleteMapping("/coupon")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartResponse> removeCoupon() {
        log.info("DELETE /buyer/v1/cart/coupon for authenticated user");
        Long userId = currentUserService.getCurrentUser().getId();
        CartResponse response = buyerCartService.removeCoupon(String.valueOf(userId));
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get cart total", description = "Calculates the total amount for the authenticated user's cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart total retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @GetMapping("/total")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<BigDecimal> calculateTotalAmount() {
        log.info("GET /buyer/v1/cart/total for authenticated user");
        Long userId = currentUserService.getCurrentUser().getId();
        BigDecimal total = buyerCartService.calculateTotalAmount(String.valueOf(userId));
        return ResponseEntity.ok(total);
    }

    @Operation(summary = "Get cart discount", description = "Calculates the discount for the authenticated user's cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart discount retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @GetMapping("/discount")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<BigDecimal> calculateDiscount() {
        log.info("GET /buyer/v1/cart/discount for authenticated user");
        Long userId = currentUserService.getCurrentUser().getId();
        BigDecimal discount = buyerCartService.calculateDiscount(String.valueOf(userId));
        return ResponseEntity.ok(discount);
    }

    @Operation(summary = "Get cart discount", description = "Calculates the discount for the authenticated user's cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart discount retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @GetMapping("/shipping")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<BigDecimal> calculateTotalShippingCost() {
        log.info("GET /buyer/v1/cart/shipping for authenticated user");
        Long userId = currentUserService.getCurrentUser().getId();
        BigDecimal totalShippingCost = buyerCartService.calculateTotalShippingCost(String.valueOf(userId));
        return ResponseEntity.ok(totalShippingCost);
    }

    @Operation(summary = "Get cart discount", description = "Calculates the discount for the authenticated user's cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart discount retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @GetMapping("/subtotal")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<BigDecimal> calculateSubtotalPrice() {
        log.info("GET /buyer/v1/cart/subtotal for authenticated user");
        Long userId = currentUserService.getCurrentUser().getId();
        BigDecimal subtotalPrice = buyerCartService.calculateSubtotalPrice(String.valueOf(userId));
        return ResponseEntity.ok(subtotalPrice);
    }

}

/*
import com.datasaz.ecommerce.models.request.CartRequest;
import com.datasaz.ecommerce.models.request.CouponCodeRequest;
import com.datasaz.ecommerce.models.response.AppliedCouponResponse;
import com.datasaz.ecommerce.models.response.CartResponse;
import com.datasaz.ecommerce.services.interfaces.IBuyerCartService;
import com.datasaz.ecommerce.utilities.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/buyer/v1/cart")
@RequiredArgsConstructor
public class BuyerCartController {

    private final IBuyerCartService buyerCartService;
    private final CurrentUserService currentUserService;

//    @Operation(summary = "Add item to cart (anonymous)", description = "Adds an item to the cart for anonymous users using session ID")
//    @ApiResponses({
//            @ApiResponse(responseCode = "201", description = "Item added to cart successfully"),
//            @ApiResponse(responseCode = "400", description = "Invalid cart request or session ID"),
//            @ApiResponse(responseCode = "404", description = "Product not found")
//    })
//    @PostMapping
//    public ResponseEntity<CartResponse> addToCart(
//            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
//            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId,
//            @Valid @RequestBody CartRequest cartRequest) {
//        String effectiveSessionId = sessionId != null ? sessionId : cookieSessionId;
//        log.info("POST /buyer/v1/cart for sessionId: {}", effectiveSessionId);
//        CartResponse response = buyerCartService.addToCart(effectiveSessionId, cartRequest);
//        return new ResponseEntity<>(response, HttpStatus.CREATED);
//    }

    @Operation(summary = "Add item to cart (authenticated)", description = "Adds an item to the cart for authenticated users")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Item added to cart successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid cart request"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PostMapping("/user")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartResponse> addToCartForUser(
            @Valid @RequestBody CartRequest cartRequest) {
        log.info("POST /buyer/v1/cart/user for authenticated user");
        CartResponse response = buyerCartService.addToCartForUser(cartRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

//    @Operation(summary = "Update cart item quantity (anonymous)", description = "Updates the quantity of a cart item for anonymous users")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Cart item updated successfully"),
//            @ApiResponse(responseCode = "400", description = "Invalid session ID or quantity"),
//            @ApiResponse(responseCode = "404", description = "Cart or cart item not found")
//    })
//    @PutMapping("/items/{cartItemId}")
//    public ResponseEntity<CartResponse> updateCartItem(
//            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
//            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId,
//            @PathVariable Long cartItemId,
//            @RequestParam int quantity) {
//        String effectiveSessionId = sessionId != null ? sessionId : cookieSessionId;
//        log.info("PUT /buyer/v1/cart/items/{} for sessionId: {}", cartItemId, effectiveSessionId);
//        CartResponse response = buyerCartService.updateCartItem(effectiveSessionId, cartItemId, quantity);
//        return ResponseEntity.ok(response);
//    }

    @Operation(summary = "Update cart item quantity (authenticated)", description = "Updates the quantity of a cart item for authenticated users")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart item updated successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Cart or cart item not found")
    })
    @PutMapping("/user/items/{cartItemId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartResponse> updateCartItemForUser(
            @PathVariable Long cartItemId,
            @RequestParam int quantity) {
        log.info("PUT /buyer/v1/cart/user/items/{} for authenticated user", cartItemId);
        CartResponse response = buyerCartService.updateCartItemForUser(cartItemId, quantity);
        return ResponseEntity.ok(response);
    }

//    @Operation(summary = "Remove item from cart (anonymous)", description = "Removes an item from the cart for anonymous users")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Item removed from cart successfully"),
//            @ApiResponse(responseCode = "400", description = "Invalid session ID"),
//            @ApiResponse(responseCode = "404", description = "Cart or cart item not found")
//    })
//    @DeleteMapping("/items/{cartItemId}")
//    public ResponseEntity<CartResponse> removeFromCart(
//            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
//            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId,
//            @PathVariable Long cartItemId) {
//        String effectiveSessionId = sessionId != null ? sessionId : cookieSessionId;
//        log.info("DELETE /buyer/v1/cart/items/{} for sessionId: {}", cartItemId, effectiveSessionId);
//        CartResponse response = buyerCartService.removeFromCart(effectiveSessionId, cartItemId);
//        return ResponseEntity.ok(response);
//    }

    @Operation(summary = "Remove item from cart (authenticated)", description = "Removes an item from the cart for authenticated users")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item removed from cart successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Cart or cart item not found")
    })
    @DeleteMapping("/user/items/{cartItemId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartResponse> removeFromCartForUser(@PathVariable Long cartItemId) {
        log.info("DELETE /buyer/v1/cart/user/items/{} for authenticated user", cartItemId);
        CartResponse response = buyerCartService.removeFromCartForUser(cartItemId);
        return ResponseEntity.ok(response);
    }

//    @Operation(summary = "Clear cart (anonymous)", description = "Clears all items from the cart for anonymous users")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Cart cleared successfully"),
//            @ApiResponse(responseCode = "400", description = "Invalid session ID"),
//            @ApiResponse(responseCode = "404", description = "Cart not found")
//    })
//    @DeleteMapping
//    public ResponseEntity<CartResponse> clearCart(
//            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
//            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId) {
//        String effectiveSessionId = sessionId != null ? sessionId : cookieSessionId;
//        log.info("DELETE /buyer/v1/cart for sessionId: {}", effectiveSessionId);
//        CartResponse response = buyerCartService.clearCart(effectiveSessionId);
//        return ResponseEntity.ok(response);
//    }

    @Operation(summary = "Clear cart (authenticated)", description = "Clears all items from the cart for authenticated users")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart cleared successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @DeleteMapping("/user")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartResponse> clearCartForUser() {
        log.info("DELETE /buyer/v1/cart/user for authenticated user");
        CartResponse response = buyerCartService.clearCartForUser();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Merge anonymous cart on login", description = "Merges an anonymous cart into the authenticated user's cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart merged successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid session ID"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Cart or user not found")
    })
    @PostMapping("/merge")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartResponse> mergeCartOnLogin(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId) {
        String effectiveSessionId = sessionId != null ? sessionId : cookieSessionId;
        log.info("POST /buyer/v1/cart/merge for sessionId: {}", effectiveSessionId);
        CartResponse response = buyerCartService.mergeCartOnLogin(effectiveSessionId);
        return ResponseEntity.ok(response);
    }

//    @Operation(summary = "Get cart (anonymous)", description = "Retrieves a paginated cart for anonymous users")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Cart retrieved successfully"),
//            @ApiResponse(responseCode = "400", description = "Invalid session ID"),
//            @ApiResponse(responseCode = "404", description = "Cart not found")
//    })
//    @GetMapping
//    public ResponseEntity<CartResponse> getCart(
//            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
//            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId,
//            @PageableDefault(size = 10) Pageable pageable) {
//        String effectiveSessionId = sessionId != null ? sessionId : cookieSessionId;
//        log.info("GET /buyer/v1/cart for sessionId: {}", effectiveSessionId);
//        CartResponse response = buyerCartService.getCart(effectiveSessionId, pageable);
//        return ResponseEntity.ok(response);
//    }

    @Operation(summary = "Get cart (authenticated)", description = "Retrieves a paginated cart for authenticated users")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @GetMapping("/user")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartResponse> getCartForUser(@PageableDefault(size = 10) Pageable pageable) {
        log.info("GET /buyer/v1/cart/user for authenticated user");
        CartResponse response = buyerCartService.getCartForUser(pageable);
        return ResponseEntity.ok(response);
    }

//    @Operation(summary = "Apply coupon to cart (anonymous)", description = "Applies a coupon to the cart for anonymous users")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Coupon applied successfully"),
//            @ApiResponse(responseCode = "400", description = "Invalid session ID or coupon code"),
//            @ApiResponse(responseCode = "404", description = "Cart or coupon not found"),
//            @ApiResponse(responseCode = "422", description = "Coupon is invalid, expired, or usage limit exceeded")
//    })
//    @PostMapping("/coupon")
//    public ResponseEntity<AppliedCouponResponse> applyCoupon(
//            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
//            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId,
//            @Valid @RequestBody CouponRequest couponRequest) {
//        String effectiveSessionId = sessionId != null ? sessionId : cookieSessionId;
//        log.info("POST /buyer/v1/cart/coupon for sessionId: {}", effectiveSessionId);
//        AppliedCouponResponse response = buyerCartService.applyCoupon(effectiveSessionId, couponRequest.getCode());
//        return ResponseEntity.ok(response);
//    }

    @Operation(summary = "Apply coupon to cart (authenticated)", description = "Applies a coupon to the cart for authenticated users")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coupon applied successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid coupon code"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Cart or coupon not found"),
            @ApiResponse(responseCode = "422", description = "Coupon is invalid, expired, or usage limit exceeded")
    })
    @PostMapping("/user/coupon")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<AppliedCouponResponse> applyCouponForUser(@RequestBody CouponCodeRequest couponCodeRequest) {
        log.info("POST /buyer/v1/cart/user/coupon for authenticated user");
        AppliedCouponResponse response = buyerCartService.applyCouponForUser(couponCodeRequest.getCode());
        return ResponseEntity.ok(response);
    }

//    @Operation(summary = "Remove coupon from cart (anonymous)", description = "Removes a coupon from the cart for anonymous users")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Coupon removed successfully"),
//            @ApiResponse(responseCode = "400", description = "Invalid session ID"),
//            @ApiResponse(responseCode = "404", description = "Cart not found")
//    })
//    @DeleteMapping("/coupon")
//    public ResponseEntity<CartResponse> removeCoupon(
//            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
//            @CookieValue(value = "cart_session_id", required = false) String cookieSessionId) {
//        String effectiveSessionId = sessionId != null ? sessionId : cookieSessionId;
//        log.info("DELETE /buyer/v1/cart/coupon for sessionId: {}", effectiveSessionId);
//        CartResponse response = buyerCartService.removeCoupon(effectiveSessionId);
//        return ResponseEntity.ok(response);
//    }

    @Operation(summary = "Remove coupon from cart (authenticated)", description = "Removes a coupon from the cart for authenticated users")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coupon removed successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @DeleteMapping("/user/coupon")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartResponse> removeCouponForUser() {
        log.info("DELETE /buyer/v1/cart/user/coupon for authenticated user");
        CartResponse response = buyerCartService.removeCouponForUser();
        return ResponseEntity.ok(response);
    }

//    @Operation(summary = "Get cart total (authenticated)", description = "Calculates the total amount for the authenticated user's cart")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Cart total retrieved successfully"),
//            @ApiResponse(responseCode = "401", description = "User not authenticated"),
//            @ApiResponse(responseCode = "404", description = "User or cart not found")
//    })
//    @GetMapping("/total")
//    @PreAuthorize("hasRole('BUYER')")
//    public ResponseEntity<BigDecimal> calculateTotalAmount() {
//        log.info("GET /buyer/v1/cart/total for authenticated user");
//        User user = currentUserService.getCurrentUser();
//        BigDecimal total = buyerCartService.calculateTotalAmount(user.getId());
//        return ResponseEntity.ok(total);
//    }
}*/


/*package com.datasaz.ecommerce.controllers.buyer;

import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.models.request.CartRequest;
import com.datasaz.ecommerce.models.response.AppliedCouponResponse;
import com.datasaz.ecommerce.models.response.CartResponse;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.services.interfaces.IBuyerCartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/buyer/v1/cart")
@RequiredArgsConstructor
@PreAuthorize("hasRole('BUYER')")
public class BuyerCartController {

    private final IBuyerCartService buyerCartService;
    private final UserRepository userRepository;
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private boolean isAuthenticated() {
        return SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().isAuthenticated() &&
                !"anonymousUser".equals(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    private boolean isValidSessionId(String sessionId) {
        return sessionId != null && UUID_PATTERN.matcher(sessionId).matches();
    }

    private CartResponse mergeIfPossible(String sessionId) {
        if (isValidSessionId(sessionId) && isAuthenticated()) {
            return buyerCartService.mergeCartOnLogin(sessionId);
        }
        return null;
    }

    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(@CookieValue(value = "cart_session_id", required = false) String sessionId,
                                                  @Valid @RequestBody CartRequest cartRequest) {
        CartResponse response;
        if (isValidSessionId(sessionId) && isAuthenticated()) {
            response = buyerCartService.mergeCartOnLogin(sessionId);
            response = buyerCartService.addToCartForUser(cartRequest);
        } else if (sessionId != null) {
            response = buyerCartService.addToCart(sessionId, cartRequest);
        } else {
            response = buyerCartService.addToCartForUser(cartRequest);
        }
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/update/{cartItemId}")
    public ResponseEntity<CartResponse> updateCartItem(@CookieValue(value = "cart_session_id", required = false) String sessionId,
                                                       @PathVariable Long cartItemId, @RequestParam int quantity) {
        CartResponse response;
        if (isValidSessionId(sessionId) && isAuthenticated()) {
            response = buyerCartService.mergeCartOnLogin(sessionId);
            response = buyerCartService.updateCartItemForUser(cartItemId, quantity);
        } else if (sessionId != null) {
            response = buyerCartService.updateCartItem(sessionId, cartItemId, quantity);
        } else {
            response = buyerCartService.updateCartItemForUser(cartItemId, quantity);
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/remove/{cartItemId}")
    public ResponseEntity<CartResponse> removeFromCart(@CookieValue(value = "cart_session_id", required = false) String sessionId,
                                                       @PathVariable Long cartItemId) {
        CartResponse response;
        if (isValidSessionId(sessionId) && isAuthenticated()) {
            response = buyerCartService.removeFromCartForUser(cartItemId);
        } else if (sessionId != null) {
            response = buyerCartService.removeFromCart(sessionId, cartItemId);
        } else {
            response = buyerCartService.removeFromCartForUser(cartItemId);
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/clear")
    public ResponseEntity<CartResponse> clearCart(@CookieValue(value = "cart_session_id", required = false) String sessionId) {
        CartResponse response;
        if (isValidSessionId(sessionId) && isAuthenticated()) {
            response = buyerCartService.clearCartForUser();
        } else if (sessionId != null) {
            response = buyerCartService.clearCart(sessionId);
        } else {
            response = buyerCartService.clearCartForUser();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@CookieValue(value = "cart_session_id", required = false) String sessionId, Pageable pageable) {
        CartResponse response;
        if (isValidSessionId(sessionId) && isAuthenticated()) {
            response = buyerCartService.mergeCartOnLogin(sessionId);
            response = buyerCartService.getCartForUser(pageable);
        } else if (sessionId != null) {
            response = buyerCartService.getCart(sessionId, pageable);
        } else {
            response = buyerCartService.getCartForUser(pageable);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/merge")
    public ResponseEntity<CartResponse> mergeCartOnLogin(@CookieValue(value = "cart_session_id") String sessionId) {
        CartResponse response = buyerCartService.mergeCartOnLogin(sessionId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/apply-coupon")
    public ResponseEntity<AppliedCouponResponse> applyCoupon(@CookieValue(value = "cart_session_id", required = false) String sessionId,
                                                             @RequestParam String couponIdentifier) {
        AppliedCouponResponse response;
        if (isValidSessionId(sessionId) && isAuthenticated()) {
            buyerCartService.mergeCartOnLogin(sessionId);
            response = buyerCartService.applyCouponForUser(couponIdentifier);
        } else if (sessionId != null) {
            response = buyerCartService.applyCoupon(sessionId, couponIdentifier);
        } else {
            response = buyerCartService.applyCouponForUser(couponIdentifier);
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/remove-coupon")
    public ResponseEntity<CartResponse> removeCoupon(@CookieValue(value = "cart_session_id", required = false) String sessionId) {
        CartResponse response;
        if (isValidSessionId(sessionId) && isAuthenticated()) {
            response = buyerCartService.mergeCartOnLogin(sessionId);
            response = buyerCartService.removeCouponForUser();
        } else if (sessionId != null) {
            response = buyerCartService.removeCoupon(sessionId);
        } else {
            response = buyerCartService.removeCouponForUser();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/total")
    public ResponseEntity<BigDecimal> calculateTotalAmount() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found").build());
        BigDecimal total = buyerCartService.calculateTotalAmount(user.getId());
        return ResponseEntity.ok(total);
    }
}*/
/*
import com.datasaz.ecommerce.models.request.CartRequest;
import com.datasaz.ecommerce.models.response.AppliedCouponResponse;
import com.datasaz.ecommerce.models.response.CartResponse;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.services.interfaces.IBuyerCartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/buyer/v1/cart")
@RequiredArgsConstructor
@PreAuthorize("hasRole('BUYER')")
public class BuyerCartController {

    private final IBuyerCartService buyerCartService;
    private final UserRepository userRepository;

    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(@CookieValue(value = "cart_session_id", required = false) String sessionId,
                                                  @Valid @RequestBody CartRequest cartRequest) {
        CartResponse response;
        if (sessionId != null) {
            response = buyerCartService.addToCart(sessionId, cartRequest);
        } else {
            response = buyerCartService.addToCartForUser(cartRequest);
        }
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/update/{cartItemId}")
    public ResponseEntity<CartResponse> updateCartItem(@CookieValue(value = "cart_session_id", required = false) String sessionId,
                                                       @PathVariable Long cartItemId, @RequestParam int quantity) {
        CartResponse response;
        if (sessionId != null) {
            response = buyerCartService.updateCartItem(sessionId, cartItemId, quantity);
        } else {
            response = buyerCartService.updateCartItemForUser(cartItemId, quantity);
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/remove/{cartItemId}")
    public ResponseEntity<CartResponse> removeFromCart(@CookieValue(value = "cart_session_id", required = false) String sessionId,
                                                       @PathVariable Long cartItemId) {
        CartResponse response;
        if (sessionId != null) {
            response = buyerCartService.removeFromCart(sessionId, cartItemId);
        } else {
            response = buyerCartService.removeFromCartForUser(cartItemId);
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/clear")
    public ResponseEntity<CartResponse> clearCart(@CookieValue(value = "cart_session_id", required = false) String sessionId) {
        CartResponse response;
        if (sessionId != null) {
            response = buyerCartService.clearCart(sessionId);
        } else {
            response = buyerCartService.clearCartForUser();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@CookieValue(value = "cart_session_id", required = false) String sessionId, Pageable pageable) {
        CartResponse response;
        if (sessionId != null) {
            response = buyerCartService.getCart(sessionId, pageable);
        } else {
            response = buyerCartService.getCartForUser(pageable);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/merge")
    public ResponseEntity<CartResponse> mergeCartOnLogin(@CookieValue(value = "cart_session_id") String sessionId) {
        CartResponse response = buyerCartService.mergeCartOnLogin(sessionId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/apply-coupon")
    public ResponseEntity<AppliedCouponResponse> applyCoupon(@CookieValue(value = "cart_session_id", required = false) String sessionId,
                                                             @RequestParam String couponIdentifier) {
        AppliedCouponResponse response;
        if (sessionId != null) {
            response = buyerCartService.applyCoupon(sessionId, couponIdentifier);
        } else {
            response = buyerCartService.applyCouponForUser(couponIdentifier);
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/remove-coupon")
    public ResponseEntity<CartResponse> removeCoupon(@CookieValue(value = "cart_session_id", required = false) String sessionId) {
        CartResponse response;
        if (sessionId != null) {
            response = buyerCartService.removeCoupon(sessionId);
        } else {
            response = buyerCartService.removeCouponForUser();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/total")
    public ResponseEntity<BigDecimal> calculateTotalAmount() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        BigDecimal total = buyerCartService.calculateTotalAmount(user.getId());
        return ResponseEntity.ok(total);
    }
}*/
