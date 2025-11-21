package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.CartItemNotFoundException;
import com.datasaz.ecommerce.exceptions.InsufficientStockException;
import com.datasaz.ecommerce.exceptions.ProductNotFoundException;
import com.datasaz.ecommerce.mappers.CartMapper;
import com.datasaz.ecommerce.models.request.CartItemRequest;
import com.datasaz.ecommerce.models.response.AppliedCouponResponse;
import com.datasaz.ecommerce.models.response.CartResponse;
import com.datasaz.ecommerce.repositories.CartRepository;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.entities.Cart;
import com.datasaz.ecommerce.repositories.entities.CartItem;
import com.datasaz.ecommerce.repositories.entities.Coupon;
import com.datasaz.ecommerce.repositories.entities.Product;
import com.datasaz.ecommerce.services.interfaces.ICouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CartMapper cartMapper;
    @Mock
    private ICouponService couponService;

    @InjectMocks
    private CartService cartService;

    private Cart cart;
    private Product product1, product2;
    private Coupon coupon;
    private CartResponse cartResponse;
    private AppliedCouponResponse appliedCouponResponse;

    private static final String VALID_SESSION_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String INVALID_SESSION_ID = "invalid";

    private Product product;
    private CartItem item;

    @BeforeEach
    void setUp() {
        product1 = Product.builder()
                .id(1L)
                .price(BigDecimal.valueOf(100.00))
                .offerPrice(null)
                .quantity(10)
                .shippingCost(BigDecimal.TEN)               // 10.00
                .eachAdditionalItemShippingCost(BigDecimal.ONE) // 1.00 per extra
                .build();

        product2 = Product.builder()
                .id(2L)
                .price(BigDecimal.valueOf(200.00))
                .quantity(5)
                .shippingCost(BigDecimal.valueOf(15.00))
                .eachAdditionalItemShippingCost(BigDecimal.valueOf(3.00))
                .build();

        product = Product.builder()
                .id(1L)
                .price(BigDecimal.valueOf(100.00))
                .offerPrice(null)
                .quantity(50)
                .shippingCost(BigDecimal.valueOf(10.00))
                .eachAdditionalItemShippingCost(BigDecimal.valueOf(2.00))
                .build();

        item = CartItem.builder()
                .id(10L)
                .product(product1)
                .quantity(2)
                .build();

        coupon = Coupon.builder()
                .id(1L)
                .code("SAVE10")
                .discountPercentage(BigDecimal.TEN)
                .build();

        cart = Cart.builder()
                .id(1L)
                .sessionId(VALID_SESSION_ID)
                .items(new ArrayList<>())
                .coupon(null)
                .subtotalPrice(BigDecimal.ZERO)
                .totalShippingCost(BigDecimal.ZERO)
                .totalDiscount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();

        cartResponse = CartResponse.builder().build();
        appliedCouponResponse = AppliedCouponResponse.builder()
                .code("SAVE10")
                .discount(BigDecimal.TEN)
                .cartResponse(cartResponse)
                .build();
    }

    /* ============================================== addToCart ============================================== */

    @Test
    void addToCart_validRequestNewCart_createsCartAndAddsItem() {
        CartItemRequest req = new CartItemRequest(1L, 2);

        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> {
            Cart c = i.getArgument(0);
            if (c.getId() == null) c.setId(999L);
            return c;
        });
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        CartResponse result = cartService.addToCart(VALID_SESSION_ID, req);

        assertNotNull(result);
        assertEquals(cartResponse, result);

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).save(captor.capture());
        Cart saved = captor.getValue();

        assertEquals(VALID_SESSION_ID, saved.getSessionId());
        assertEquals(1, saved.getItems().size());
        assertEquals(2, saved.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("200.0"), saved.getSubtotalPrice());
        assertEquals(new BigDecimal("11"), saved.getTotalShippingCost()); // 10 + 1
        assertEquals(BigDecimal.ZERO, saved.getTotalDiscount());
        assertEquals(new BigDecimal("211.0"), saved.getTotalAmount());
    }

    @Test
    void addToCart_validRequestExistingCart_addsNewItem() {
        CartItemRequest req = new CartItemRequest(1L, 2);

        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(cartRepository.save(any())).thenReturn(cart);
        when(cartMapper.toResponse(any())).thenReturn(cartResponse);

        cartService.addToCart(VALID_SESSION_ID, req);

        assertEquals(1, cart.getItems().size());
        assertEquals(2, cart.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("200.0"), cart.getSubtotalPrice());
    }

    @Test
    void addToCart_existingItem_updatesQuantity() {
        cart.getItems().add(CartItem.builder().product(product1).quantity(3).cart(cart).build());
        CartItemRequest req = new CartItemRequest(1L, 2);

        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(cartRepository.save(any())).thenReturn(cart);
        when(cartMapper.toResponse(any())).thenReturn(cartResponse);

        cartService.addToCart(VALID_SESSION_ID, req);

        assertEquals(5, cart.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("500.0"), cart.getSubtotalPrice());
    }

    @Test
    void addToCart_nullProductId_throwsBadRequestException() {
        CartItemRequest req = new CartItemRequest(null, 2);

        assertThrows(BadRequestException.class, () -> cartService.addToCart(VALID_SESSION_ID, req));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addToCart_negativeQuantity_throwsBadRequestException() {
        CartItemRequest req = new CartItemRequest(1L, -1);

        assertThrows(BadRequestException.class, () -> cartService.addToCart(VALID_SESSION_ID, req));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addToCart_nonExistentProduct_throwsProductNotFoundException() {
        CartItemRequest req = new CartItemRequest(1L, 2);

        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> cartService.addToCart(VALID_SESSION_ID, req));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addToCart_insufficientStock_throwsInsufficientStockException() {
        CartItemRequest req = new CartItemRequest(1L, 15);

        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));

        assertThrows(InsufficientStockException.class, () -> cartService.addToCart(VALID_SESSION_ID, req));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addToCart_invalidSessionId_throwsBadRequestException() {
        CartItemRequest req = new CartItemRequest(1L, 2);

        assertThrows(BadRequestException.class, () -> cartService.addToCart(INVALID_SESSION_ID, req));
        verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(any());
    }

    /* ============================================ updateCartItem ============================================ */

    @Test
    void updateCartItem_validRequest_updatesQuantity() {
        CartItem ci = CartItem.builder().id(1L).product(product1).quantity(2).cart(cart).build();
        cart.getItems().add(ci);

        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(cartRepository.save(any())).thenReturn(cart);
        when(cartMapper.toResponse(any())).thenReturn(cartResponse);

        cartService.updateCartItem(VALID_SESSION_ID, 1L, 5);

        assertEquals(5, cart.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("500.0"), cart.getSubtotalPrice());
    }

    @Test
    void updateCartItem_zeroQuantity_removesItem() {
        CartItem ci = CartItem.builder().id(1L).product(product1).quantity(2).cart(cart).build();
        cart.getItems().add(ci);

        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(cartRepository.save(any())).thenReturn(cart);
        when(cartMapper.toResponse(any())).thenReturn(cartResponse);

        cartService.updateCartItem(VALID_SESSION_ID, 1L, 0);

        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void updateCartItem_nonExistentItem_throwsCartItemNotFoundException() {
        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));

        assertThrows(CartItemNotFoundException.class, () -> cartService.updateCartItem(VALID_SESSION_ID, 1L, 5));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateCartItem_nonExistentProduct_throwsProductNotFoundException() {
        CartItem ci = CartItem.builder().id(1L).product(product1).quantity(2).cart(cart).build();
        cart.getItems().add(ci);

        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> cartService.updateCartItem(VALID_SESSION_ID, 1L, 5));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateCartItem_insufficientStock_throwsInsufficientStockException() {
        CartItem ci = CartItem.builder().id(1L).product(product1).quantity(2).cart(cart).build();
        cart.getItems().add(ci);

        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));

        assertThrows(InsufficientStockException.class, () -> cartService.updateCartItem(VALID_SESSION_ID, 1L, 15));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateCartItem_invalidSessionId_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> cartService.updateCartItem(INVALID_SESSION_ID, 1L, 5));
        verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(any());
    }

    /* ============================================ removeFromCart ============================================ */

    @Test
    void removeFromCart_validItemId_removesItem() {
        CartItem ci = CartItem.builder().id(1L).product(product1).quantity(2).cart(cart).build();
        cart.getItems().add(ci);

        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenReturn(cart);
        when(cartMapper.toResponse(any())).thenReturn(cartResponse);

        cartService.removeFromCart(VALID_SESSION_ID, 1L);

        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void removeFromCart_nonExistentItem_throwsCartItemNotFoundException() {
        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));

        assertThrows(CartItemNotFoundException.class, () -> cartService.removeFromCart(VALID_SESSION_ID, 1L));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void removeFromCart_invalidSessionId_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> cartService.removeFromCart(INVALID_SESSION_ID, 1L));
        verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(any());
    }

    /* =============================================== clearCart =============================================== */

    @Test
    void clearCart_withItems_clearsItemsAndSaves() {
        cart.getItems().add(CartItem.builder().product(product1).quantity(2).cart(cart).build());

        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenReturn(cart);
        when(cartMapper.toResponse(any())).thenReturn(cartResponse);

        cartService.clearCart(VALID_SESSION_ID);

        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository).save(cart);
    }

    @Test
    void clearCart_invalidSessionId_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> cartService.clearCart(INVALID_SESSION_ID));
        verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(any());
    }

    /* ================================================= getCart ================================================= */

    @Test
    void getCart_existingCart_returnsCartResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(eq(cart), eq(pageable))).thenReturn(cartResponse);

        CartResponse result = cartService.getCart(VALID_SESSION_ID, pageable);

        assertEquals(cartResponse, result);
        verify(cartMapper).toResponse(cart, pageable);
    }

    @Test
    void getCart_newCart_createsInMemoryAndReturns() {
        Pageable pageable = PageRequest.of(0, 10);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());
        when(cartMapper.toResponse(any(Cart.class), eq(pageable))).thenReturn(cartResponse);

        CartResponse result = cartService.getCart(VALID_SESSION_ID, pageable);

        assertEquals(cartResponse, result);
        verify(cartRepository, never()).save(any());

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartMapper).toResponse(captor.capture(), eq(pageable));
        Cart created = captor.getValue();

        assertEquals(VALID_SESSION_ID, created.getSessionId());
        assertNull(created.getId());
        assertTrue(created.getItems().isEmpty());
        assertEquals(BigDecimal.ZERO, created.getSubtotalPrice());
    }

    @Test
    void getCart_invalidSessionId_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> cartService.getCart(INVALID_SESSION_ID, PageRequest.of(0, 10)));
        verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(any());
    }

    /* =============================================== applyCoupon ============================================= */

    @Test
    void applyCoupon_validCoupon_appliesAndUpdatesTotals() {
        cart.getItems().add(CartItem.builder().product(product1).quantity(2).cart(cart).build());

        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(couponService.validateCoupon(eq("SAVE10"), isNull(), any(), any())).thenReturn(coupon);
        when(couponService.calculateDiscount(eq(coupon), any(), any())).thenReturn(BigDecimal.valueOf(20.00));
        when(cartRepository.save(any())).thenReturn(cart);
        when(cartMapper.toResponse(any())).thenReturn(cartResponse);

        AppliedCouponResponse result = cartService.applyCoupon(VALID_SESSION_ID, "SAVE10");

        assertEquals("SAVE10", result.getCode());
        assertEquals(BigDecimal.valueOf(20.00), result.getDiscount());
        assertEquals(coupon, cart.getCoupon());
        assertEquals(BigDecimal.valueOf(20.00), cart.getTotalDiscount());
    }

    @Test
    void applyCoupon_invalidCoupon_throwsException() {
        cart.getItems().add(CartItem.builder().product(product1).quantity(2).cart(cart).build());

        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(couponService.validateCoupon(eq("INVALID"), isNull(), any(), any()))
                .thenThrow(new RuntimeException("Invalid coupon"));

        assertThrows(RuntimeException.class, () -> cartService.applyCoupon(VALID_SESSION_ID, "INVALID"));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void applyCoupon_invalidSessionId_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> cartService.applyCoupon(INVALID_SESSION_ID, "SAVE10"));
        verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(any());
    }

    /* ============================================== removeCoupon ============================================= */

    @Test
    void removeCoupon_withCoupon_removesCoupon() {
        cart.setCoupon(coupon);
        cart.setTotalDiscount(BigDecimal.TEN);

        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenReturn(cart);
        when(cartMapper.toResponse(any())).thenReturn(cartResponse);

        cartService.removeCoupon(VALID_SESSION_ID);

        assertNull(cart.getCoupon());
        assertEquals(BigDecimal.ZERO, cart.getTotalDiscount());
        verify(cartRepository).save(cart);
    }

    @Test
    void removeCoupon_noCoupon_doesNothing() {
        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenReturn(cart);
        when(cartMapper.toResponse(any())).thenReturn(cartResponse);

        cartService.removeCoupon(VALID_SESSION_ID);

        assertNull(cart.getCoupon());
        verify(cartRepository).save(cart);
    }

    @Test
    void removeCoupon_invalidSessionId_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> cartService.removeCoupon(INVALID_SESSION_ID));
        verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(any());
    }

    /* ========================================= calculateSubtotalPrice ======================================== */

    @Test
    void calculateSubtotalPrice_withItems_returnsCorrectSubtotal() {
        cart.getItems().add(CartItem.builder().product(product1).quantity(2).cart(cart).build());

        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));

        BigDecimal result = cartService.calculateSubtotalPrice(VALID_SESSION_ID);

        assertEquals(new BigDecimal("200.0"), result);
    }

    @Test
    void calculateSubtotalPrice_emptyCart_returnsZero() {
        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));

        BigDecimal result = cartService.calculateSubtotalPrice(VALID_SESSION_ID);

        assertEquals(BigDecimal.ZERO, result);
    }


    /* ====================================== calculateTotalShippingCost ====================================== */

    @Test
    void calculateTotalShippingCost_withItems_returnsCorrectShipping() {
        cart.getItems().add(CartItem.builder().product(product1).quantity(3).cart(cart).build());
        cart.getItems().add(CartItem.builder().product(product2).quantity(2).cart(cart).build());

        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));

        BigDecimal result = cartService.calculateTotalShippingCost(VALID_SESSION_ID);

        // product1: 10 + 1 + 1 = 12
        // product2: 15 + 3 = 18
        // total = 30
        assertEquals(new BigDecimal("30.0"), result);
    }

    @Test
    void calculateTotalShippingCost_emptyCart_returnsZero() {
        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));

        BigDecimal result = cartService.calculateTotalShippingCost(VALID_SESSION_ID);

        assertEquals(BigDecimal.ZERO, result);
    }

    /* =========================================== calculateDiscount =========================================== */

    @Test
    void calculateDiscount_withCoupon_returnsDiscount() {
        cart.getItems().add(CartItem.builder().product(product1).quantity(2).cart(cart).build());
        cart.setCoupon(coupon);

        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(couponService.calculateDiscount(eq(coupon), any(), any())).thenReturn(BigDecimal.valueOf(20.00));

        BigDecimal result = cartService.calculateDiscount(VALID_SESSION_ID);

        assertEquals(BigDecimal.valueOf(20.00), result);
    }

    @Test
    void calculateDiscount_noCoupon_returnsZero() {
        cart.getItems().add(CartItem.builder().product(product1).quantity(2).cart(cart).build());

        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));

        BigDecimal result = cartService.calculateDiscount(VALID_SESSION_ID);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateDiscount_noCoupon_returnsZero1() {
        cart.getItems().add(item);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));

        BigDecimal result = cartService.calculateDiscount(VALID_SESSION_ID);

        assertEquals(BigDecimal.ZERO, result);
        verify(couponService, never()).calculateDiscount(any(), any(), any());
    }
    /* ========================================== calculateTotalAmount ========================================= */

    @Test
    void calculateTotalAmount_withItemsAndCoupon_returnsCorrectTotal() {
        cart.getItems().add(CartItem.builder().product(product1).quantity(2).cart(cart).build());
        cart.setCoupon(coupon);

        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(couponService.calculateDiscount(any(), any(), any())).thenReturn(BigDecimal.valueOf(20.00));

        BigDecimal result = cartService.calculateTotalAmount(VALID_SESSION_ID);

        // subtotal: 200.00
        // shipping: 10 + 1 = 11.00
        // discount: 20.00
        // total: 200 + 11 - 20 = 191.00
        assertEquals(new BigDecimal("191.0"), result);
    }

    // === calculateTotalAmount ===

    @Test
    void calculateTotalAmount_emptyCart_returnsZero() {
        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));

        BigDecimal result = cartService.calculateTotalAmount(VALID_SESSION_ID);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateTotalAmount_invalidSessionId_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> cartService.calculateTotalAmount(INVALID_SESSION_ID));
        verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(any());
    }

    /* ============================================= cleanupStaleCarts ========================================= */

    @Test
    void cleanupStaleCarts_callsDeleteWithCorrectThreshold() {
        cartService.cleanupStaleCarts();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(cartRepository).deleteBySessionIdNotNullAndUserIdNullAndLastModifiedBefore(captor.capture());

        LocalDateTime threshold = captor.getValue();
        assertTrue(threshold.isBefore(LocalDateTime.now().minusDays(89)));
        assertTrue(threshold.isAfter(LocalDateTime.now().minusDays(91)));
    }

    @Test
    void cleanupStaleCarts_callsDeleteWithCorrectThreshold1() {
        cartService.cleanupStaleCarts();
        verify(cartRepository).deleteBySessionIdNotNullAndUserIdNullAndLastModifiedBefore(
                argThat(t -> t.isBefore(LocalDateTime.now().minusDays(89)))
        );
    }

    // === addToCart ===

    @Test
    void addToCart_newCart_createsAndAddsItem() {
        var request = new CartItemRequest(1L, 2);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        CartResponse result = cartService.addToCart(VALID_SESSION_ID, request);

        assertNotNull(result);
        verify(cartRepository).save(argThat(c -> c.getItems().size() == 1));
        verify(cartMapper).toResponse(any(Cart.class));
    }

    @Test
    void addToCart_existingItem_incrementsQuantity() {
        cart.getItems().add(CartItem.builder().product(product).quantity(3).build());
        var request = new CartItemRequest(1L, 2);

        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any())).thenReturn(cart);
        when(cartMapper.toResponse(any())).thenReturn(cartResponse);

        cartService.addToCart(VALID_SESSION_ID, request);

        assertEquals(5, cart.getItems().get(0).getQuantity());
    }

    @Test
    void addToCart_invalidSessionId_throwsBadRequest() {
        var request = new CartItemRequest(1L, 2);
        assertThrows(BadRequestException.class, () -> cartService.addToCart(INVALID_SESSION_ID, request));
        verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(any());
    }

    @Test
    void addToCart_insufficientStock_throws() {
        var request = new CartItemRequest(1L, 100);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(InsufficientStockException.class, () -> cartService.addToCart(VALID_SESSION_ID, request));
    }

    // === calculateDiscount ===

    @Test
    void calculateDiscount_withValidCoupon_returnsCalculatedDiscount() {
        cart.getItems().add(item);
        cart.setCoupon(coupon);

        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(couponService.calculateDiscount(eq(coupon), any(), eq(BigDecimal.valueOf(200.00))))
                .thenReturn(BigDecimal.valueOf(20.00)); // 10% of 200

        BigDecimal result = cartService.calculateDiscount(VALID_SESSION_ID);

        assertEquals(BigDecimal.valueOf(20.00), result);
        verify(couponService).calculateDiscount(any(), any(), any());
    }

    // === applyCoupon ===

    @Test
    void applyCoupon_validCode_appliesAndUpdatesTotals() {
        cart.getItems().add(item);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(couponService.validateCoupon(eq("SAVE10"), isNull(), any(), any())).thenReturn(coupon);
        when(couponService.calculateDiscount(eq(coupon), any(), any())).thenReturn(BigDecimal.valueOf(20.00));
        when(cartRepository.save(any())).thenReturn(cart);
        when(cartMapper.toResponse(any())).thenReturn(cartResponse);

        AppliedCouponResponse result = cartService.applyCoupon(VALID_SESSION_ID, "SAVE10");

        assertEquals(BigDecimal.valueOf(20.00), result.getDiscount());
        assertEquals(coupon, cart.getCoupon());
        assertEquals(BigDecimal.valueOf(20.00), cart.getTotalDiscount());
    }

    // === cleanupStaleCarts ===

//    @Test
//    void cleanupStaleCarts_callsDeleteWithCorrectThreshold() {
//        cartService.cleanupStaleCarts();
//        verify(cartRepository).deleteBySessionIdNotNullAndUserIdNullAndLastModifiedBefore(
//                argThat(t -> t.isBefore(LocalDateTime.now().minusDays(89)))
//        );
//    }

}

/*
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartMapper cartMapper;

    @Mock
    private ICouponService couponService;

    @InjectMocks
    private CartService cartService;

    private Cart cart;
    private Product product;
    private CartResponse cartResponse;
    private final String sessionId = "123e4567-e89b-12d3-a456-426614174000";

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(BigDecimal.valueOf(10.00))
                .quantity(10)
                .build();

        cart = Cart.builder()
                .id(1L)
                .sessionId(sessionId)
                .subtotalPrice(BigDecimal.ZERO)
                .totalDiscount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        cartResponse = CartResponse.builder().build();
    }

    // addToCart Tests
    @Test
    void addToCart_validRequestNewCart_createsCartAndAddsItem() {
        CartItemRequest cartItemRequest = new CartItemRequest(1L, 2);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.empty());
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);

        CartResponse result = cartService.addToCart(sessionId, cartItemRequest);

        assertNotNull(result);
        assertEquals(1, cart.getItems().size());
        assertEquals(2, cart.getItems().get(0).getQuantity());
        assertEquals(BigDecimal.valueOf(20.00), cart.getSubtotalPrice());
        verify(cartRepository).save(cart);
        verify(cartMapper).toResponse(cart);
    }

    @Test
    void addToCart_validRequestExistingCart_addsNewItem() {
        CartItemRequest cartItemRequest = new CartItemRequest(1L, 2);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);

        CartResponse result = cartService.addToCart(sessionId, cartItemRequest);

        assertNotNull(result);
        assertEquals(1, cart.getItems().size());
        assertEquals(2, cart.getItems().get(0).getQuantity());
        verify(cartRepository).save(cart);
        verify(cartMapper).toResponse(cart);
    }

    @Test
    void addToCart_existingItem_updatesQuantity() {
        CartItem existingItem = CartItem.builder()
                .id(1L)
                .product(product)
                .quantity(3)
                .cart(cart)
                .build();
        cart.getItems().add(existingItem);
        CartItemRequest cartItemRequest = new CartItemRequest(1L, 2);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);

        CartResponse result = cartService.addToCart(sessionId, cartItemRequest);

        assertNotNull(result);
        assertEquals(1, cart.getItems().size());
        assertEquals(5, cart.getItems().get(0).getQuantity());
        assertEquals(BigDecimal.valueOf(50.00), cart.getSubtotalPrice());
        verify(cartRepository).save(cart);
    }

    @Test
    void addToCart_nullProductId_throwsBadRequestException() {
        CartItemRequest cartItemRequest = new CartItemRequest(null, 2);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));

        assertThrows(BadRequestException.class, () -> cartService.addToCart(sessionId, cartItemRequest));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addToCart_negativeQuantity_throwsBadRequestException() {
        CartItemRequest cartItemRequest = new CartItemRequest(1L, -1);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));

        assertThrows(BadRequestException.class, () -> cartService.addToCart(sessionId, cartItemRequest));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addToCart_nonExistentProduct_throwsProductNotFoundException() {
        CartItemRequest cartItemRequest = new CartItemRequest(1L, 2);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> cartService.addToCart(sessionId, cartItemRequest));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addToCart_insufficientStock_throwsInsufficientStockException() {
        CartItemRequest cartItemRequest = new CartItemRequest(1L, 15);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(InsufficientStockException.class, () -> cartService.addToCart(sessionId, cartItemRequest));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addToCart_invalidSessionId_throwsBadRequestException() {
        CartItemRequest cartItemRequest = new CartItemRequest(1L, 2);
        assertThrows(BadRequestException.class, () -> cartService.addToCart("invalid-uuid", cartItemRequest));
        verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(any());
    }

    @Test
    void addToCart_nullSessionId_throwsBadRequestException() {
        CartItemRequest cartItemRequest = new CartItemRequest(1L, 2);
        assertThrows(BadRequestException.class, () -> cartService.addToCart(null, cartItemRequest));
        verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(any());
    }

    // updateCartItem Tests
    @Test
    void updateCartItem_validRequest_updatesQuantity() {
        CartItem cartItem = CartItem.builder()
                .id(1L)
                .product(product)
                .quantity(2)
                .cart(cart)
                .build();
        cart.getItems().add(cartItem);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse result = cartService.updateCartItem(sessionId, 1L, 5);

        assertNotNull(result);
        assertEquals(5, cart.getItems().get(0).getQuantity());
        assertEquals(BigDecimal.valueOf(50.00), cart.getSubtotalPrice());
        verify(cartRepository).save(cart);
        verify(cartMapper).toResponse(cart);
    }

    @Test
    void updateCartItem_zeroQuantity_removesItem() {
        CartItem cartItem = CartItem.builder()
                .id(1L)
                .product(product)
                .quantity(2)
                .cart(cart)
                .build();
        cart.getItems().add(cartItem);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse result = cartService.updateCartItem(sessionId, 1L, 0);

        assertNotNull(result);
        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository).save(cart);
        verify(cartMapper).toResponse(cart);
    }

    @Test
    void updateCartItem_nonExistentItem_throwsCartItemNotFoundException() {
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        assertThrows(CartItemNotFoundException.class, () -> cartService.updateCartItem(sessionId, 1L, 5));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateCartItem_nonExistentProduct_throwsProductNotFoundException() {
        CartItem cartItem = CartItem.builder()
                .id(1L)
                .product(product)
                .quantity(2)
                .cart(cart)
                .build();
        cart.getItems().add(cartItem);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> cartService.updateCartItem(sessionId, 1L, 5));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateCartItem_insufficientStock_throwsInsufficientStockException() {
        CartItem cartItem = CartItem.builder()
                .id(1L)
                .product(product)
                .quantity(2)
                .cart(cart)
                .build();
        cart.getItems().add(cartItem);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(InsufficientStockException.class, () -> cartService.updateCartItem(sessionId, 1L, 15));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateCartItem_invalidSessionId_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> cartService.updateCartItem("invalid-uuid", 1L, 5));
        verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(any());
    }

    // removeFromCart Tests
    @Test
    void removeFromCart_validItemId_removesItem() {
        CartItem cartItem = CartItem.builder()
                .id(1L)
                .product(product)
                .quantity(2)
                .cart(cart)
                .build();
        cart.getItems().add(cartItem);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse result = cartService.removeFromCart(sessionId, 1L);

        assertNotNull(result);
        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository).save(cart);
        verify(cartMapper).toResponse(cart);
    }

    @Test
    void removeFromCart_nonExistentItem_throwsCartItemNotFoundException() {
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        assertThrows(CartItemNotFoundException.class, () -> cartService.removeFromCart(sessionId, 1L));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void removeFromCart_invalidSessionId_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> cartService.removeFromCart("invalid-uuid", 1L));
        verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(any());
    }

    // clearCart Tests
    @Test
    void clearCart_emptyCart_savesEmptyCart() {
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse result = cartService.clearCart(sessionId);

        assertNotNull(result);
        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository).save(cart);
        verify(cartMapper).toResponse(cart);
    }

    @Test
    void clearCart_withItems_clearsItemsAndSaves() {
        CartItem cartItem = CartItem.builder()
                .id(1L)
                .product(product)
                .quantity(2)
                .cart(cart)
                .build();
        cart.getItems().add(cartItem);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse result = cartService.clearCart(sessionId);

        assertNotNull(result);
        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository).save(cart);
        verify(cartMapper).toResponse(cart);
    }

    @Test
    void clearCart_invalidSessionId_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> cartService.clearCart("invalid-uuid"));
        verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(any());
    }

    // getCart Tests
    @Test
    void getCart_existingCart_returnsCartResponse() {
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(cart, PageRequest.of(0, 10))).thenReturn(cartResponse);

        CartResponse result = cartService.getCart(sessionId, PageRequest.of(0, 10));

        assertNotNull(result);
        verify(cartMapper).toResponse(cart, PageRequest.of(0, 10));
    }

    @Test
    void getCart_newCart_createsAndReturnsCart() {
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.toResponse(cart, PageRequest.of(0, 10))).thenReturn(cartResponse);

        CartResponse result = cartService.getCart(sessionId, PageRequest.of(0, 10));

        assertNotNull(result);
        verify(cartRepository).save(any(Cart.class));
        verify(cartMapper).toResponse(cart, PageRequest.of(0, 10));
    }

    @Test
    void getCart_invalidSessionId_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> cartService.getCart("invalid-uuid", PageRequest.of(0, 10)));
        verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(any());
    }

    // applyCoupon Tests
    @Test
    void applyCoupon_validCoupon_appliesCoupon() {
        Coupon coupon = Coupon.builder().id(1L).code("SAVE10").build();
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        when(couponService.validateCoupon(eq("SAVE10"), isNull(), any(BigDecimal.class), any())).thenReturn(coupon);
        when(couponService.calculateDiscount(coupon, cart.getItems(), BigDecimal.ZERO)).thenReturn(BigDecimal.TEN);
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        AppliedCouponResponse result = cartService.applyCoupon(sessionId, "SAVE10");

        assertNotNull(result);
        assertEquals(BigDecimal.TEN, result.getDiscount());
        assertEquals(coupon, cart.getCoupon());
        verify(cartRepository).save(cart);
        verify(cartMapper).toResponse(cart);
    }

    @Test
    void applyCoupon_invalidCoupon_throwsException() {
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        when(couponService.validateCoupon(eq("INVALID"), isNull(), any(BigDecimal.class), any()))
                .thenThrow(new RuntimeException("Invalid coupon"));

        assertThrows(RuntimeException.class, () -> cartService.applyCoupon(sessionId, "INVALID"));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void applyCoupon_invalidSessionId_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> cartService.applyCoupon("invalid-uuid", "SAVE10"));
        verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(any());
    }

    // removeCoupon Tests
    @Test
    void removeCoupon_withCoupon_removesCoupon() {
        cart.setCoupon(Coupon.builder().id(1L).code("SAVE10").build());
        cart.setTotalDiscount(BigDecimal.TEN);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse result = cartService.removeCoupon(sessionId);

        assertNotNull(result);
        assertNull(cart.getCoupon());
        assertEquals(BigDecimal.ZERO, cart.getTotalDiscount());
        verify(cartRepository).save(cart);
        verify(cartMapper).toResponse(cart);
    }

    @Test
    void removeCoupon_noCoupon_doesNothing() {
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse result = cartService.removeCoupon(sessionId);

        assertNotNull(result);
        assertNull(cart.getCoupon());
        verify(cartRepository).save(cart);
        verify(cartMapper).toResponse(cart);
    }

    @Test
    void removeCoupon_invalidSessionId_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> cartService.removeCoupon("invalid-uuid"));
        verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(any());
    }

    // calculateTotalAmount Tests
    @Test
    void calculateTotalAmount_withItems_returnsTotal() {
        CartItem cartItem = CartItem.builder()
                .id(1L)
                .product(product)
                .quantity(2)
                .cart(cart)
                .build();
        cart.getItems().add(cartItem);
        cart.setSubtotalPrice(BigDecimal.valueOf(20.00));
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));

        BigDecimal result = cartService.calculateTotalAmount(sessionId);

        assertEquals(BigDecimal.valueOf(20.00), result);
    }

    @Test
    void calculateTotalAmount_emptyCart_returnsZero() {
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));

        BigDecimal result = cartService.calculateTotalAmount(sessionId);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateTotalAmount_invalidSessionId_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> cartService.calculateTotalAmount("invalid-uuid"));
        verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(any());
    }

    // calculateDiscount Tests
    @Test
    void calculateDiscount_withCoupon_returnsDiscount() {
        cart.setTotalDiscount(BigDecimal.TEN);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));

        BigDecimal result = cartService.calculateDiscount(sessionId);

        assertEquals(BigDecimal.TEN, result);
    }

    @Test
    void calculateDiscount_noCoupon_returnsZero() {
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));

        BigDecimal result = cartService.calculateDiscount(sessionId);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateDiscount_invalidSessionId_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> cartService.calculateDiscount("invalid-uuid"));
        verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(any());
    }

    // cleanupStaleCarts Tests
    @Test
    void cleanupStaleCarts_deletesOldCarts() {
        cartService.cleanupStaleCarts();
        verify(cartRepository).deleteBySessionIdNotNullAndUserIdNullAndLastModifiedBefore(any(LocalDateTime.class));
    }
}*/


/*import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.CartItemNotFoundException;
import com.datasaz.ecommerce.exceptions.InsufficientStockException;
import com.datasaz.ecommerce.exceptions.ProductNotFoundException;
import com.datasaz.ecommerce.mappers.CartMapper;
import com.datasaz.ecommerce.models.request.CartRequest;
import com.datasaz.ecommerce.models.response.AppliedCouponResponse;
import com.datasaz.ecommerce.models.response.CartResponse;
import com.datasaz.ecommerce.repositories.CartRepository;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.entities.Cart;
import com.datasaz.ecommerce.repositories.entities.CartItem;
import com.datasaz.ecommerce.repositories.entities.Coupon;
import com.datasaz.ecommerce.repositories.entities.Product;
import com.datasaz.ecommerce.services.interfaces.ICouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartMapper cartMapper;

    @Mock
    private ICouponService couponService;

    @InjectMocks
    private CartService cartService;

    private Cart cart;
    private Product product;
    private CartResponse cartResponse;
    private final String sessionId = "123e4567-e89b-12d3-a456-426614174000";

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(BigDecimal.valueOf(10.00))
                .quantity(10)
                .build();

        cart = Cart.builder()
                .id(1L)
                .sessionId(sessionId)
                .totalPrice(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        cartResponse = CartResponse.builder().build();
    }

    @Test
    void addToCart_validRequest_addsItemAndSavesCart() {
        CartRequest cartRequest = new CartRequest(1L, 2);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse result = cartService.addToCart(sessionId, cartRequest);

        assertNotNull(result);
        assertEquals(1, cart.getItems().size());
        assertEquals(2, cart.getItems().get(0).getQuantity());
        assertEquals(BigDecimal.valueOf(20.00), cart.getTotalPrice());
        verify(cartRepository).save(cart);
        verify(cartMapper).toResponse(cart);
    }

    @Test
    void addToCart_invalidProductId_throwsProductNotFoundException() {
        CartRequest cartRequest = new CartRequest(1L, 2);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> cartService.addToCart(sessionId, cartRequest));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addToCart_insufficientStock_throwsInsufficientStockException() {
        CartRequest cartRequest = new CartRequest(1L, 15);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(InsufficientStockException.class, () -> cartService.addToCart(sessionId, cartRequest));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addToCart_invalidSessionId_throwsBadRequestException() {
        CartRequest cartRequest = new CartRequest(1L, 2);
        assertThrows(BadRequestException.class, () -> cartService.addToCart("invalid-uuid", cartRequest));
        verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(any());
    }

    @Test
    void updateCartItem_validRequest_updatesQuantity() {
        CartItem cartItem = CartItem.builder().id(1L).product(product).quantity(2).price(product.getPrice()).productName(product.getName()).cart(cart).build();
        cart.getItems().add(cartItem);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse result = cartService.updateCartItem(sessionId, 1L, 5);

        assertNotNull(result);
        assertEquals(5, cart.getItems().get(0).getQuantity());
        assertEquals(BigDecimal.valueOf(50.00), cart.getTotalPrice());
        verify(cartRepository).save(cart);
        verify(cartMapper).toResponse(cart);
    }

    @Test
    void updateCartItem_nonExistentItem_throwsCartItemNotFoundException() {
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        assertThrows(CartItemNotFoundException.class, () -> cartService.updateCartItem(sessionId, 1L, 5));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void removeFromCart_validItemId_removesItem() {
        CartItem cartItem = CartItem.builder().id(1L).product(product).quantity(2).price(product.getPrice()).productName(product.getName()).cart(cart).build();
        cart.getItems().add(cartItem);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse result = cartService.removeFromCart(sessionId, 1L);

        assertNotNull(result);
        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository).save(cart);
        verify(cartMapper).toResponse(cart);
    }

    @Test
    void clearCart_clearsItemsAndSaves() {
        CartItem cartItem = CartItem.builder().id(1L).product(product).quantity(2).price(product.getPrice()).productName(product.getName()).cart(cart).build();
        cart.getItems().add(cartItem);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse result = cartService.clearCart(sessionId);

        assertNotNull(result);
        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository).save(cart);
        verify(cartMapper).toResponse(cart);
    }

    @Test
    void applyCoupon_validCoupon_appliesCoupon() {
        Coupon coupon = Coupon.builder().id(1L).code("SAVE10").build();
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        when(couponService.validateCoupon(eq("SAVE10"), isNull(), any(BigDecimal.class), any())).thenReturn(coupon);
        when(couponService.calculateDiscount(coupon, cart.getItems(), BigDecimal.ZERO)).thenReturn(BigDecimal.TEN);
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        AppliedCouponResponse result = cartService.applyCoupon(sessionId, "SAVE10");

        assertNotNull(result);
        assertEquals(BigDecimal.TEN, result.getDiscount());
        assertEquals(coupon, cart.getCoupon());
        verify(cartRepository).save(cart);
        verify(cartMapper).toResponse(cart);
    }

    @Test
    void removeCoupon_removesCoupon() {
        cart.setCoupon(Coupon.builder().id(1L).code("SAVE10").build());
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse result = cartService.removeCoupon(sessionId);

        assertNotNull(result);
        assertNull(cart.getCoupon());
        assertEquals(BigDecimal.ZERO, cart.getDiscount());
        verify(cartRepository).save(cart);
        verify(cartMapper).toResponse(cart);
    }

    @Test
    void getCart_returnsCartResponse() {
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(cart, PageRequest.of(0, 10))).thenReturn(cartResponse);

        CartResponse result = cartService.getCart(sessionId, PageRequest.of(0, 10));

        assertNotNull(result);
        verify(cartMapper).toResponse(cart, PageRequest.of(0, 10));
    }

    @Test
    void calculateTotalAmount_returnsTotal() {
        cart.setTotalPrice(BigDecimal.valueOf(50.00));
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));

        BigDecimal result = cartService.calculateTotalAmount(sessionId);

        assertEquals(BigDecimal.valueOf(50.00), result);
    }

    @Test
    void calculateDiscount_returnsDiscount() {
        cart.setDiscount(BigDecimal.TEN);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)).thenReturn(Optional.of(cart));

        BigDecimal result = cartService.calculateDiscount(sessionId);

        assertEquals(BigDecimal.TEN, result);
    }

    @Test
    void cleanupStaleCarts_deletesOldCarts() {
        cartService.cleanupStaleCarts();
        verify(cartRepository).deleteBySessionIdNotNullAndLastModifiedBefore(any(LocalDateTime.class));
    }
}*/

/*

import com.datasaz.ecommerce.exceptions.*;
import com.datasaz.ecommerce.mappers.CartMapper;
import com.datasaz.ecommerce.models.request.CartRequest;
import com.datasaz.ecommerce.models.response.AppliedCouponResponse;
import com.datasaz.ecommerce.models.response.CartItemResponse;
import com.datasaz.ecommerce.models.response.CartResponse;
import com.datasaz.ecommerce.repositories.CartRepository;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.*;
import com.datasaz.ecommerce.services.interfaces.ICouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartMapper cartMapper;

    @Mock
    private ICouponService couponService;

    @InjectMocks
    private CartService cartService;

    private Cart sessionCart;
    private Cart userCart;
    private Product product;
    private User user;
    private Company company;
    private Coupon coupon;
    private CartItem cartItem;
    private CartResponse sessionCartResponse;
    private CartResponse userCartResponse;
    private static final String VALID_SESSION_ID = "eafa58da-f94b-4e68-90c5-169a7cd1b1c1";
    private static final Long VALID_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        // Reset mocks to avoid test interference
        Mockito.reset(cartRepository, productRepository, userRepository, cartMapper, couponService);

        company = new Company();
        company.setId(1L);
        company.setName("Test Company");

        user = new User();
        user.setId(VALID_USER_ID);
        user.setEmailAddress("buyer@test.com");
        user.setCompany(company);

        product = new Product();
        product.setId(1L);
        product.setPrice(new BigDecimal("99.99"));
        product.setName("Test Product");
        product.setQuantity(10);
        product.setAuthor(user);
        product.setCompany(company);

        cartItem = CartItem.builder()
                .id(1L)
                .product(product)
                .productName(product.getName())
                .price(product.getPrice())
                .quantity(2)
                .cart(null) // Set later to avoid circular reference
                .build();

        sessionCart = Cart.builder()
                .id(1L)
                .sessionId(VALID_SESSION_ID)
                .user(null)
                .items(new ArrayList<>())
                .totalPrice(new BigDecimal("199.98"))
                .discount(BigDecimal.ZERO)
                .lastModified(LocalDateTime.now())
                .version(1L)
                .build();
        cartItem.setCart(sessionCart);
        sessionCart.getItems().add(cartItem);

        userCart = Cart.builder()
                .id(2L)
                .sessionId(null)
                .user(user)
                .items(new ArrayList<>())
                .totalPrice(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .lastModified(LocalDateTime.now())
                .version(1L)
                .build();

        coupon = Coupon.builder()
                .code("SAVE10")
                .state(CouponState.ACTIVE)
                .category(CouponCategory.GENERAL)
                .couponScope(CouponScope.ORDER)
                .couponType(CouponType.PERCENTAGE)
                .discountPercentage(new BigDecimal("10"))
                .startFrom(LocalDateTime.now().minusDays(1))
                .endAt(LocalDateTime.now().plusDays(1))
                .maxUsesPerUser(1)
                .maxUses(10)
                .author(user)
                .couponTrackings(new HashSet<>())
                .version(1L)
                .build();

        sessionCartResponse = CartResponse.builder()
                .id(1L)
                .sessionId(VALID_SESSION_ID)
                .userId(null)
                .items(List.of(CartItemResponse.builder()
                        .id(1L)
                        .productId(1L)
                        .productName("Test Product")
                        .price(new BigDecimal("99.99"))
                        .quantity(2)
                        .build()))
                .subtotal(new BigDecimal("199.98"))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("199.98"))
                .build();

        userCartResponse = CartResponse.builder()
                .id(2L)
                .sessionId(null)
                .userId(user.getId())
                .items(new ArrayList<>())
                .subtotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();
    }

    @Nested
    class AddToCartTests {
        @Test
        void addToCart_newCart_success() {
            CartRequest cartRequest = CartRequest.builder()
                    .productId(1L)
                    .quantity(1)
                    .build();
            Cart newCart = Cart.builder()
                    .id(3L)
                    .sessionId(VALID_SESSION_ID)
                    .items(new ArrayList<>())
                    .totalPrice(BigDecimal.ZERO)
                    .discount(BigDecimal.ZERO)
                    .lastModified(LocalDateTime.now())
                    .version(1L)
                    .build();
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(cartRepository.save(any(Cart.class))).thenReturn(newCart).thenReturn(newCart);
            when(cartMapper.toResponse(any(Cart.class))).thenReturn(sessionCartResponse);

            CartResponse response = cartService.addToCart(VALID_SESSION_ID, cartRequest);

            assertNotNull(response);
            assertEquals(VALID_SESSION_ID, response.getSessionId());
            assertNull(response.getUserId());
            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
            verify(cartRepository, times(2)).save(any(Cart.class));
            verify(productRepository).findById(1L);
            verify(cartMapper).toResponse(any(Cart.class));
        }

        @Test
        void addToCart_existingCart_success() {
            CartRequest cartRequest = CartRequest.builder()
                    .productId(1L)
                    .quantity(1)
                    .build();
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(cartRepository.save(any(Cart.class))).thenReturn(sessionCart);
            when(cartMapper.toResponse(sessionCart)).thenReturn(sessionCartResponse);

            CartResponse response = cartService.addToCart(VALID_SESSION_ID, cartRequest);

            assertNotNull(response);
            assertEquals(VALID_SESSION_ID, response.getSessionId());
            assertNull(response.getUserId());
            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
            verify(cartRepository).save(sessionCart);
            verify(productRepository).findById(1L);
            verify(cartMapper).toResponse(sessionCart);
        }

        @Test
        void addToCart_invalidSessionId_throwsBadRequestException() {
            CartRequest cartRequest = CartRequest.builder()
                    .productId(1L)
                    .quantity(1)
                    .build();
            assertThrows(BadRequestException.class,
                    () -> cartService.addToCart("invalid-session", cartRequest));
            verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(anyString());
            verify(cartRepository, never()).save(any(Cart.class));
            verify(productRepository, never()).findById(anyLong());
        }

        @Test
        void addToCart_productNotFound_throwsProductNotFoundException() {
            CartRequest cartRequest = CartRequest.builder()
                    .productId(1L)
                    .quantity(1)
                    .build();
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
            when(productRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ProductNotFoundException.class,
                    () -> cartService.addToCart(VALID_SESSION_ID, cartRequest));
            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
            verify(cartRepository, never()).save(any(Cart.class));
            verify(productRepository).findById(1L);
        }

        @Test
        void addToCart_insufficientStock_throwsInsufficientStockException() {
            Cart emptyCart = Cart.builder()
                    .id(1L)
                    .sessionId(VALID_SESSION_ID)
                    .user(null)
                    .items(new ArrayList<>())
                    .totalPrice(BigDecimal.ZERO)
                    .discount(BigDecimal.ZERO)
                    .lastModified(LocalDateTime.now())
                    .version(1L)
                    .build();
            CartRequest cartRequest = CartRequest.builder()
                    .productId(1L)
                    .quantity(17)
                    .build();
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(emptyCart));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            InsufficientStockException exception = assertThrows(InsufficientStockException.class,
                    () -> cartService.addToCart(VALID_SESSION_ID, cartRequest));

            assertEquals("Insufficient stock for product ID: 1, requested: 17, available: 10", exception.getMessage());
            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
            verify(cartRepository, never()).save(any(Cart.class));
            verify(productRepository).findById(1L);
        }
    }

    @Nested
    class UpdateCartTests {
        @Test
        void updateCart_validItem_success() {
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(cartRepository.save(sessionCart)).thenReturn(sessionCart);
            when(cartMapper.toResponse(sessionCart)).thenReturn(sessionCartResponse);

            CartResponse response = cartService.updateCartItem(VALID_SESSION_ID, 1L, 5);

            assertNotNull(response);
            assertEquals(VALID_SESSION_ID, response.getSessionId());
            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
            verify(cartRepository).save(sessionCart);
            verify(productRepository).findById(1L);
            verify(cartMapper).toResponse(sessionCart);
        }

        @Test
        void updateCart_removeItem_success() {
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(cartRepository.save(sessionCart)).thenReturn(sessionCart);
            when(cartMapper.toResponse(sessionCart)).thenReturn(sessionCartResponse);

            CartResponse response = cartService.updateCartItem(VALID_SESSION_ID, 1L, 0);

            assertNotNull(response);
            assertEquals(VALID_SESSION_ID, response.getSessionId());
            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
            verify(cartRepository).save(sessionCart);
            verify(productRepository).findById(1L);
            verify(cartMapper).toResponse(sessionCart);
        }

        @Test
        void updateCart_cartNotFound_throwsCartNotFoundException() {
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());

            assertThrows(CartNotFoundException.class,
                    () -> cartService.updateCartItem(VALID_SESSION_ID, 1L, 5));
            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
            verify(cartRepository, never()).save(any(Cart.class));
            verify(productRepository, never()).findById(anyLong());
        }

        @Test
        void updateCart_invalidSessionId_throwsBadRequestException() {
            assertThrows(BadRequestException.class,
                    () -> cartService.updateCartItem("invalid-session", 1L, 5));
            verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(anyString());
            verify(cartRepository, never()).save(any(Cart.class));
            verify(productRepository, never()).findById(anyLong());
        }

        @Test
        void updateCart_cartItemNotFound_throwsCartItemNotFoundException() {
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));

            assertThrows(CartItemNotFoundException.class,
                    () -> cartService.updateCartItem(VALID_SESSION_ID, 999L, 5));
            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
            verify(cartRepository, never()).save(any(Cart.class));
            verify(productRepository, never()).findById(anyLong());
        }

        @Test
        void updateCart_productNotFound_throwsProductNotFoundException() {
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
            when(productRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ProductNotFoundException.class,
                    () -> cartService.updateCartItem(VALID_SESSION_ID, 1L, 5));
            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
            verify(cartRepository, never()).save(any(Cart.class));
            verify(productRepository).findById(1L);
        }

        @Test
        void updateCart_insufficientStock_throwsInsufficientStockException() {
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            InsufficientStockException exception = assertThrows(InsufficientStockException.class,
                    () -> cartService.updateCartItem(VALID_SESSION_ID, 1L, 15));

            assertEquals("Insufficient stock for product ID: 1, requested: 15, available: 10", exception.getMessage());
            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
            verify(cartRepository, never()).save(any(Cart.class));
            verify(productRepository).findById(1L);
        }
    }

    @Nested
    class RemoveFromCartTests {
        @Test
        void removeFromCart_validItem_success() {
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
            when(cartRepository.save(sessionCart)).thenReturn(sessionCart);
            when(cartMapper.toResponse(sessionCart)).thenReturn(sessionCartResponse);

            CartResponse response = cartService.removeFromCart(VALID_SESSION_ID, 1L);

            assertNotNull(response);
            assertEquals(VALID_SESSION_ID, response.getSessionId());
            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
            verify(cartRepository).save(sessionCart);
            verify(cartMapper).toResponse(sessionCart);
        }

        @Test
        void removeFromCart_cartNotFound_throwsCartNotFoundException() {
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());

            assertThrows(CartNotFoundException.class,
                    () -> cartService.removeFromCart(VALID_SESSION_ID, 1L));
            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
            verify(cartRepository, never()).save(any(Cart.class));
        }

        @Test
        void removeFromCart_invalidSessionId_throwsBadRequestException() {
            assertThrows(BadRequestException.class,
                    () -> cartService.removeFromCart("invalid-session", 1L));
            verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(anyString());
            verify(cartRepository, never()).save(any(Cart.class));
        }

        @Test
        void removeFromCart_cartItemNotFound_throwsCartItemNotFoundException() {
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));

            assertThrows(CartItemNotFoundException.class,
                    () -> cartService.removeFromCart(VALID_SESSION_ID, 999L));
            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
            verify(cartRepository, never()).save(any(Cart.class));
        }
    }

//    @Nested
//    class MergeCartOnLoginTests {
//        @Test
//        void mergeCartOnLogin_validSessionId_success() {
//            when(userRepository.findById(VALID_USER_ID)).thenReturn(Optional.of(user));
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//            when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
//            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//            when(cartRepository.save(userCart)).thenReturn(userCart);
//            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
//
//            CartResponse response = cartService.mergeCartOnLogin(VALID_SESSION_ID, VALID_USER_ID);
//
//            assertNotNull(response);
//            assertNull(response.getSessionId());
//            assertEquals(user.getId(), response.getUserId());
//            verify(userRepository).findById(VALID_USER_ID);
//            verify(cartRepository).findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID);
//            verify(cartRepository).findByUserWithItems(user);
//            verify(cartRepository).save(userCart);
//            verify(cartRepository).delete(sessionCart);
//            verify(cartMapper).toResponse(userCart);
//        }
//
//        @Test
//        void mergeCartOnLogin_noAnonymousCart_success() {
//            when(userRepository.findById(VALID_USER_ID)).thenReturn(Optional.of(user));
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//            when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.empty());
//            when(cartRepository.save(userCart)).thenReturn(userCart);
//            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
//
//            CartResponse response = cartService.mergeCartOnLogin(VALID_SESSION_ID, VALID_USER_ID);
//
//            assertNotNull(response);
//            assertNull(response.getSessionId());
//            assertEquals(user.getId(), response.getUserId());
//            verify(userRepository).findById(VALID_USER_ID);
//            verify(cartRepository).findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID);
//            verify(cartRepository).findByUserWithItems(user);
//            verify(cartRepository).save(userCart);
//            verify(cartRepository, never()).delete(any(Cart.class));
//            verify(cartMapper).toResponse(userCart);
//        }
//
//        @Test
//        void mergeCartOnLogin_invalidSessionId_throwsBadRequestException() {
//            assertThrows(BadRequestException.class,
//                    () -> cartService.mergeCartOnLogin("invalid-session", VALID_USER_ID));
//            verify(cartRepository, never()).findBySessionIdWithItemsAndCoupon(anyString());
//            verify(cartRepository, never()).save(any(Cart.class));
//            verify(userRepository, never()).findById(anyLong());
//        }
//
//        @Test
//        void mergeCartOnLogin_userNotFound_throwsUserNotFoundException() {
//            when(userRepository.findById(VALID_USER_ID)).thenReturn(Optional.empty());
//
//            assertThrows(UserNotFoundException.class,
//                    () -> cartService.mergeCartOnLogin(VALID_SESSION_ID, VALID_USER_ID));
//            verify(userRepository).findById(VALID_USER_ID);
//            verify(cartRepository, never()).findBySessionIdWithItemsAndCoupon(anyString());
//            verify(cartRepository, never()).save(any(Cart.class));
//        }
//
//        @Test
//        void mergeCartOnLogin_insufficientStock_skipsItem() {
//            Product lowStockProduct = new Product();
//            lowStockProduct.setId(1L);
//            lowStockProduct.setPrice(new BigDecimal("99.99"));
//            lowStockProduct.setName("Test Product");
//            lowStockProduct.setQuantity(1);
//            lowStockProduct.setAuthor(user);
//            lowStockProduct.setCompany(company);
//
//            CartItem highQuantityItem = CartItem.builder()
//                    .id(1L)
//                    .product(lowStockProduct)
//                    .productName(lowStockProduct.getName())
//                    .price(lowStockProduct.getPrice())
//                    .quantity(5)
//                    .cart(sessionCart)
//                    .build();
//            sessionCart.getItems().clear();
//            sessionCart.getItems().add(highQuantityItem);
//
//            when(userRepository.findById(VALID_USER_ID)).thenReturn(Optional.of(user));
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//            when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
//            when(productRepository.findById(1L)).thenReturn(Optional.of(lowStockProduct));
//            when(cartRepository.save(userCart)).thenReturn(userCart);
//            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
//
//            CartResponse response = cartService.mergeCartOnLogin(VALID_SESSION_ID, VALID_USER_ID);
//
//            assertNotNull(response);
//            assertEquals(user.getId(), response.getUserId());
//            verify(userRepository).findById(VALID_USER_ID);
//            verify(cartRepository).findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID);
//            verify(cartRepository).findByUserWithItems(user);
//            verify(cartRepository).save(userCart);
//            verify(cartRepository).delete(sessionCart);
//            verify(cartMapper).toResponse(userCart);
//        }
//    }

    @Nested
    class GetCartTests {
        @Test
        void getCart_validSessionId_success() {
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
            when(cartMapper.toResponse(eq(sessionCart), any(Pageable.class))).thenReturn(sessionCartResponse);

            CartResponse response = cartService.getCart(VALID_SESSION_ID, mock(Pageable.class));

            assertNotNull(response);
            assertEquals(VALID_SESSION_ID, response.getSessionId());
            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
            verify(cartMapper).toResponse(eq(sessionCart), any(Pageable.class));
        }

        @Test
        void getCart_invalidSessionId_throwsBadRequestException() {
            assertThrows(BadRequestException.class,
                    () -> cartService.getCart("invalid-session", mock(Pageable.class)));
            verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(anyString());
        }

        @Test
        void getCart_cartNotFound_throwsCartNotFoundException() {
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());

            assertThrows(CartNotFoundException.class,
                    () -> cartService.getCart(VALID_SESSION_ID, mock(Pageable.class)));
            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
        }
    }

//    @Nested
//    class GetCartByUsersIdTests {
//        @Test
//        void getCartByUsersId_validUserId_success() {
//            when(userRepository.findById(VALID_USER_ID)).thenReturn(Optional.of(user));
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
//
//            CartResponse response = cartService.getCartByUsersId(VALID_USER_ID);
//
//            assertNotNull(response);
//            assertEquals(user.getId(), response.getUserId());
//            verify(userRepository).findById(VALID_USER_ID);
//            verify(cartRepository).findByUserWithItems(user);
//            verify(cartMapper).toResponse(userCart);
//        }
//
//        @Test
//        void getCartByUsersId_userNotFound_throwsUserNotFoundException() {
//            when(userRepository.findById(VALID_USER_ID)).thenReturn(Optional.empty());
//
//            assertThrows(UserNotFoundException.class,
//                    () -> cartService.getCartByUsersId(VALID_USER_ID));
//            verify(userRepository).findById(VALID_USER_ID);
//            verify(cartRepository, never()).findByUserWithItems(any(User.class));
//        }
//
//        @Test
//        void getCartByUsersId_cartNotFound_throwsCartNotFoundException() {
//            when(userRepository.findById(VALID_USER_ID)).thenReturn(Optional.of(user));
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.empty());
//
//            assertThrows(CartNotFoundException.class,
//                    () -> cartService.getCartByUsersId(VALID_USER_ID));
//            verify(userRepository).findById(VALID_USER_ID);
//            verify(cartRepository).findByUserWithItems(user);
//        }
//    }

    @Nested
    class ClearCartTests {
        @Test
        void clearCart_validSessionId_success() {
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
            when(cartRepository.save(sessionCart)).thenReturn(sessionCart);

            cartService.clearCart(VALID_SESSION_ID);

            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
            verify(cartRepository).save(sessionCart);
            assertTrue(sessionCart.getItems().isEmpty());
        }

        @Test
        void clearCart_invalidSessionId_throwsBadRequestException() {
            assertThrows(BadRequestException.class,
                    () -> cartService.clearCart("invalid-session"));
            verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(anyString());
            verify(cartRepository, never()).save(any(Cart.class));
        }

        @Test
        void clearCart_cartNotFound_throwsCartNotFoundException() {
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());

            assertThrows(CartNotFoundException.class,
                    () -> cartService.clearCart(VALID_SESSION_ID));
            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
            verify(cartRepository, never()).save(any(Cart.class));
        }
    }

    @Nested
    class ApplyCouponTests {
        @Test
        void applyCoupon_validCoupon_success() {
            when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
            when(couponService.validateCoupon(eq("SAVE10"), isNull(), eq(new BigDecimal("199.98")), eq(sessionCart.getItems()))).thenReturn(coupon);
            when(couponService.calculateDiscount(eq(coupon), eq(sessionCart.getItems()), eq(new BigDecimal("199.98")))).thenReturn(new BigDecimal("20.00"));
            when(cartRepository.save(sessionCart)).thenReturn(sessionCart);
            when(cartMapper.toResponse(sessionCart)).thenReturn(sessionCartResponse);

            AppliedCouponResponse response = cartService.applyCoupon(VALID_SESSION_ID, "SAVE10");

            assertNotNull(response);
            assertEquals(new BigDecimal("20.00"), response.getDiscount());
            assertNotNull(response.getCartResponse());
            verify(cartRepository).findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID);
            verify(couponService).validateCoupon(eq("SAVE10"), isNull(), eq(new BigDecimal("199.98")), eq(sessionCart.getItems()));
            verify(couponService).calculateDiscount(eq(coupon), eq(sessionCart.getItems()), eq(new BigDecimal("199.98")));
            verify(cartRepository).save(sessionCart);
            verify(cartMapper).toResponse(sessionCart);
        }

        @Test
        void applyCoupon_cartNotFound_throwsCartNotFoundException() {
            when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.empty());

            assertThrows(CartNotFoundException.class,
                    () -> cartService.applyCoupon(VALID_SESSION_ID, "SAVE10"));
            verify(cartRepository).findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID);
            verify(cartRepository, never()).save(any(Cart.class));
            verify(couponService, never()).validateCoupon(anyString(), any(), any(), any());
        }

        @Test
        void applyCoupon_invalidCoupon_throwsBadRequestException() {
            when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
            when(couponService.validateCoupon(eq("INVALID"), isNull(), eq(new BigDecimal("199.98")), eq(sessionCart.getItems())))
                    .thenThrow(BadRequestException.builder().message("Invalid coupon code").build());

            assertThrows(BadRequestException.class,
                    () -> cartService.applyCoupon(VALID_SESSION_ID, "INVALID"));
            verify(cartRepository).findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID);
            verify(couponService).validateCoupon(eq("INVALID"), isNull(), eq(new BigDecimal("199.98")), eq(sessionCart.getItems()));
            verify(cartRepository, never()).save(any(Cart.class));
        }
    }

    @Nested
    class RemoveCouponTests {
        @Test
        void removeCoupon_validSessionId_success() {
            sessionCart.setCoupon(coupon);
            when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
            when(cartRepository.save(sessionCart)).thenReturn(sessionCart);
            when(cartMapper.toResponse(sessionCart)).thenReturn(sessionCartResponse);

            CartResponse response = cartService.removeCoupon(VALID_SESSION_ID);

            assertNotNull(response);
            assertEquals(VALID_SESSION_ID, response.getSessionId());
            assertNull(sessionCart.getCoupon());
            verify(cartRepository).findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID);
            verify(cartRepository).save(sessionCart);
            verify(cartMapper).toResponse(sessionCart);
        }

        @Test
        void removeCoupon_cartNotFound_throwsCartNotFoundException() {
            when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.empty());

            assertThrows(CartNotFoundException.class,
                    () -> cartService.removeCoupon(VALID_SESSION_ID));
            verify(cartRepository).findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID);
            verify(cartRepository, never()).save(any(Cart.class));
        }

        @Test
        void removeCoupon_noCoupon_success() {
            sessionCart.setCoupon(null);
            when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
            when(cartRepository.save(sessionCart)).thenReturn(sessionCart);
            when(cartMapper.toResponse(sessionCart)).thenReturn(sessionCartResponse);

            CartResponse response = cartService.removeCoupon(VALID_SESSION_ID);

            assertNotNull(response);
            assertEquals(VALID_SESSION_ID, response.getSessionId());
            assertNull(sessionCart.getCoupon());
            verify(cartRepository).findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID);
            verify(cartRepository).save(sessionCart);
            verify(cartMapper).toResponse(sessionCart);
        }
    }

    @Nested
    class CalculateTotalAmountTests {
        @Test
        void calculateTotalAmount_validSessionId_success() {

            when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
            sessionCart.setTotalPrice(new BigDecimal("199.98"));

            BigDecimal total = cartService.calculateTotalAmount(VALID_SESSION_ID);

            assertEquals(new BigDecimal("199.98"), total);
            verify(cartRepository).findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID);
        }

        @Test
        void calculateTotalAmount_cartNotFound_throwsCartNotFoundException() {
            when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.empty());

            assertThrows(CartNotFoundException.class,
                    () -> cartService.calculateTotalAmount(VALID_SESSION_ID));

            verify(cartRepository).findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID);
        }
    }

    @Nested
    class CalculateDiscountTests {
        @Test
        void calculateDiscount_validSessionId_withDiscount() {
            when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
            sessionCart.setDiscount(new BigDecimal("20.00"));

            BigDecimal discount = cartService.calculateDiscount(VALID_SESSION_ID);

            assertEquals(new BigDecimal("20.00"), discount);
            verify(cartRepository).findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID);
        }

        @Test
        void calculateDiscount_validSessionId_noDiscount() {
            when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
            sessionCart.setDiscount(null);

            BigDecimal discount = cartService.calculateDiscount(VALID_SESSION_ID);

            assertEquals(BigDecimal.ZERO, discount);
            verify(cartRepository).findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID);
        }

        @Test
        void calculateDiscount_cartNotFound_throwsCartNotFoundException() {
            when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.empty());

            assertThrows(CartNotFoundException.class,
                    () -> cartService.calculateDiscount(VALID_SESSION_ID));

            verify(cartRepository).findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID);
        }
    }
}
*/


//package com.datasaz.ecommerce.services;
//
//import com.datasaz.ecommerce.exceptions.CartItemNotFoundException;
//import com.datasaz.ecommerce.exceptions.CartNotFoundException;
//import com.datasaz.ecommerce.exceptions.ProductNotFoundException;
//import com.datasaz.ecommerce.mappers.CartMapper;
//import com.datasaz.ecommerce.models.request.CartRequest;
//import com.datasaz.ecommerce.models.response.CartItemResponse;
//import com.datasaz.ecommerce.models.response.CartResponse;
//import com.datasaz.ecommerce.repositories.CartRepository;
//import com.datasaz.ecommerce.repositories.ProductRepository;
//import com.datasaz.ecommerce.repositories.UserRepository;
//import com.datasaz.ecommerce.repositories.entities.Cart;
//import com.datasaz.ecommerce.repositories.entities.CartItem;
//import com.datasaz.ecommerce.repositories.entities.Product;
//import com.datasaz.ecommerce.repositories.entities.User;
//import com.datasaz.ecommerce.services.implementations.CartService;
//import com.datasaz.ecommerce.services.implementations.CouponService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.math.BigDecimal;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//public class CartServiceTest {
//
//    @Mock
//    private CartRepository cartRepository;
//    @Mock
//    private ProductRepository productRepository;
//    @Mock
//    private UserRepository userRepository;
//    @Mock
//    private CartMapper cartMapper;
//
//    private CouponService couponService;
//
//    @InjectMocks
//    private CartService cartService;
//
//    private String sessionId;
//    private Product product;
//    private Cart cart;
//    private CartItem cartItem;
//    private CartResponse cartResponse;
//    private User user;
//
//    @BeforeEach
//    void setUp() {
//        sessionId = "test-session";
//        product = new Product();
//        product.setId(1L);
//        product.setName("Test Product");
//        product.setPrice(new BigDecimal("99.99"));
//
//        cart = new Cart();
//        cart.setId(1L);
//        cart.setSessionId(sessionId);
//        cart.setItems(new ArrayList<>());
//
//        cartItem = new CartItem();
//        cartItem.setId(1L);
//        cartItem.setCart(cart);
//        cartItem.setProduct(product);
//        cartItem.setQuantity(2);
//        cart.getItems().add(cartItem);
//
//        cartResponse = CartResponse.builder()
//                .id(1L)
//                .sessionId(sessionId)
//                .items(List.of(CartItemResponse.builder()
//                        .id(1L)
//                        .productId(1L)
//                        .quantity(2)
//                        .build()))
//                .build();
//
//        user = new User();
//        user.setId(1L);
//        user.setEmailAddress("test@example.com");
//    }
//
/// /    @Test
/// /    void addToCart_newCart_createsCart() {
/// /        String sessionId = "test-session";
/// /        CartRequest request = new CartRequest();
/// /        request.setProductId(1L);
/// /        request.setQuantity(2);
/// /
/// /        Product product = new Product();
/// /        product.setId(1L);
/// /        Cart cart = new Cart();
/// /        cart.setSessionId(sessionId);
/// /        CartResponse response = CartResponse.builder().id(1L).sessionId(sessionId).build();
/// /
/// /        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());
/// /        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
/// /        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
/// /        when(cartMapper.toResponse(cart)).thenReturn(response);
/// /
/// /        CartResponse result = cartService.addToCart(sessionId, request);
/// /        assertNotNull(result);
/// /    }
//
//    @Test
//    void addToCart_newCart_createsCartAndAddsItem() {
//        CartRequest request = new CartRequest();
//        request.setProductId(1L);
//        request.setQuantity(2);
//
//        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());
//        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
//        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
//
//        CartResponse result = cartService.addToCart(sessionId, request);
//
//        assertNotNull(result);
//        assertEquals(cartResponse, result);
//        verify(cartRepository).save(any(Cart.class));
//        verify(cartMapper).toResponse(cart);
//    }
//
//    @Test
//    void addToCart_existingCart_updatesExistingItem() {
//        CartRequest request = new CartRequest();
//        request.setProductId(1L);
//        request.setQuantity(3);
//
//        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(cart));
//        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//        when(cartRepository.save(cart)).thenReturn(cart);
//        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
//
//        CartResponse result = cartService.addToCart(sessionId, request);
//
//        assertNotNull(result);
//        assertEquals(5, cart.getItems().get(0).getQuantity()); // 2 + 3
//        verify(cartRepository).save(cart);
//        verify(cartMapper).toResponse(cart);
//    }
//
//    @Test
//    void addToCart_productNotFound_throwsException() {
//        CartRequest request = new CartRequest();
//        request.setProductId(1L);
//        request.setQuantity(2);
//
//        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());
//        when(productRepository.findById(1L)).thenReturn(Optional.empty());
//
//        assertThrows(ProductNotFoundException.class, () -> cartService.addToCart(sessionId, request));
//        verify(cartRepository, never()).save(any(Cart.class));
//    }
//
//    @Test
//    void updateCartItem_validItem_updatesQuantity() {
//        int newQuantity = 5;
//
//        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(cart));
//        when(cartRepository.save(cart)).thenReturn(cart);
//        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
//
//        CartResponse result = cartService.updateCart(sessionId, 1L, newQuantity);
//
//        assertNotNull(result);
//        assertEquals(newQuantity, cart.getItems().get(0).getQuantity());
//        verify(cartRepository).save(cart);
//        verify(cartMapper).toResponse(cart);
//    }
//
//    @Test
//    void updateCartItem_zeroQuantity_removesItem() {
//        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(cart));
//        when(cartRepository.save(cart)).thenReturn(cart);
//        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
//
//        CartResponse result = cartService.updateCart(sessionId, 1L, 0);
//
//        assertNotNull(result);
//        assertTrue(cart.getItems().isEmpty());
//        verify(cartRepository).save(cart);
//        verify(cartMapper).toResponse(cart);
//    }
//
//    @Test
//    void updateCartItem_cartNotFound_throwsException() {
//        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());
//
//        assertThrows(CartNotFoundException.class, () -> cartService.updateCart(sessionId, 1L, 5));
//        verify(cartRepository, never()).save(any(Cart.class));
//    }
//
//    @Test
//    void updateCartItem_itemNotFound_throwsException() {
//        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(cart));
//
//        assertThrows(CartItemNotFoundException.class, () -> cartService.updateCart(sessionId, 999L, 5));
//        verify(cartRepository, never()).save(any(Cart.class));
//    }
//
//    @Test
//    void removeFromCart_validItem_removesItem() {
//        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(cart));
//        when(cartRepository.save(cart)).thenReturn(cart);
//        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
//
//        CartResponse result = cartService.removeFromCart(sessionId, 1L);
//
//        assertNotNull(result);
//        assertTrue(cart.getItems().isEmpty());
//        verify(cartRepository).save(cart);
//        verify(cartMapper).toResponse(cart);
//    }
//
//    @Test
//    void removeFromCart_cartNotFound_throwsException() {
//        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());
//
//        assertThrows(CartNotFoundException.class, () -> cartService.removeFromCart(sessionId, 1L));
//        verify(cartRepository, never()).save(any(Cart.class));
//    }
//
//    @Test
//    void removeFromCart_itemNotFound_throwsException() {
//        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(cart));
//
//        assertThrows(CartItemNotFoundException.class, () -> cartService.removeFromCart(sessionId, 999L));
//        verify(cartRepository, never()).save(any(Cart.class));
//    }
//
//    @Test
//    void getCart_existingCart_returnsCart() {
//        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(cart));
//        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
//
//        CartResponse result = cartService.getCart(sessionId);
//
//        assertNotNull(result);
//        assertEquals(cartResponse, result);
//        verify(cartMapper).toResponse(cart);
//    }
//
//    @Test
//    void getCart_cartNotFound_throwsException() {
//        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());
//
//        assertThrows(CartNotFoundException.class, () -> cartService.getCart(sessionId));
//    }
//
//    @Test
//    void mergeCartOnLogin_anonymousCartExists_mergesWithUserCart() {
//        Cart userCart = new Cart();
//        userCart.setId(2L);
//        userCart.setUser(user);
//        userCart.setItems(new ArrayList<>());
//
//        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
//        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(cart));
//        when(cartRepository.findByUser(user)).thenReturn(Optional.of(userCart));
//        when(cartRepository.save(userCart)).thenReturn(userCart);
//        when(cartMapper.toResponse(userCart)).thenReturn(cartResponse);
//
//        CartResponse result = cartService.mergeCartOnLogin(sessionId, 1L);
//
//        assertNotNull(result);
//        verify(cartRepository).delete(cart);
//        verify(cartRepository).save(userCart);
//        verify(cartMapper).toResponse(userCart);
//    }
//
//    @Test
//    void mergeCartOnLogin_noAnonymousCart_returnsUserCart() {
//        Cart userCart = new Cart();
//        userCart.setId(2L);
//        userCart.setUser(user);
//        userCart.setItems(new ArrayList<>());
//
//        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
//        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());
//        when(cartRepository.findByUser(user)).thenReturn(Optional.of(userCart));
//        when(cartRepository.save(userCart)).thenReturn(userCart);
//        when(cartMapper.toResponse(userCart)).thenReturn(cartResponse);
//
//        CartResponse result = cartService.mergeCartOnLogin(sessionId, 1L);
//
//        assertNotNull(result);
//        verify(cartRepository, never()).delete(any(Cart.class));
//        verify(cartRepository).save(userCart);
//        verify(cartMapper).toResponse(userCart);
//    }
//
//    @Test
//    void mergeCartOnLogin_noUserCart_createsNewUserCart() {
//        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
//        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(cart));
//        when(cartRepository.findByUser(user)).thenReturn(Optional.empty());
//        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
//        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
//
//        CartResponse result = cartService.mergeCartOnLogin(sessionId, 1L);
//
//        assertNotNull(result);
//        verify(cartRepository).delete(cart);
//        verify(cartRepository).save(any(Cart.class));
//        verify(cartMapper).toResponse(cart);
//    }
//
//    @Test
//    void mergeCartOnLogin_userNotFound_throwsException() {
//        when(userRepository.findById(1L)).thenReturn(Optional.empty());
//
//        assertThrows(RuntimeException.class, () -> cartService.mergeCartOnLogin(sessionId, 1L));
//        verify(cartRepository, never()).save(any(Cart.class));
//    }
//
//    @Test
//    void clearCart_existingCart_clearsItems() {
//        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(cart));
//        when(cartRepository.save(cart)).thenReturn(cart);
//
//        cartService.clearCart(sessionId);
//
//        assertTrue(cart.getItems().isEmpty());
//        verify(cartRepository).save(cart);
//    }
//
//    @Test
//    void clearCart_cartNotFound_throwsException() {
//        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());
//
//        assertThrows(CartNotFoundException.class, () -> cartService.clearCart(sessionId));
//        verify(cartRepository, never()).save(any(Cart.class));
//    }
//
/// *
//    @Test
//    void testAddToCartProductNotFound() {
//        CartRequest cartRequest = CartRequest.builder().product(Product.builder().id(1L).build()).users(Users.builder().id(1L).build()).quantity(2).build();
//        when(productRepository.findById(anyLong())).thenThrow(new RuntimeException("Product not found"));
//        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> {
//            cartService.addToCart(cartRequest);
//        });
//        Assertions.assertEquals(exception.getMessage(), "Product not found");
//    }
//    @Test
//    void testAddToCartClientNotFound() {
//        Product mockProduct = Product.builder().id(1L).name("product").price(BigDecimal.valueOf(100.0)).quantity(10).build();
//        CartRequest cartRequest = CartRequest.builder().product(Product.builder().id(1L).build()).users(Users.builder().id(1L).build()).quantity(2).build();
//        when(productRepository.findById(any())).thenReturn(Optional.of(mockProduct));
//        //  when(userRepository.findById(any())).thenThrow(new RuntimeException("Client not found"));
//        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> {
//            cartService.addToCart(cartRequest);
//        });
//        Assertions.assertEquals(exception.getMessage(), "Client not found");
//    }
//    @Test
//    void testAddToCartInsufficientStock() {
//        Users mockUsers = Users.builder().id(1L).emailAddress("email").password("password").resetToken("resetToken").build();
//        Product mockProduct = Product.builder().id(1L).name("product").price(BigDecimal.valueOf(100.0)).quantity(1).build();
//        when(productRepository.findById(any())).thenReturn(Optional.of(mockProduct));
//        //  when(userRepository.findById(any())).thenReturn(Optional.ofNullable(mockUsers));
//        Assertions.assertThrows(RuntimeException.class, () -> {
//            cartService.addToCart(CartRequest.builder().product(mockProduct).users(mockUsers).quantity(2).build());
//        });
//    }
//    @Test
//    void testGetCartByClient() {
//        Users mockUsers = Users.builder().id(1L).emailAddress("email").password("password").resetToken("resetToken").build();
//        Product mockProduct = Product.builder().id(1L).name("product").price(BigDecimal.valueOf(100.0)).quantity(10).build();
//        Cart mockCart = Cart.builder().id(1L).product(mockProduct).users(mockUsers).quantity(2).totalPrice(BigDecimal.valueOf(200.0)).discount(BigDecimal.valueOf(0.0)).build();
//        //  when(cartRepository.findByUsers(any())).thenReturn(java.util.List.of(mockCart));
//        when(cartMapper.mapToCartResponse(any())).thenReturn(CartResponse.builder().id(mockCart.getId()).product(mockProduct).quantity(mockCart.getQuantity()).totalPrice(mockCart.getTotalPrice()).build());
//        //java.util.List<CartResponse> cart = cartService.getCartByUsers(mockUsers);
//       /* Assertions.assertEquals(1, cart.size());
//        Assertions.assertEquals(mockCart.getId(), cart.get(0).getId());
//        Assertions.assertEquals(mockCart.getProduct(), cart.get(0).getProduct());
//        Assertions.assertEquals(mockCart.getQuantity(), cart.get(0).getQuantity());
//        Assertions.assertEquals(mockCart.getTotalPrice(), cart.get(0).getTotalPrice());
//                */
/// ***  }
//
// @Test void testRemoveFromCart() {
// Users mockUsers = Users.builder().id(1L).emailAddress("email").password("password").resetToken("resetToken").build();
// Product mockProduct = Product.builder().id(1L).name("product").price(BigDecimal.valueOf(100.0)).quantity(10).build();
// Cart mockCart = Cart.builder().id(1L).product(mockProduct).users(mockUsers).quantity(2).totalPrice(BigDecimal.valueOf(200.0)).discount(BigDecimal.valueOf(0.0)).build();
// when(cartRepository.findById(any())).thenReturn(Optional.of(mockCart));
// when(productRepository.save(any())).thenReturn(mockProduct);
// doNothing().when(cartRepository).deleteById(any());
// Assertions.assertDoesNotThrow(() -> {
// cartService.removeFromCart(1L);
// Assertions.assertEquals(12, mockProduct.getQuantity());
// });
// }
//
// @Test void testRemoveFromCartNotFound() {
// when(cartRepository.findById(any())).thenReturn(Optional.empty());
// Assertions.assertThrows(RuntimeException.class, () -> {
// cartService.removeFromCart(1L);
// });
// }
//
// @Test void testRemoveFromCartProductNotFound() {
// Users mockUsers = Users.builder().id(1L).emailAddress("email").password("password").resetToken("resetToken").build();
// Product mockProduct = Product.builder().id(1L).name("product").price(BigDecimal.valueOf(100.0)).quantity(10).build();
// Cart mockCart = Cart.builder().id(1L).product(mockProduct).users(mockUsers).quantity(2).totalPrice(BigDecimal.valueOf(200.0)).discount(BigDecimal.valueOf(0.0)).build();
// when(cartRepository.findById(any())).thenThrow(new RuntimeException("No items found in cart"));
// RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> {
// cartService.removeFromCart(1L);
// });
// Assertions.assertEquals(exception.getMessage(), "No items found in cart");
// }
//
// @Test void testRemoveFromCartDeleteFailed() {
// Users mockUsers = Users.builder().id(1L).emailAddress("email").password("password").resetToken("resetToken").build();
// Product mockProduct = Product.builder().id(1L).name("product").price(BigDecimal.valueOf(100.0)).quantity(10).build();
// Cart mockCart = Cart.builder().id(1L).product(mockProduct).users(mockUsers).quantity(2).totalPrice(BigDecimal.valueOf(200.0)).discount(BigDecimal.valueOf(0.0)).build();
// when(cartRepository.findById(any())).thenReturn(Optional.of(mockCart));
// doNothing().when(cartRepository).deleteById(any());
// Assertions.assertDoesNotThrow(() -> {
// cartService.removeFromCart(1L);
// Assertions.assertEquals(12, mockProduct.getQuantity());
// });
// }
//
// @Test void testRemoveFromCartDeleteFailedNotFound() {
// when(cartRepository.findById(any())).thenReturn(Optional.empty());
// Assertions.assertThrows(RuntimeException.class, () -> {
// cartService.removeFromCart(1L);
// });
// }
//
// @Test void addToCart_newCart_sufficientStock_addsItem() {
// CartRequest request = new CartRequest();
// request.setProductId(1L);
// request.setQuantity(5); // Within stock (10)
//
// when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());
// when(productRepository.findById(1L)).thenReturn(Optional.of(product));
// when(cartRepository.save(any(Cart.class))).thenReturn(cart);
// when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
//
// CartResponse result = cartService.addToCart(sessionId, request);
//
// assertNotNull(result);
// assertEquals(cartResponse, result);
// verify(cartRepository).save(any(Cart.class));
// verify(cartMapper).toResponse(cart);
// }
//
// @Test void addToCart_newCart_insufficientStock_throwsException() {
// CartRequest request = new CartRequest();
// request.setProductId(1L);
// request.setQuantity(15); // Exceeds stock (10)
//
// when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());
// when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//
// assertThrows(InsufficientStockException.class,
// () -> cartService.addToCart(sessionId, request));
// verify(cartRepository, never()).save(any(Cart.class));
// }
//
// @Test void addToCart_existingCart_sufficientStock_updatesItem() {
// CartRequest request = new CartRequest();
// request.setProductId(1L);
// request.setQuantity(3); // Total: 2 + 3 = 5, within stock (10)
//
// when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(cart));
// when(productRepository.findById(1L)).thenReturn(Optional.of(product));
// when(cartRepository.save(cart)).thenReturn(cart);
// when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
//
// CartResponse result = cartService.addToCart(sessionId, request);
//
// assertNotNull(result);
// assertEquals(5, cart.getItems().get(0).getQuantity());
// verify(cartRepository).save(cart);
// verify(cartMapper).toResponse(cart);
// }
//
// @Test void addToCart_existingCart_insufficientStock_throwsException() {
// CartRequest request = new CartRequest();
// request.setProductId(1L);
// request.setQuantity(10); // Total: 2 + 10 = 12, exceeds stock (10)
//
// when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(cart));
// when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//
// assertThrows(InsufficientStockException.class,
// () -> cartService.addToCart(sessionId, request));
// verify(cartRepository, never()).save(any(Cart.class));
// }
//
// @Test void updateCartItem_sufficientStock_updatesQuantity() {
// int newQuantity = 7; // Within stock (10)
//
// when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(cart));
// when(cartRepository.save(cart)).thenReturn(cart);
// when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
//
// CartResponse result = cartService.updateCartItem(sessionId, 1L, newQuantity);
//
// assertNotNull(result);
// assertEquals(newQuantity, cart.getItems().get(0).getQuantity());
// verify(cartRepository).save(cart);
// verify(cartMapper).toResponse(cart);
// }
//
// @Test void updateCartItem_insufficientStock_throwsException() {
// int newQuantity = 15; // Exceeds stock (10)
//
// when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(cart));
//
// assertThrows(InsufficientStockException.class,
// () -> cartService.updateCartItem(sessionId, 1L, newQuantity));
// verify(cartRepository, never()).save(any(Cart.class));
// }
//
// @Test void updateCartItem_zeroQuantity_removesItem() {
// when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(cart));
// when(cartRepository.save(cart)).thenReturn(cart);
// when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
//
// CartResponse result = cartService.updateCartItem(sessionId, 1L, 0);
//
// assertNotNull(result);
// assertTrue(cart.getItems().isEmpty());
// verify(cartRepository).save(cart);
// verify(cartMapper).toResponse(cart);
// }
//
// @Test void updateCartItem_cartNotFound_throwsException() {
// when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());
//
// assertThrows(CartNotFoundException.class,
// () -> cartService.updateCartItem(sessionId, 1L, 5));
// verify(cartRepository, never()).save(any(Cart.class));
// }
//
// @Test void updateCartItem_itemNotFound_throwsException() {
// when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(cart));
//
// assertThrows(CartItemNotFoundException.class,
// () -> cartService.updateCartItem(sessionId, 999L, 5));
// verify(cartRepository, never()).save(any(Cart.class));
// }
//
// @Test void removeFromCart_validItem_removesItem() {
// when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(cart));
// when(cartRepository.save(cart)).thenReturn(cart);
// when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
//
// CartResponse result = cartService.removeFromCart(sessionId, 1L);
//
// assertNotNull(result);
// assertTrue(cart.getItems().isEmpty());
// verify(cartRepository).save(cart);
// verify(cartMapper).toResponse(cart);
// }
//
// @Test void removeFromCart_cartNotFound_throwsException() {
// when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());
//
// assertThrows(CartNotFoundException.class,
// () -> cartService.removeFromCart(sessionId, 1L));
// verify(cartRepository, never()).save(any(Cart.class));
// }
//
// @Test void removeFromCart_itemNotFound_throwsException() {
// when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(cart));
//
// assertThrows(CartItemNotFoundException.class,
// () -> cartService.removeFromCart(sessionId, 999L));
// verify(cartRepository, never()).save(any(Cart.class));
// }
//
// @Test void getCart_existingCart_returnsCart() {
// when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(cart));
// when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
//
// CartResponse result = cartService.getCart(sessionId);
//
// assertNotNull(result);
// assertEquals(cartResponse, result);
// verify(cartMapper).toResponse(cart);
// }
//
// @Test void getCart_cartNotFound_throwsException() {
// when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());
//
// assertThrows(CartNotFoundException.class, () -> cartService.getCart(sessionId));
// }
//
// @Test void mergeCartOnLogin_anonymousCart_sufficientStock_mergesCart() {
// Cart userCart = new Cart();
// userCart.setId(2L);
// userCart.setUser(user);
// userCart.setItems(new ArrayList<>());
//
// when(userRepository.findById(1L)).thenReturn(Optional.of(user));
// when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(cart));
// when(cartRepository.findByUser(user)).thenReturn(Optional.of(userCart));
// when(cartRepository.save(userCart)).thenReturn(userCart);
// when(cartMapper.toResponse(userCart)).thenReturn(cartResponse);
//
// CartResponse result = cartService.mergeCartOnLogin(sessionId, 1L);
//
// assertNotNull(result);
// verify(cartRepository).delete(cart);
// verify(cartRepository).save(userCart);
// verify(cartMapper).toResponse(userCart);
// }
//
// @Test void mergeCartOnLogin_anonymousCart_insufficientStock_throwsException() {
// product.setStock(1); // Stock too low for quantity 2 in cartItem
//
// when(userRepository.findById(1L)).thenReturn(Optional.of(user));
// when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(cart));
// when(cartRepository.findByUser(user)).thenReturn(Optional.of(new Cart()));
//
// assertThrows(InsufficientStockException.class,
// () -> cartService.mergeCartOnLogin(sessionId, 1L));
// verify(cartRepository, never()).delete(any(Cart.class));
// verify(cartRepository, never()).save(any(Cart.class));
// }
//
// @Test void mergeCartOnLogin_noAnonymousCart_returnsUserCart() {
// Cart userCart = new Cart();
// userCart.setId(2L);
// userCart.setUser(user);
// userCart.setItems(new ArrayList<>());
//
// when(userRepository.findById(1L)).thenReturn(Optional.of(user));
// when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());
// when(cartRepository.findByUser(user)).thenReturn(Optional.of(userCart));
// when(cartRepository.save(userCart)).thenReturn(userCart);
// when(cartMapper.toResponse(userCart)).thenReturn(cartResponse);
//
// CartResponse result = cartService.mergeCartOnLogin(sessionId, 1L);
//
// assertNotNull(result);
// verify(cartRepository, never()).delete(any(Cart.class));
// verify(cartRepository).save(userCart);
// verify(cartMapper).toResponse(userCart);
// }
//
// @Test void mergeCartOnLogin_noUserCart_sufficientStock_createsUserCart() {
// when(userRepository.findById(1L)).thenReturn(Optional.of(user));
// when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(cart));
// when(cartRepository.findByUser(user)).thenReturn(Optional.empty());
// when(cartRepository.save(any(Cart.class))).thenReturn(cart);
// when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
//
// CartResponse result = cartService.mergeCartOnLogin(sessionId, 1L);
//
// assertNotNull(result);
// verify(cartRepository).delete(cart);
// verify(cartRepository).save(any(Cart.class));
// verify(cartMapper).toResponse(cart);
// }
//
// @Test void mergeCartOnLogin_userNotFound_throwsException() {
// when(userRepository.findById(1L)).thenReturn(Optional.empty());
//
// assertThrows(RuntimeException.class,
// () -> cartService.mergeCartOnLogin(sessionId, 1L));
// verify(cartRepository, never()).save(any(Cart.class));
// }
//
// @Test void clearCart_existingCart_clearsItems() {
// when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(cart));
// when(cartRepository.save(cart)).thenReturn(cart);
//
// cartService.clearCart(sessionId);
//
// assertTrue(cart.getItems().isEmpty());
// verify(cartRepository).save(cart);
// }
//
// @Test void clearCart_cartNotFound_throwsException() {
// when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());
//
// assertThrows(CartNotFoundException.class, () -> cartService.clearCart(sessionId));
// verify(cartRepository, never()).save(any(Cart.class));
// }
//
// @Test void applyCoupon_productSpecific_validProduct_appliesDiscount() {
// Coupon coupon = new Coupon();
// coupon.setIdentifier("SAVE10");
// coupon.setState(CouponState.ACTIVE);
// coupon.setCategory(CouponCategory.PRODUCT_SPECIFIC);
// coupon.setCouponScope(CouponScope.ITEM);
// coupon.setCouponType(CouponType.FIXED);
// coupon.setCouponValue(new BigDecimal("10.00"));
// coupon.setStartFrom(LocalDateTime.now().minusDays(1));
// coupon.setEndAt(LocalDateTime.now().plusDays(1));
// coupon.setCouponTrackings(new HashSet<>());
//
// CouponTracking tracking = new CouponTracking();
// tracking.setCoupon(coupon);
// tracking.setProduct(product);
// tracking.setUsed(false);
// coupon.getCouponTrackings().add(tracking);
//
// when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(cart));
// when(couponService.validateCoupon(eq("SAVE10"), isNull(), any(BigDecimal.class),
// eq(cart.getItems()), isNull())).thenReturn(coupon);
// when(couponService.calculateDiscount(coupon, cart.getItems(), any(BigDecimal.class)))
// .thenReturn(new BigDecimal("10.00"));
// when(cartRepository.save(cart)).thenReturn(cart);
// when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
//
// CartResponse result = cartService.applyCoupon(sessionId, "SAVE10");
//
// assertNotNull(result);
// assertEquals("SAVE10", result.getCouponIdentifier());
// assertEquals(new BigDecimal("10.00"), result.getDiscountAmount());
// verify(cartRepository).save(cart);
// }*/
//
//
//}
//
//
//
//
