package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.CartNotFoundException;
import com.datasaz.ecommerce.exceptions.CouponExpiredException;
import com.datasaz.ecommerce.exceptions.CouponInvalidException;
import com.datasaz.ecommerce.exceptions.CouponLimitExceededException;
import com.datasaz.ecommerce.repositories.CartRepository;
import com.datasaz.ecommerce.repositories.CouponRepository;
import com.datasaz.ecommerce.repositories.entities.*;
import com.datasaz.ecommerce.services.interfaces.ICouponService;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService implements ICouponService {

    private final CouponRepository couponRepository;
    private final CartRepository cartRepository;

    /**********
    public void applyCoupon(String codeCoupon) {
        log.info("applyCoupon {}",codeCoupon);

     }********/

    //@Retryable for optimistic locking to retry up to 3 times
    // with a 100ms delay on OptimisticLockException
    @Override
    @Transactional(readOnly = true)
    @Retryable(
            value = OptimisticLockException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    public Coupon validateCoupon(String code, User user, BigDecimal orderTotal,
                                 List<CartItem> cartItems) {
        Coupon coupon = couponRepository.findByCodeWithTrackings(code)
                .orElseThrow(() -> CouponInvalidException.builder().message("Coupon not found: " + code).build());

        // Validate state
        if (coupon.getState() != CouponState.ACTIVE) {
            log.info("Coupon is not active: {}", code);
            throw CouponInvalidException.builder().message("Coupon is not active: " + code).build();
        }

        // Validate dates
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getStartFrom()) || now.isAfter(coupon.getEndAt())) {
            log.info("Coupon is expired or not yet valid: {}", code);
            throw CouponExpiredException.builder().message("Coupon is expired or not yet valid: " + code).build();
        }

        // Validate minimum order amount
        if (coupon.getMinimumOrderAmount() != null &&
                orderTotal.compareTo(coupon.getMinimumOrderAmount()) < 0) {
            log.info("Order total does not meet minimum amount for coupon: {}", code);
            throw CouponInvalidException.builder().message(
                    "Order total does not meet minimum amount for coupon: " + code).build();
        }

        // Validate that all cart items belong to the coupon's author or their company
        if (!isCouponValidForAuthorOrCompany(coupon, cartItems)) {
            log.info("Coupon is not valid for the products in the cart (wrong author or company): {}", code);
            throw CouponInvalidException.builder()
                    .message("Coupon is not valid for the products in the cart: " + code).build();
        }

        // Validate category-specific rules
        switch (coupon.getCategory()) {
            case USER_SPECIFIC:
                if (user == null || !isCouponValidForUser(coupon, user)) {
                    log.info("Coupon is not valid for this user: {}", code);
                    throw CouponInvalidException.builder().message(
                            "Coupon is not valid for this user: " + code).build();
                }
                break;
            case PRODUCT_SPECIFIC:
                if (cartItems == null || !isCouponValidForProducts(coupon, cartItems)) {
                    log.info("Coupon is not valid for any products in the cart: {}", code);
                    throw CouponInvalidException.builder().message(
                            "Coupon is not valid for any products in the cart: " + code).build();
                }
                break;
            case CATEGORY_SPECIFIC:
                if (cartItems == null || !isCouponValidForCategories(coupon, cartItems)) {
                    log.info("Coupon is not valid for any categories in the cart: {}", code);
                    throw CouponInvalidException.builder().message(
                            "Coupon is not valid for any categories in the cart: " + code).build();
                }
                break;
            case GENERAL:
                // No specific restrictions
                break;
        }

        if (coupon.getMaxUses() > 0) {
            long totalUses = coupon.getCouponTrackings().stream()
                    .filter(CouponTracking::isUsed)
                    .count();
            if (totalUses >= coupon.getMaxUses()) {
                throw CouponLimitExceededException.builder().message("Coupon total usage limit exceeded: " + code).build();
            }
        }

        // Validate max uses per user
        if (coupon.getMaxUsesPerUser() > 0 && user != null) {
            long userUses = coupon.getCouponTrackings().stream()
                    .filter(tracking -> tracking.getUser() != null &&
                            tracking.getUser().getId().equals(user.getId()) &&
                            tracking.isUsed())
                    .count();
            if (userUses >= coupon.getMaxUsesPerUser()) {
                log.info("Coupon usage limit exceeded for user: {}", code);
                throw CouponLimitExceededException.builder().message(
                        "Coupon usage limit exceeded for user: " + code).build();
            }
        }

        return coupon;
    }

    // validateCouponWithLock for pessimistic locking using findByIdentifierWithLock
    @Transactional(readOnly = true)
    public Coupon validateCouponWithLock(String code, User user, BigDecimal orderTotal,
                                         List<CartItem> cartItems, Company company) {
        Coupon coupon = couponRepository.findByCodeWithLock(code)
                .orElseThrow(() -> {
                    log.info("Coupon not found: {}", code);
                    return CouponInvalidException.builder().message("Coupon not found: " + code).build();
                });

        // Same validation logic as validateCoupon
        if (coupon.getState() != CouponState.ACTIVE) {
            log.info("Coupon is not active: {}", code);
            throw CouponInvalidException.builder().message("Coupon is not active: " + code).build();
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getStartFrom()) || now.isAfter(coupon.getEndAt())) {
            log.info("Coupon is expired or not yet valid: {}", code);
            throw CouponExpiredException.builder().message("Coupon is expired or not yet valid: " + code).build();
        }

        if (coupon.getMinimumOrderAmount() != null &&
                orderTotal.compareTo(coupon.getMinimumOrderAmount()) < 0) {
            log.info("Order total does not meet minimum amount for coupon: {}", code);
            throw CouponInvalidException.builder().message(
                    "Order total does not meet minimum amount for coupon: " + code).build();
        }

        if (!isCouponValidForAuthorOrCompany(coupon, cartItems)) {
            log.info("Coupon is not valid for the products in the cart (wrong author or company): {}", code);
            throw CouponInvalidException.builder()
                    .message("Coupon is not valid for the products in the cart: " + code).build();
        }

        switch (coupon.getCategory()) {
            case USER_SPECIFIC:
                if (user == null || !isCouponValidForUser(coupon, user)) {
                    log.info("Coupon is not valid for this user: {}", code);
                    throw CouponInvalidException.builder().message(
                            "Coupon is not valid for this user: " + code).build();
                }
                break;
            case PRODUCT_SPECIFIC:
                if (cartItems == null || !isCouponValidForProducts(coupon, cartItems)) {
                    log.info("Coupon is not valid for any products in the cart: {}", code);
                    throw CouponInvalidException.builder().message(
                            "Coupon is not valid for any products in the cart: " + code).build();
                }
                break;
            case CATEGORY_SPECIFIC:
                if (cartItems == null || !isCouponValidForCategories(coupon, cartItems)) {
                    log.info("Coupon is not valid for any categories in the cart: {}", code);
                    throw CouponInvalidException.builder().message(
                            "Coupon is not valid for any categories in the cart: " + code).build();
                }
                break;
            case COMPANY_SPECIFIC:
                if (company == null || !isCouponValidForCompany(coupon, company)) {
                    log.info("Coupon is not valid for this company: {}", code);
                    throw CouponInvalidException.builder().message(
                            "Coupon is not valid for this company: " + code).build();
                }
                break;
            case GENERAL:
                break;
        }

        if (coupon.getMaxUses() > 0) {
            long totalUses = coupon.getCouponTrackings().stream()
                    .filter(CouponTracking::isUsed)
                    .count();
            if (totalUses >= coupon.getMaxUses()) {
                throw CouponLimitExceededException.builder().message("Coupon total usage limit exceeded: " + code).build();
            }
        }

        if (coupon.getMaxUsesPerUser() > 0 && user != null) {
            long userUses = coupon.getCouponTrackings().stream()
                    .filter(tracking -> tracking.getUser() != null &&
                            tracking.getUser().getId().equals(user.getId()) &&
                            tracking.isUsed())
                    .count();
            if (userUses >= coupon.getMaxUsesPerUser()) {
                log.info("Coupon usage limit exceeded for user: {}", code);
                throw CouponLimitExceededException.builder().message(
                        "Coupon usage limit exceeded for user: " + code).build();
            }
        }

        return coupon;
    }

    @Override
    @Transactional
    @Retryable(
            value = OptimisticLockException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    public void trackCouponUsage(Coupon coupon, User user, Order order,
                                 Product product, Category category) {
        CouponTracking tracking = new CouponTracking();
        tracking.setCoupon(coupon);
        tracking.setUser(user);
        tracking.setProduct(product); // Set if PRODUCT_SPECIFIC
        tracking.setCategory(category); // Set if CATEGORY_SPECIFIC
        tracking.setCompany(coupon.getAuthor().getCompany()); // Set company based on coupon's author
        tracking.setUsed(true);
        coupon.getCouponTrackings().add(tracking);
        couponRepository.save(coupon);
    }

    @Override
    public BigDecimal calculateDiscount(Coupon coupon, List<CartItem> cartItems,
                                        BigDecimal subtotal) {
        BigDecimal applicableTotal = subtotal;

        // For ITEM scope, only apply to eligible items
        if (coupon.getCouponScope() == CouponScope.ITEM) {
            applicableTotal = BigDecimal.ZERO;
            for (CartItem item : cartItems) {
                if (isItemEligibleForCoupon(coupon, item)) {
                    applicableTotal = applicableTotal.add(
                            item.getProduct().getPrice()
                                    .multiply(BigDecimal.valueOf(item.getQuantity())));
                }
            }
        }

        BigDecimal discount = switch (coupon.getCouponType()) {
            case FIXED -> coupon.getDiscountFixedAmount();
            case PERCENTAGE -> applicableTotal.multiply(coupon.getDiscountPercentage())
                    .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);

        };

        // Ensure discount doesn't exceed applicable total
        return discount.min(applicableTotal);
    }

    //@Override
    private boolean isCouponValidForUser(Coupon coupon, User user) {
        return coupon.getCouponTrackings().stream()
                .anyMatch(tracking -> tracking.getUser() != null &&
                        tracking.getUser().getId().equals(user.getId()));
    }

    //@Override
    private boolean isCouponValidForProducts(Coupon coupon, List<CartItem> cartItems) {
        return cartItems.stream().anyMatch(item ->
                coupon.getCouponTrackings().stream()
                        .anyMatch(tracking -> tracking.getProduct() != null &&
                                tracking.getProduct().getId().equals(item.getProduct().getId())));
    }

    //@Override
    private boolean isCouponValidForCategories(Coupon coupon, List<CartItem> cartItems) {
        return cartItems.stream()
                .filter(item -> item.getProduct().getCategory() != null)
                .anyMatch(item ->
                        coupon.getCouponTrackings().stream()
                                .anyMatch(tracking -> tracking.getCategory() != null &&
                                        tracking.getCategory().getId().equals(
                                                item.getProduct().getCategory().getId())));
    }

    //@Override
    private boolean isCouponValidForCompany(Coupon coupon, Company company) {
        return coupon.getCouponTrackings().stream()
                .anyMatch(tracking -> tracking.getCompany() != null &&
                        tracking.getCompany().getId().equals(company.getId()));
    }

    @Override
    @Transactional
    public BigDecimal applyCoupon(String couponIdentifier, String sessionId) {
        Cart cart = cartRepository.findBySessionIdWithItemsAndCoupon(sessionId)
                .orElseThrow(() -> {
                    log.error("Cart not found for sessionId: {}", sessionId);
                    return CartNotFoundException.builder().message("Cart not found").build();
                });

        BigDecimal subtotal = cart.getItems().stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        User user = cart.getUser();
        Coupon coupon = validateCoupon(couponIdentifier, user, subtotal, cart.getItems());
        cart.setCoupon(coupon);
        BigDecimal discount = calculateDiscount(coupon, cart.getItems(), subtotal);
        cart.setTotalDiscount(discount);
        cart.setSubtotalPrice(subtotal.subtract(discount));
        cartRepository.save(cart);
        return discount;
    }

    @Override
    public boolean isItemEligibleForCoupon(Coupon coupon, CartItem item) {
        // Check if the item's product belongs to the coupon's author or their company
        Product product = item.getProduct();
        User couponAuthor = coupon.getAuthor();
        if (!product.getAuthor().getId().equals(couponAuthor.getId()) &&
                (product.getCompany() == null || couponAuthor.getCompany() == null ||
                        !product.getCompany().getId().equals(couponAuthor.getCompany().getId()))) {
            return false;
        }

        return switch (coupon.getCategory()) {
            case PRODUCT_SPECIFIC -> coupon.getCouponTrackings().stream()
                    .anyMatch(tracking -> tracking.getProduct() != null &&
                            tracking.getProduct().getId().equals(item.getProduct().getId()));
            case CATEGORY_SPECIFIC -> item.getProduct().getCategory() != null &&
                    coupon.getCouponTrackings().stream()
                            .anyMatch(tracking -> tracking.getCategory() != null &&
                                    tracking.getCategory().getId().equals(
                                            item.getProduct().getCategory().getId()));
            case GENERAL, USER_SPECIFIC, COMPANY_SPECIFIC -> true;
        };
    }

    private boolean isCouponValidForAuthorOrCompany(Coupon coupon, List<CartItem> cartItems) {
        User couponAuthor = coupon.getAuthor();
        Company authorCompany = couponAuthor.getCompany(); // Assuming User has a getCompany() method

        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            // Check if product author matches coupon author or product company matches coupon author's company
            if (!product.getAuthor().getId().equals(couponAuthor.getId()) &&
                    (product.getCompany() == null || authorCompany == null ||
                            !product.getCompany().getId().equals(authorCompany.getId()))) {
                return false;
            }
        }
        return true;
    }

}
