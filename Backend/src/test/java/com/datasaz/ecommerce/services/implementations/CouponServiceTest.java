package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.CartNotFoundException;
import com.datasaz.ecommerce.exceptions.CouponInvalidException;
import com.datasaz.ecommerce.exceptions.CouponLimitExceededException;
import com.datasaz.ecommerce.repositories.CartRepository;
import com.datasaz.ecommerce.repositories.CouponRepository;
import com.datasaz.ecommerce.repositories.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.annotation.EnableRetry;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@EnableRetry
public class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CouponService couponService;

    private Coupon coupon;
    private User seller;
    private Company company;
    private Cart cart;
    private Product product;
    private List<CartItem> cartItems;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        seller = new User();
        seller.setId(1L);
        seller.setCompany(company);

        coupon = new Coupon();
        coupon.setCode("SAVE10");
        coupon.setState(CouponState.ACTIVE);
        coupon.setCategory(CouponCategory.GENERAL);
        coupon.setCouponScope(CouponScope.ORDER);
        coupon.setCouponType(CouponType.PERCENTAGE);
        coupon.setDiscountPercentage(new BigDecimal("10"));
        coupon.setStartFrom(LocalDateTime.now().minusDays(1));
        coupon.setEndAt(LocalDateTime.now().plusDays(1));
        coupon.setMaxUsesPerUser(1);
        coupon.setMaxUses(10);
        coupon.setAuthor(seller);
        coupon.setCouponTrackings(new HashSet<>());
        coupon.setVersion(1L);

        product = new Product();
        product.setId(1L);
        product.setPrice(new BigDecimal("99.99"));
        product.setAuthor(seller);
        product.setCompany(company);

        CartItem cartItem = new CartItem();
        cartItem.setProduct(product);
        cartItem.setQuantity(2);
        cartItems = List.of(cartItem);

        cart = new Cart();
        cart.setSessionId("test-session");
        cart.setItems(new ArrayList<>(cartItems));
        cart.setSubtotalPrice(new BigDecimal("199.98"));
        cart.setTotalDiscount(BigDecimal.ZERO);
    }

    @Test
    void validateCouponWithLock_succeeds() {
        when(couponRepository.findByCodeWithLock("SAVE10")).thenReturn(Optional.of(coupon));

        Coupon result = couponService.validateCouponWithLock(
                "SAVE10", null, new BigDecimal("199.98"), cartItems, null);

        assertNotNull(result);
        assertEquals("SAVE10", result.getCode());
        verify(couponRepository).findByCodeWithLock("SAVE10");
    }

    @Test
    void validateCoupon_maxUsesExceeded_throwsException() {
        CouponTracking tracking = new CouponTracking();
        tracking.setUser(seller);
        tracking.setUsed(true);
        coupon.getCouponTrackings().add(tracking);

        when(couponRepository.findByCodeWithTrackings("SAVE10")).thenReturn(Optional.of(coupon));

        assertThrows(CouponLimitExceededException.class, () ->
                couponService.validateCoupon("SAVE10", seller, new BigDecimal("199.98"), cartItems));
    }

    @Test
    void applyCoupon_validAuthorProduct_succeeds() {
        when(couponRepository.findByCodeWithTrackings("SAVE10")).thenReturn(Optional.of(coupon));
        when(cartRepository.findBySessionIdWithItemsAndCoupon("test-session")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        BigDecimal discount = couponService.applyCoupon("SAVE10", "test-session");

        assertEquals(new BigDecimal("20.00"), discount); // 10% of 199.98, rounded up
        assertEquals(new BigDecimal("20.00"), cart.getTotalDiscount());
        assertEquals(new BigDecimal("179.98"), cart.getSubtotalPrice());
        verify(cartRepository).save(cart);
        verify(couponRepository).findByCodeWithTrackings("SAVE10");
    }

    @Test
    void applyCoupon_invalidAuthorProduct_throwsException() {
        User differentSeller = new User();
        differentSeller.setId(2L);
        Company differentCompany = new Company();
        differentCompany.setId(2L);
        differentSeller.setCompany(differentCompany);

        Product invalidProduct = new Product();
        invalidProduct.setId(2L);
        invalidProduct.setPrice(new BigDecimal("99.99"));
        invalidProduct.setAuthor(differentSeller);
        invalidProduct.setCompany(differentCompany);

        CartItem invalidCartItem = new CartItem();
        invalidCartItem.setProduct(invalidProduct);
        invalidCartItem.setQuantity(2);
        List<CartItem> invalidCartItems = List.of(invalidCartItem);

        Cart invalidCart = new Cart();
        invalidCart.setSessionId("invalid-session");
        invalidCart.setItems(new ArrayList<>(invalidCartItems));
        invalidCart.setSubtotalPrice(new BigDecimal("199.98"));
        invalidCart.setTotalDiscount(BigDecimal.ZERO);

        when(couponRepository.findByCodeWithTrackings("SAVE10")).thenReturn(Optional.of(coupon));
        when(cartRepository.findBySessionIdWithItemsAndCoupon("invalid-session")).thenReturn(Optional.of(invalidCart));

        assertThrows(CouponInvalidException.class, () ->
                couponService.applyCoupon("SAVE10", "invalid-session"));
        verify(couponRepository).findByCodeWithTrackings("SAVE10");
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void applyCoupon_cartNotFound_throwsException() {
        when(cartRepository.findBySessionIdWithItemsAndCoupon("invalid-session")).thenReturn(Optional.empty());

        assertThrows(CartNotFoundException.class, () ->
                couponService.applyCoupon("SAVE10", "invalid-session"));
        verify(cartRepository).findBySessionIdWithItemsAndCoupon("invalid-session");
        verify(couponRepository, never()).findByCodeWithTrackings(anyString());
    }

    @Test
    void validateCoupon_companySpecific_validCompany_succeeds() {
        coupon.setCategory(CouponCategory.COMPANY_SPECIFIC);
        CouponTracking tracking = new CouponTracking();
        tracking.setCompany(company);
        tracking.setUsed(false);
        coupon.getCouponTrackings().add(tracking);

        when(couponRepository.findByCodeWithTrackings("SAVE10")).thenReturn(Optional.of(coupon));

        Coupon result = couponService.validateCoupon("SAVE10", null, new BigDecimal("199.98"), cartItems);

        assertNotNull(result);
        assertEquals("SAVE10", result.getCode());
        verify(couponRepository).findByCodeWithTrackings("SAVE10");
    }

    @Test
    void validateCoupon_companySpecific_invalidCompany_throwsException() {
        coupon.setCategory(CouponCategory.COMPANY_SPECIFIC);
        CouponTracking tracking = new CouponTracking();
        tracking.setCompany(company);
        tracking.setUsed(false);
        coupon.getCouponTrackings().add(tracking);

        User differentSeller = new User();
        differentSeller.setId(2L);
        Company differentCompany = new Company();
        differentCompany.setId(2L);
        differentSeller.setCompany(differentCompany);

        Product invalidProduct = new Product();
        invalidProduct.setId(2L);
        invalidProduct.setPrice(new BigDecimal("99.99"));
        invalidProduct.setAuthor(differentSeller);
        invalidProduct.setCompany(differentCompany);

        CartItem invalidCartItem = new CartItem();
        invalidCartItem.setProduct(invalidProduct);
        invalidCartItem.setQuantity(2);
        List<CartItem> invalidCartItems = List.of(invalidCartItem);

        when(couponRepository.findByCodeWithTrackings("SAVE10")).thenReturn(Optional.of(coupon));

        assertThrows(CouponInvalidException.class, () ->
                couponService.validateCoupon("SAVE10", null, new BigDecimal("199.98"), invalidCartItems));
        verify(couponRepository).findByCodeWithTrackings("SAVE10");
    }
}

/*

import com.datasaz.ecommerce.exceptions.CouponLimitExceededException;
import com.datasaz.ecommerce.repositories.CouponRepository;
import com.datasaz.ecommerce.repositories.entities.*;
import com.datasaz.ecommerce.services.implementations.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.annotation.EnableRetry;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@EnableRetry
public class CouponServiceTest {
    @Mock
    private CouponRepository couponRepository;
    @InjectMocks
    private CouponService couponService;

    private Coupon coupon;
    private User user;
    private Order order;
    private List<CartItem> cartItems;

    @BeforeEach
    void setUp() {
        coupon = new Coupon();
        coupon.setCode("SAVE10");
        coupon.setState(CouponState.ACTIVE);
        coupon.setCategory(CouponCategory.GENERAL);
        coupon.setCouponScope(CouponScope.ORDER);
        coupon.setCouponType(CouponType.FIXED);
        coupon.setStartFrom(LocalDateTime.now().minusDays(1));
        coupon.setEndAt(LocalDateTime.now().plusDays(1));
        coupon.setMaxUsesPerUser(1);
        coupon.setCouponTrackings(new HashSet<>());
        coupon.setVersion(1L);

        user = new User();
        user.setId(1L);

        order = new Order();
        order.setId(1L);

        Product product = new Product();
        product.setId(1L);
        product.setPrice(new BigDecimal("99.99"));

        CartItem cartItem = new CartItem();
        cartItem.setProduct(product);
        cartItem.setQuantity(2);
        cartItems = List.of(cartItem);
    }

//    @Test
//    void validateCoupon_optimisticLockConflict_retriesAndSucceeds() {
//        when(couponRepository.findByIdentifier("SAVE10"))
//                .thenThrow(new OptimisticLockException())
//                .thenReturn(Optional.of(coupon));
//
//        Coupon result = couponService.validateCoupon(
//                "SAVE10", user, new BigDecimal("199.98"), cartItems);
//
//        assertNotNull(result);
//        assertEquals("SAVE10", result.getIdentifier());
//        verify(couponRepository, times(2)).findByIdentifier("SAVE10");
//    }
//
//    @Test
//    void trackCouponUsage_optimisticLockConflict_retriesAndSucceeds() {
//        when(couponRepository.save(any(Coupon.class)))
//                .thenThrow(new OptimisticLockException())
//                .thenReturn(coupon);
//
//        couponService.trackCouponUsage(coupon, user, order, null, null);
//
//        assertEquals(1, coupon.getCouponTrackings().size());
//        verify(couponRepository, times(2)).save(coupon);
//    }

    @Test
    void validateCouponWithLock_succeeds() {
        when(couponRepository.findByCodeWithLock("SAVE10")).thenReturn(Optional.of(coupon));

        Coupon result = couponService.validateCouponWithLock(
                "SAVE10", user, new BigDecimal("199.98"), cartItems, null);

        assertNotNull(result);
        assertEquals("SAVE10", result.getCode());
        verify(couponRepository).findByCodeWithLock("SAVE10");
    }

    @Test
    void validateCoupon_maxUsesExceeded_throwsException() {
        CouponTracking tracking = new CouponTracking();
        tracking.setUser(user);
        tracking.setUsed(true);
        coupon.getCouponTrackings().add(tracking);

        when(couponRepository.findByCodeWithTrackings("SAVE10")).thenReturn(Optional.of(coupon));

        assertThrows(CouponLimitExceededException.class, () ->
                couponService.validateCoupon("SAVE10", user, new BigDecimal("199.98"), cartItems));
    }
}*/
