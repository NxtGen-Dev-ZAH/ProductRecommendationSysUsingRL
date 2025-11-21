package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.*;
import com.datasaz.ecommerce.mappers.CartMapper;
import com.datasaz.ecommerce.models.request.CartItemRequest;
import com.datasaz.ecommerce.models.response.AppliedCouponResponse;
import com.datasaz.ecommerce.models.response.CartResponse;
import com.datasaz.ecommerce.repositories.CartRepository;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.*;
import com.datasaz.ecommerce.services.interfaces.ICouponService;
import com.datasaz.ecommerce.utilities.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)          // <-- allows different arguments
class BuyerCartServiceTest {

    /* ------------------------------------------------- MOCKS ------------------------------------------------- */
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
    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private BuyerCartService buyerCartService;

    /* ----------------------------------------------- COMMON DATA --------------------------------------------- */
    private User user, user2;
    private Cart userCart, userCart2, anonymousCart;
    private Product product1, product2;
    private Coupon coupon;
    private CartResponse cartResponse;
    private AppliedCouponResponse appliedCouponResponse;

    private static final String VALID_SESSION_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String INVALID_SESSION_ID = "invalid";
    private static final Long USER_ID = 100L;
    private static final Long USER_ID2 = 1L;
    private static final String USER_ID_STR = "100";

    @BeforeEach
    void setUp() {
        user = User.builder().id(USER_ID).build();
        user2 = User.builder().id(USER_ID2).build();

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
                .build();

        coupon = Coupon.builder()
                .id(1L)
                .code("SAVE10")
                .discountPercentage(BigDecimal.TEN)
                .build();

        userCart = Cart.builder()
                .id(1L)
                .user(user)
                .items(new ArrayList<>())                   // <-- mutable list
                .build();

        userCart2 = Cart.builder()
                .id(1L)
                .user(user2)
                .items(new ArrayList<>())                   // <-- mutable list
                .build();

        anonymousCart = Cart.builder()
                .id(2L)
                .sessionId(VALID_SESSION_ID)
                .items(new ArrayList<>())                   // <-- mutable list
                .coupon(coupon)
                .build();

        cartResponse = CartResponse.builder().build();

        appliedCouponResponse = AppliedCouponResponse.builder()
                .code("SAVE10")
                .discount(BigDecimal.TEN)
                .cartResponse(cartResponse)
                .build();
    }

    /* --------------------------------------------------- HELPERS -------------------------------------------------- */
    private void mockSaveAndReturnId(Cart cart) {
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> {
            Cart c = i.getArgument(0);
            if (c.getId() == null) c.setId(999L);
            return c;
        });
    }

    /* ================================================= MERGE ON LOGIN ============================================= */

    @Test
    void mergeCartOnLogin_valid_mergeItemsAndCoupon() {
        // user already has 3 of product1
        userCart.getItems().add(CartItem.builder()
                .id(100L)
                .product(product1)
                .quantity(3)
                .cart(userCart)
                .build());

        // anonymous cart has 2 of product1 and 1 of product2
        anonymousCart.getItems().add(CartItem.builder()
                .id(200L)
                .product(product1)
                .quantity(2)
                .cart(anonymousCart)
                .build());
        anonymousCart.getItems().add(CartItem.builder()
                .id(201L)
                .product(product2)
                .quantity(1)
                .cart(anonymousCart)
                .build());

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(anonymousCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product2));
        when(couponService.validateCoupon(eq("SAVE10"), eq(user), any(), any())).thenReturn(coupon);
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.mergeCartOnLogin(VALID_SESSION_ID, USER_ID);

        // product1: 3 (user) + 2 (anon) = 5
        CartItem merged = userCart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(1L))
                .findFirst()
                .orElseThrow();
        assertEquals(5, merged.getQuantity());

        // two different products → two items
        assertEquals(2, userCart.getItems().size());
        assertEquals(coupon, userCart.getCoupon());
        verify(cartRepository).delete(anonymousCart);
    }

    @Test
    void mergeCartOnLogin_noAnonymousCart_createsUserCart() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.empty());
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.empty());
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.mergeCartOnLogin(VALID_SESSION_ID, USER_ID);

        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void mergeCartOnLogin_insufficientStock_skipsItem() {
        product1.setQuantity(1);

        Cart anon = Cart.builder()
                .id(2L)
                .sessionId(VALID_SESSION_ID)
                .items(new ArrayList<>(List.of(
                        CartItem.builder().id(10L).product(product1).quantity(2).build(),
                        CartItem.builder().id(20L).product(product2).quantity(3).build()
                )))
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(anon));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product2));
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.mergeCartOnLogin(VALID_SESSION_ID, USER_ID);

        assertEquals(1, userCart.getItems().size());
        assertEquals(2L, userCart.getItems().get(0).getProduct().getId());
        verify(cartRepository).delete(anon);
    }

    @Test
    void mergeCartOnLogin_productNotFound_skipsItem() {
        // anonymous cart contains product1 (not found) and product2 (found)
        anonymousCart.getItems().add(CartItem.builder()
                .id(200L)
                .product(product1)
                .quantity(2)
                .cart(anonymousCart)
                .build());
        anonymousCart.getItems().add(CartItem.builder()
                .id(201L)
                .product(product2)
                .quantity(1)
                .cart(anonymousCart)
                .build());

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(anonymousCart));
        when(productRepository.findById(1L)).thenReturn(Optional.empty());
        when(productRepository.findById(2L)).thenReturn(Optional.of(product2));
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.mergeCartOnLogin(VALID_SESSION_ID, USER_ID);

        // only product2 survived
        assertEquals(1, userCart.getItems().size());
        assertEquals(2L, userCart.getItems().get(0).getProduct().getId());
    }

    @Test
    void mergeCartOnLogin_couponInvalid_skipsCoupon() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(anonymousCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product2));
        when(couponService.validateCoupon(eq("SAVE10"), eq(user), any(), any()))
                .thenThrow(new RuntimeException("Invalid"));
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.mergeCartOnLogin(VALID_SESSION_ID, USER_ID);

        assertNull(userCart.getCoupon());
        verify(cartRepository).delete(anonymousCart);
    }


    @Test
    void mergeCartOnLogin_invalidSessionId_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> buyerCartService.mergeCartOnLogin(INVALID_SESSION_ID, USER_ID));
        verify(cartRepository, never()).findBySessionIdWithItemsAndCoupon(any());
    }

    @Test
    void mergeCartOnLogin_userNotFound_throwsUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> buyerCartService.mergeCartOnLogin(VALID_SESSION_ID, USER_ID));
    }

    /* ================================================ getOrCreateCart ============================================ */
    @Test
    void getOrCreateCart_existingCart_returnsIt() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));

        Cart result = buyerCartService.getOrCreateCart(USER_ID_STR);

        assertEquals(userCart, result);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void getOrCreateCart_noCart_returnsInMemoryCart() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.empty());

        Cart result = buyerCartService.getOrCreateCart(USER_ID_STR);

        assertNotNull(result);
        assertNull(result.getId());
        assertEquals(user, result.getUser());
        assertTrue(result.getItems().isEmpty());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void getOrCreateCart_userNotFound_throwsUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> buyerCartService.getOrCreateCart(USER_ID_STR));
    }

    @Test
    void getOrCreateCart_invalidUserId_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> buyerCartService.getOrCreateCart("invalid"));

        verify(userRepository, never()).findById(any());
        verify(cartRepository, never()).findByUserWithItems(any());
    }

    /* ================================================ addToCart ================================================= */
    @Test
    void addToCart_validRequestNewCart_createsCartAndAddsItem() {
        CartItemRequest req = new CartItemRequest(1L, 2);

        // 1. user exists
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        // 2. no cart yet → getOrCreateCart will create a new one
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.empty());

        // 3. product exists
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));

        // 4. save returns the same instance (with id = 999L)
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart c = invocation.getArgument(0);
            if (c.getId() == null) c.setId(999L);
            return c;
        });

        // 5. mapper is called with the cart that was just saved
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        CartResponse result = buyerCartService.addToCart(USER_ID_STR, req);

        assertNotNull(result);

        // -----------------------------------------------------------------
        // The cart that is returned from getOrCreateCart is the one we saved.
        // Capture it so we can assert on the **real** cart instance.
        // -----------------------------------------------------------------
        ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).save(cartCaptor.capture());
        Cart savedCart = cartCaptor.getValue();

        assertEquals(1, savedCart.getItems().size());
        assertEquals(2, savedCart.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("200.0"), savedCart.getSubtotalPrice());

        verify(cartRepository).save(any(Cart.class));
    }

//    @Test
//    void addToCart_validRequestNewCart_createsCartAndAddsItem() {
//        CartItemRequest req = new CartItemRequest(1L, 2);
//        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
//        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.empty());
//        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
//        mockSaveAndReturnId(userCart);
//        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);
//
//        CartResponse result = buyerCartService.addToCart(USER_ID_STR, req);
//
//        assertNotNull(result);
//        assertEquals(1, userCart.getItems().size());
//        assertEquals(2, userCart.getItems().get(0).getQuantity());
//        // 2 × 100.00 = 200.00
//        assertEquals(new BigDecimal("200.00"), userCart.getSubtotalPrice());
//        verify(cartRepository).save(userCart);
//    }

    @Test
    void addToCart_validRequestExistingCart_addsNewItem() {
        CartItemRequest req = new CartItemRequest(1L, 2);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.addToCart(USER_ID_STR, req);

        assertEquals(1, userCart.getItems().size());
        assertEquals(2, userCart.getItems().get(0).getQuantity());
    }

    @Test
    void addToCart_existingItem_updatesQuantity() {
        userCart.getItems().add(CartItem.builder()
                .id(1L)
                .product(product1)
                .quantity(3)
                .cart(userCart)
                .build());

        CartItemRequest req = new CartItemRequest(1L, 2);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.addToCart(USER_ID_STR, req);

        assertEquals(5, userCart.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("500.0"), userCart.getSubtotalPrice());
    }

    @Test
    void addToCart_nullProductId_throwsBadRequestException() {
        CartItemRequest req = new CartItemRequest(null, 2);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));

        assertThrows(BadRequestException.class, () -> buyerCartService.addToCart(USER_ID_STR, req));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addToCart_negativeQuantity_throwsBadRequestException() {
        CartItemRequest req = new CartItemRequest(1L, -1);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));

        assertThrows(BadRequestException.class, () -> buyerCartService.addToCart(USER_ID_STR, req));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addToCart_nonExistentProduct_throwsProductNotFoundException() {
        CartItemRequest req = new CartItemRequest(1L, 2);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> buyerCartService.addToCart(USER_ID_STR, req));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addToCart_insufficientStock_throwsInsufficientStockException() {
        CartItemRequest req = new CartItemRequest(1L, 15);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));

        assertThrows(InsufficientStockException.class, () -> buyerCartService.addToCart(USER_ID_STR, req));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addToCart_invalidUserId_throwsBadRequestException() {
        CartItemRequest req = new CartItemRequest(1L, 2);
        assertThrows(BadRequestException.class, () -> buyerCartService.addToCart("invalid", req));
        verify(userRepository, never()).findById(any());
    }

    @Test
    void addToCart_nonExistentUser_throwsUserNotFoundException() {
        CartItemRequest req = new CartItemRequest(1L, 2);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> buyerCartService.addToCart(USER_ID_STR, req));
        verify(cartRepository, never()).findByUserWithItems(any());
    }

    /* ============================================== updateCartItem ============================================== */
    @Test
    void updateCartItem_validRequest_updatesQuantity() {
        CartItem ci = CartItem.builder().id(1L).product(product1).quantity(2).cart(userCart).build();
        userCart.getItems().add(ci);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.updateCartItem(USER_ID_STR, 1L, 5);

        assertEquals(5, userCart.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("500.0"), userCart.getSubtotalPrice());
    }

    @Test
    void updateCartItem_zeroQuantity_removesItem() {
        CartItem ci = CartItem.builder().id(1L).product(product1).quantity(2).cart(userCart).build();
        userCart.getItems().add(ci);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.updateCartItem(USER_ID_STR, 1L, 0);

        assertTrue(userCart.getItems().isEmpty());
    }

    @Test
    void updateCartItem_nonExistentItem_throwsCartItemNotFoundException() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));

        assertThrows(CartItemNotFoundException.class,
                () -> buyerCartService.updateCartItem(USER_ID_STR, 1L, 5));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateCartItem_nonExistentProduct_throwsProductNotFoundException() {
        CartItem ci = CartItem.builder().id(1L).product(product1).quantity(2).cart(userCart).build();
        userCart.getItems().add(ci);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> buyerCartService.updateCartItem(USER_ID_STR, 1L, 5));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateCartItem_insufficientStock_throwsInsufficientStockException() {
        CartItem ci = CartItem.builder().id(1L).product(product1).quantity(2).cart(userCart).build();
        userCart.getItems().add(ci);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));

        assertThrows(InsufficientStockException.class,
                () -> buyerCartService.updateCartItem(USER_ID_STR, 1L, 15));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateCartItem_invalidUserId_throwsBadRequestException() {
        assertThrows(BadRequestException.class,
                () -> buyerCartService.updateCartItem("invalid", 1L, 5));
        verify(userRepository, never()).findById(any());
    }

    /* ============================================== removeFromCart ============================================== */
    @Test
    void removeFromCart_validItemId_removesItem() {
        CartItem ci = CartItem.builder().id(1L).product(product1).quantity(2).cart(userCart).build();
        userCart.getItems().add(ci);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.removeFromCart(USER_ID_STR, 1L);

        assertTrue(userCart.getItems().isEmpty());
    }

    @Test
    void removeFromCart_nonExistentItem_throwsCartItemNotFoundException() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));

        assertThrows(CartItemNotFoundException.class,
                () -> buyerCartService.removeFromCart(USER_ID_STR, 1L));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void removeFromCart_invalidUserId_throwsBadRequestException() {
        assertThrows(BadRequestException.class,
                () -> buyerCartService.removeFromCart("invalid", 1L));
        verify(userRepository, never()).findById(any());
    }

    /* ================================================= clearCart ================================================= */
    @Test
    void clearCart_withItems_clearsItemsAndSaves() {
        userCart.getItems().add(CartItem.builder().id(1L).product(product1).quantity(2).cart(userCart).build());

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.clearCart(USER_ID_STR);

        assertTrue(userCart.getItems().isEmpty());
        verify(cartRepository).save(userCart);
    }

    @Test
    void clearCart_invalidUserId_throwsBadRequestException() {
        assertThrows(BadRequestException.class,
                () -> buyerCartService.clearCart("invalid"));
        verify(userRepository, never()).findById(any());
    }

    /* ================================================= getCart =================================================== */
    @Test
    void getCart_existingCart_returnsCartResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(cartMapper.toResponse(eq(userCart), eq(pageable))).thenReturn(cartResponse);

        CartResponse result = buyerCartService.getCart(USER_ID_STR, pageable);

        assertNotNull(result);
        verify(cartMapper).toResponse(userCart, pageable);
    }

    @Test
    void getCart_newCart_createsAndReturnsCart() {
        Pageable pageable = PageRequest.of(0, 10);

        // 1. User exists
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        // 2. No existing cart → orElseGet creates a new in-memory Cart
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.empty());

        // 3. cartMapper is called with the **newly created in-memory cart**
        //    (note: it has no ID yet, and is NOT saved)
        when(cartMapper.toResponse(any(Cart.class), eq(pageable))).thenReturn(cartResponse);

        // Execute
        CartResponse result = buyerCartService.getCart(USER_ID_STR, pageable);

        // Assertions
        assertNotNull(result);
        assertEquals(cartResponse, result);

        // IMPORTANT: The cart is **created in memory**, but **NOT saved** to DB
        verify(cartRepository, never()).save(any(Cart.class));

        // Verify mapper was called with a Cart that:
        // - belongs to the correct user
        // - has empty mutable items list
        // - has zero totals
        ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
        verify(cartMapper).toResponse(cartCaptor.capture(), eq(pageable));

        Cart createdCart = cartCaptor.getValue();
        assertEquals(user, createdCart.getUser());
        assertNull(createdCart.getId());
        assertTrue(createdCart.getItems().isEmpty());
        assertEquals(BigDecimal.ZERO, createdCart.getSubtotalPrice());
        assertEquals(BigDecimal.ZERO, createdCart.getTotalShippingCost());
        assertEquals(BigDecimal.ZERO, createdCart.getTotalDiscount());
        assertEquals(BigDecimal.ZERO, createdCart.getTotalAmount());
    }


    @Test
    void getCart_invalidUserId_throwsBadRequestException() {
        assertThrows(BadRequestException.class,
                () -> buyerCartService.getCart("invalid", PageRequest.of(0, 10)));
        verify(userRepository, never()).findById(any());
    }

    /* ================================================ applyCoupon =============================================== */
    @Test
    void applyCoupon_validCoupon_appliesCoupon() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(couponService.validateCoupon(eq("SAVE10"), eq(user), any(), any())).thenReturn(coupon);
        when(couponService.calculateDiscount(eq(coupon), any(), any())).thenReturn(BigDecimal.TEN);
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        AppliedCouponResponse result = buyerCartService.applyCoupon(USER_ID_STR, "SAVE10");

        assertEquals(BigDecimal.TEN, result.getDiscount());
        assertEquals(coupon, userCart.getCoupon());
        verify(cartRepository).save(userCart);
    }

    @Test
    void applyCoupon_invalidCoupon_throwsException() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(couponService.validateCoupon(eq("INVALID"), eq(user), any(), any()))
                .thenThrow(new RuntimeException("Invalid coupon"));

        assertThrows(RuntimeException.class,
                () -> buyerCartService.applyCoupon(USER_ID_STR, "INVALID"));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void applyCoupon_invalidUserId_throwsBadRequestException() {
        assertThrows(BadRequestException.class,
                () -> buyerCartService.applyCoupon("invalid", "SAVE10"));
        verify(userRepository, never()).findById(any());
    }

    /* ================================================ removeCoupon =============================================== */
    @Test
    void removeCoupon_withCoupon_removesCoupon() {
        userCart.setCoupon(coupon);
        userCart.setTotalDiscount(BigDecimal.TEN);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.removeCoupon(USER_ID_STR);

        assertNull(userCart.getCoupon());
        assertEquals(BigDecimal.ZERO, userCart.getTotalDiscount());
        verify(cartRepository).save(userCart);
    }

    @Test
    void removeCoupon_noCoupon_doesNothing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.removeCoupon(USER_ID_STR);

        assertNull(userCart.getCoupon());
        verify(cartRepository).save(userCart);
    }

    @Test
    void removeCoupon_invalidUserId_throwsBadRequestException() {
        assertThrows(BadRequestException.class,
                () -> buyerCartService.removeCoupon("invalid"));
        verify(userRepository, never()).findById(any());
    }

    /* ============================================ calculateTotalAmount =========================================== */
    @Test
    void calculateTotalAmount_withItems_returnsTotal() {
        userCart.getItems().add(CartItem.builder()
                .id(1L)
                .product(product1)
                .quantity(2)
                .cart(userCart)
                .build());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));

        BigDecimal result = buyerCartService.calculateTotalAmount(USER_ID_STR);

        // 2 × 100.00 = 200.00
        // shipping: base 10 + 1 extra = 11.00
        // total = 211.00
        assertEquals(new BigDecimal("211.0"), result);
    }

    @Test
    void calculateTotalAmount_emptyCart_returnsZero() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));

        BigDecimal result = buyerCartService.calculateTotalAmount(USER_ID_STR);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateTotalAmount_invalidUserId_throwsBadRequestException() {
        assertThrows(BadRequestException.class,
                () -> buyerCartService.calculateTotalAmount("invalid"));
        verify(userRepository, never()).findById(any());
    }

    /* ============================================= calculateDiscount ============================================ */
    @Test
    void calculateDiscount_withCoupon_returnsDiscount() {
        userCart.setCoupon(coupon);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(couponService.calculateDiscount(eq(coupon), any(), any())).thenReturn(BigDecimal.TEN);

        BigDecimal result = buyerCartService.calculateDiscount(USER_ID_STR);

        assertEquals(BigDecimal.TEN, result);
    }

    @Test
    void calculateDiscount_noCoupon_returnsZero() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));

        BigDecimal result = buyerCartService.calculateDiscount(USER_ID_STR);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateDiscount_invalidUserId_throwsBadRequestException() {
        assertThrows(BadRequestException.class,
                () -> buyerCartService.calculateDiscount("invalid"));
        verify(userRepository, never()).findById(any());
    }

    /* ================================================ validateIdentifier ========================================= */
    @Test
    void validateIdentifier_validUserId_passes() {
        assertDoesNotThrow(() -> buyerCartService.validateIdentifier("100"));
    }

    @Test
    void validateIdentifier_invalidUserId_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> buyerCartService.validateIdentifier("abc"));
    }

    /* ================================================ mergeCartOnLogin (old) ===================================== */
    @Test
    void mergeCartOnLogin_validSessionAndUser_mergesCart() {
        Product p = Product.builder().id(1L).price(BigDecimal.TEN).quantity(10).build();
        CartItem anonItem = CartItem.builder().id(1L).product(p).quantity(2).cart(anonymousCart).build();
        anonymousCart.getItems().add(anonItem);
        Coupon c = Coupon.builder().id(1L).code("SAVE10").build();
        anonymousCart.setCoupon(c);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user2));
        when(cartRepository.findByUserWithItems(user2)).thenReturn(Optional.of(userCart2));
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(anonymousCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(couponService.validateCoupon(eq("SAVE10"), eq(user2), any(), any())).thenReturn(c);
        when(couponService.calculateDiscount(eq(c), any(), eq(new BigDecimal("20.00")))).thenReturn(BigDecimal.TEN);
        mockSaveAndReturnId(userCart2);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.mergeCartOnLogin(VALID_SESSION_ID, 1L);

        assertEquals(1, userCart2.getItems().size());
        assertEquals(2, userCart2.getItems().get(0).getQuantity());
        assertEquals(c, userCart2.getCoupon());
        assertEquals(new BigDecimal("20"), userCart2.getSubtotalPrice());
        verify(cartRepository).delete(anonymousCart);
    }

    @Test
    void mergeCartOnLogin_noAnonymousCart_doesNothing() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user2));
        when(cartRepository.findByUserWithItems(user2)).thenReturn(Optional.of(userCart2));
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.empty());
        mockSaveAndReturnId(userCart2);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.mergeCartOnLogin(VALID_SESSION_ID, 1L);

        assertTrue(userCart2.getItems().isEmpty());
        verify(cartRepository, never()).delete(any());
        verify(cartRepository).save(userCart2);
    }

//    @Test
//    void mergeCartOnLogin_noAnonymousCart_doesNothing() {
//        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
//        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.empty());
//        when(cartMapper.toResponse(userCart)).thenReturn(cartResponse);
//        when(cartRepository.save(any(Cart.class))).thenReturn(userCart);
//
//        CartResponse result = buyerCartService.mergeCartOnLogin(VALID_SESSION_ID, 1L);
//
//        assertNotNull(result);
//        assertTrue(userCart.getItems().isEmpty());
//        verify(cartRepository, never()).delete(any());
//        verify(cartRepository).save(userCart);
//        verify(cartMapper).toResponse(userCart);
//    }

    @Test
    void mergeCartOnLogin_existingItem_updatesQuantity() {
        Product p = Product.builder().id(1L).price(BigDecimal.TEN).quantity(10).build();
        CartItem anonItem = CartItem.builder().id(1L).product(p).quantity(2).cart(anonymousCart).build();
        anonymousCart.getItems().add(anonItem);
        CartItem userItem = CartItem.builder().id(2L).product(p).quantity(3).cart(userCart2).build();
        userCart2.getItems().add(userItem);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user2));
        when(cartRepository.findByUserWithItems(user2)).thenReturn(Optional.of(userCart2));
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(anonymousCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        mockSaveAndReturnId(userCart2);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.mergeCartOnLogin(VALID_SESSION_ID, 1L);

        assertEquals(5, userCart2.getItems().get(0).getQuantity());
        verify(cartRepository).delete(anonymousCart);
    }

//    @Test
//    void mergeCartOnLogin_insufficientStock_skipsItem() {
//        Product p = Product.builder().id(1L).price(BigDecimal.TEN).quantity(1).build();
//        CartItem anonItem = CartItem.builder().id(1L).product(p).quantity(15).cart(anonymousCart).build();
//        anonymousCart.getItems().add(anonItem);
//
//        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
//        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(anonymousCart));
//        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
//        mockSaveAndReturnId(userCart);
//        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);
//
//        buyerCartService.mergeCartOnLogin(VALID_SESSION_ID, 1L);
//
//        assertTrue(userCart.getItems().isEmpty());
//        verify(cartRepository).delete(anonymousCart);
//    }

    @Test
    void mergeCartOnLogin_invalidCoupon_skipsCoupon() {
        Product p = Product.builder().id(1L).price(BigDecimal.TEN).quantity(10).build();
        CartItem anonItem = CartItem.builder().id(1L).product(p).quantity(2).cart(anonymousCart).build();
        anonymousCart.getItems().add(anonItem);
        Coupon c = Coupon.builder().id(1L).code("SAVE10").build();
        anonymousCart.setCoupon(c);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user2));
        when(cartRepository.findByUserWithItems(user2)).thenReturn(Optional.of(userCart2));
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(anonymousCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(couponService.validateCoupon(eq("SAVE10"), eq(user2), any(), any()))
                .thenThrow(new RuntimeException("Invalid coupon"));
        mockSaveAndReturnId(userCart2);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.mergeCartOnLogin(VALID_SESSION_ID, 1L);

        assertEquals(1, userCart2.getItems().size());
        assertNull(userCart2.getCoupon());
        verify(cartRepository).delete(anonymousCart);
    }

    @Test
    void mergeCartOnLogin_nonExistentProduct_skipsItem() {
        Product p = Product.builder().id(1L).price(BigDecimal.TEN).quantity(10).build();
        CartItem anonItem = CartItem.builder().id(1L).product(p).quantity(2).cart(anonymousCart).build();
        anonymousCart.getItems().add(anonItem);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user2));
        when(cartRepository.findByUserWithItems(user2)).thenReturn(Optional.of(userCart2));
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(anonymousCart));
        when(productRepository.findById(1L)).thenReturn(Optional.empty());
        mockSaveAndReturnId(userCart2);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.mergeCartOnLogin(VALID_SESSION_ID, 1L);

        assertTrue(userCart2.getItems().isEmpty());
        verify(cartRepository).delete(anonymousCart);
    }

    @Test
    void mergeCartOnLogin_nonExistentProduct_skipsItem2() {
        CartItem anonymousItem = CartItem.builder()
                .id(1L)
                .product(product1)
                .quantity(2)
                .cart(anonymousCart)
                .build();
        anonymousCart.getItems().add(anonymousItem);

        when(userRepository.findById(USER_ID2)).thenReturn(Optional.of(user2));
        when(cartRepository.findByUserWithItems(user2)).thenReturn(Optional.of(userCart2));
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(anonymousCart));
        when(productRepository.findById(1L)).thenReturn(Optional.empty());
        when(cartMapper.toResponse(userCart2)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(userCart2);

        CartResponse result = buyerCartService.mergeCartOnLogin(VALID_SESSION_ID, USER_ID2);

        assertNotNull(result);
        assertTrue(userCart2.getItems().isEmpty());
        verify(cartRepository).delete(anonymousCart);
        verify(cartRepository).save(userCart2);
    }

}


/*import com.datasaz.ecommerce.exceptions.*;
import com.datasaz.ecommerce.mappers.CartMapper;
import com.datasaz.ecommerce.models.request.CartItemRequest;
import com.datasaz.ecommerce.models.response.AppliedCouponResponse;
import com.datasaz.ecommerce.models.response.CartResponse;
import com.datasaz.ecommerce.repositories.CartRepository;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.*;
import com.datasaz.ecommerce.services.interfaces.ICouponService;
import com.datasaz.ecommerce.utilities.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)          // <-- allows different arguments
class BuyerCartService3Test {

    *//* ------------------------------------------------- MOCKS ------------------------------------------------- *//*
    @Mock private CartRepository cartRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private CartMapper cartMapper;
    @Mock private ICouponService couponService;
    @Mock private CurrentUserService currentUserService;

    @InjectMocks private BuyerCartService buyerCartService;

    *//* ----------------------------------------------- COMMON DATA --------------------------------------------- *//*
    private User user;
    private Cart userCart, anonymousCart;
    private Product product1, product2;
    private Coupon coupon;
    private CartResponse cartResponse;
    private AppliedCouponResponse appliedCouponResponse;

    private static final String VALID_SESSION_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String INVALID_SESSION_ID = "invalid";
    private static final Long USER_ID = 100L;
    private static final String USER_ID_STR = "100";

    @BeforeEach
    void setUp() {
        user = User.builder().id(USER_ID).build();

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
                .build();

        coupon = Coupon.builder()
                .id(1L)
                .code("SAVE10")
                .discountPercentage(BigDecimal.TEN)
                .build();

        userCart = Cart.builder()
                .id(1L)
                .user(user)
                .items(new ArrayList<>())                   // <-- mutable list
                .build();

        anonymousCart = Cart.builder()
                .id(2L)
                .sessionId(VALID_SESSION_ID)
                .items(new ArrayList<>())                   // <-- mutable list
                .coupon(coupon)
                .build();

        cartResponse = CartResponse.builder().build();

        appliedCouponResponse = AppliedCouponResponse.builder()
                .code("SAVE10")
                .discount(BigDecimal.TEN)
                .cartResponse(cartResponse)
                .build();
    }

    *//* --------------------------------------------------- HELPERS -------------------------------------------------- *//*
    private void mockSaveAndReturnId(Cart cart) {
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> {
            Cart c = i.getArgument(0);
            if (c.getId() == null) c.setId(999L);
            return c;
        });
    }

    *//* ================================================= MERGE ON LOGIN ============================================= *//*
    @Test
    void mergeCartOnLogin_valid_mergeItemsAndCoupon() {
        // user already has 3 of product1
        userCart.getItems().add(CartItem.builder()
                .id(100L)
                .product(product1)
                .quantity(3)
                .cart(userCart)
                .build());

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(anonymousCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product2));
        when(couponService.validateCoupon(eq("SAVE10"), eq(user), any(), any())).thenReturn(coupon);
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.mergeCartOnLogin(VALID_SESSION_ID, USER_ID);

        CartItem merged = userCart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(1L))
                .findFirst()
                .orElseThrow();
        assertEquals(5, merged.getQuantity());
        assertEquals(2, userCart.getItems().size());
        assertEquals(coupon, userCart.getCoupon());
        verify(cartRepository).delete(anonymousCart);
    }

    @Test
    void mergeCartOnLogin_noAnonymousCart_createsUserCart() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.empty());
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.empty());
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.mergeCartOnLogin(VALID_SESSION_ID, USER_ID);

        verify(cartRepository, times(1)).save(any(Cart.class));
    }

//    @Test
//    void mergeCartOnLogin_insufficientStock_skipsItem() {
//        product1.setQuantity(1);
//
//        Cart anon = Cart.builder()
//                .id(2L)
//                .sessionId(VALID_SESSION_ID)
//                .items(new ArrayList<>(List.of(
//                        CartItem.builder().id(10L).product(product1).quantity(2).build(),
//                        CartItem.builder().id(20L).product(product2).quantity(3).build()
//                )))
//                .build();
//
//        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
//        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(anon));
//        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
//        when(productRepository.findById(2L)).thenReturn(Optional.of(product2));
//        mockSaveAndReturnId(userCart);
//        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);
//
//        buyerCartService.mergeCartOnLogin(VALID_SESSION_ID, USER_ID);
//
//        assertEquals(1, userCart.getItems().size());
//        assertEquals(2L, userCart.getItems().get(0).getProduct().getId());
//        verify(cartRepository).delete(anon);
//    }

    @Test
    void mergeCartOnLogin_productNotFound_skipsItem() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(anonymousCart));
        when(productRepository.findById(1L)).thenReturn(Optional.empty());
        when(productRepository.findById(2L)).thenReturn(Optional.of(product2));
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.mergeCartOnLogin(VALID_SESSION_ID, USER_ID);

        assertEquals(1, userCart.getItems().size());
        assertEquals(2L, userCart.getItems().get(0).getProduct().getId());
    }

    @Test
    void mergeCartOnLogin_couponInvalid_skipsCoupon() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(anonymousCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product2));
        when(couponService.validateCoupon(eq("SAVE10"), eq(user), any(), any()))
                .thenThrow(new RuntimeException("Invalid"));
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.mergeCartOnLogin(VALID_SESSION_ID, USER_ID);

        assertNull(userCart.getCoupon());
        verify(cartRepository).delete(anonymousCart);
    }

    @Test
    void mergeCartOnLogin_invalidSessionId_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> buyerCartService.mergeCartOnLogin(INVALID_SESSION_ID, USER_ID));
        verify(cartRepository, never()).findBySessionIdWithItemsAndCoupon(any());
    }

    @Test
    void mergeCartOnLogin_userNotFound_throwsUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> buyerCartService.mergeCartOnLogin(VALID_SESSION_ID, USER_ID));
    }

    *//* ================================================ getOrCreateCart ============================================ *//*
    @Test
    void getOrCreateCart_existingCart_returnsIt() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));

        Cart result = buyerCartService.getOrCreateCart(USER_ID_STR);

        assertEquals(userCart, result);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void getOrCreateCart_noCart_returnsInMemoryCart() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.empty());

        Cart result = buyerCartService.getOrCreateCart(USER_ID_STR);

        assertNotNull(result);
        assertNull(result.getId());
        assertEquals(user, result.getUser());
        assertTrue(result.getItems().isEmpty());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void getOrCreateCart_userNotFound_throwsUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> buyerCartService.getOrCreateCart(USER_ID_STR));
    }

    @Test
    void getOrCreateCart_invalidUserId_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> buyerCartService.getOrCreateCart("invalid"));
        verify(userRepository, never()).findById(any());
    }

    *//* ================================================ addToCart ================================================= *//*
    @Test
    void addToCart_validRequestNewCart_createsCartAndAddsItem() {
        CartItemRequest req = new CartItemRequest(1L, 2);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.empty());
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        CartResponse result = buyerCartService.addToCart(USER_ID_STR, req);

        assertNotNull(result);
        assertEquals(1, userCart.getItems().size());
        assertEquals(2, userCart.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("200.00"), userCart.getSubtotalPrice());
        verify(cartRepository).save(userCart);
    }

    @Test
    void addToCart_validRequestExistingCart_addsNewItem() {
        CartItemRequest req = new CartItemRequest(1L, 2);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.addToCart(USER_ID_STR, req);

        assertEquals(1, userCart.getItems().size());
        assertEquals(2, userCart.getItems().get(0).getQuantity());
    }

    @Test
    void addToCart_existingItem_updatesQuantity() {
        userCart.getItems().add(CartItem.builder()
                .id(1L)
                .product(product1)
                .quantity(3)
                .cart(userCart)
                .build());

        CartItemRequest req = new CartItemRequest(1L, 2);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.addToCart(USER_ID_STR, req);

        assertEquals(5, userCart.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("500.00"), userCart.getSubtotalPrice());
    }

    @Test
    void addToCart_nullProductId_throwsBadRequestException() {
        CartItemRequest req = new CartItemRequest(null, 2);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));

        assertThrows(BadRequestException.class, () -> buyerCartService.addToCart(USER_ID_STR, req));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addToCart_negativeQuantity_throwsBadRequestException() {
        CartItemRequest req = new CartItemRequest(1L, -1);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));

        assertThrows(BadRequestException.class, () -> buyerCartService.addToCart(USER_ID_STR, req));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addToCart_nonExistentProduct_throwsProductNotFoundException() {
        CartItemRequest req = new CartItemRequest(1L, 2);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> buyerCartService.addToCart(USER_ID_STR, req));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addToCart_insufficientStock_throwsInsufficientStockException() {
        CartItemRequest req = new CartItemRequest(1L, 15);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));

        assertThrows(InsufficientStockException.class, () -> buyerCartService.addToCart(USER_ID_STR, req));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addToCart_invalidUserId_throwsBadRequestException() {
        CartItemRequest req = new CartItemRequest(1L, 2);
        assertThrows(BadRequestException.class, () -> buyerCartService.addToCart("invalid", req));
        verify(userRepository, never()).findById(any());
    }

    @Test
    void addToCart_nonExistentUser_throwsUserNotFoundException() {
        CartItemRequest req = new CartItemRequest(1L, 2);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> buyerCartService.addToCart(USER_ID_STR, req));
        verify(cartRepository, never()).findByUserWithItems(any());
    }

    *//* ============================================== updateCartItem ============================================== *//*
    @Test
    void updateCartItem_validRequest_updatesQuantity() {
        CartItem ci = CartItem.builder().id(1L).product(product1).quantity(2).cart(userCart).build();
        userCart.getItems().add(ci);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.updateCartItem(USER_ID_STR, 1L, 5);

        assertEquals(5, userCart.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("500.00"), userCart.getSubtotalPrice());
    }

    @Test
    void updateCartItem_zeroQuantity_removesItem() {
        CartItem ci = CartItem.builder().id(1L).product(product1).quantity(2).cart(userCart).build();
        userCart.getItems().add(ci);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.updateCartItem(USER_ID_STR, 1L, 0);

        assertTrue(userCart.getItems().isEmpty());
    }

    @Test
    void updateCartItem_nonExistentItem_throwsCartItemNotFoundException() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));

        assertThrows(CartItemNotFoundException.class,
                () -> buyerCartService.updateCartItem(USER_ID_STR, 1L, 5));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateCartItem_nonExistentProduct_throwsProductNotFoundException() {
        CartItem ci = CartItem.builder().id(1L).product(product1).quantity(2).cart(userCart).build();
        userCart.getItems().add(ci);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> buyerCartService.updateCartItem(USER_ID_STR, 1L, 5));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateCartItem_insufficientStock_throwsInsufficientStockException() {
        CartItem ci = CartItem.builder().id(1L).product(product1).quantity(2).cart(userCart).build();
        userCart.getItems().add(ci);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));

        assertThrows(InsufficientStockException.class,
                () -> buyerCartService.updateCartItem(USER_ID_STR, 1L, 15));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateCartItem_invalidUserId_throwsBadRequestException() {
        assertThrows(BadRequestException.class,
                () -> buyerCartService.updateCartItem("invalid", 1L, 5));
        verify(userRepository, never()).findById(any());
    }

    *//* ============================================== removeFromCart ============================================== *//*
    @Test
    void removeFromCart_validItemId_removesItem() {
        CartItem ci = CartItem.builder().id(1L).product(product1).quantity(2).cart(userCart).build();
        userCart.getItems().add(ci);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.removeFromCart(USER_ID_STR, 1L);

        assertTrue(userCart.getItems().isEmpty());
    }

    @Test
    void removeFromCart_nonExistentItem_throwsCartItemNotFoundException() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));

        assertThrows(CartItemNotFoundException.class,
                () -> buyerCartService.removeFromCart(USER_ID_STR, 1L));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void removeFromCart_invalidUserId_throwsBadRequestException() {
        assertThrows(BadRequestException.class,
                () -> buyerCartService.removeFromCart("invalid", 1L));
        verify(userRepository, never()).findById(any());
    }

    *//* ================================================= clearCart ================================================= *//*
    @Test
    void clearCart_withItems_clearsItemsAndSaves() {
        userCart.getItems().add(CartItem.builder().id(1L).product(product1).quantity(2).cart(userCart).build());

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.clearCart(USER_ID_STR);

        assertTrue(userCart.getItems().isEmpty());
        verify(cartRepository).save(userCart);
    }

    @Test
    void clearCart_invalidUserId_throwsBadRequestException() {
        assertThrows(BadRequestException.class,
                () -> buyerCartService.clearCart("invalid"));
        verify(userRepository, never()).findById(any());
    }

    *//* ================================================= getCart =================================================== *//*
    @Test
    void getCart_existingCart_returnsCartResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(cartMapper.toResponse(eq(userCart), eq(pageable))).thenReturn(cartResponse);

        CartResponse result = buyerCartService.getCart(USER_ID_STR, pageable);

        assertNotNull(result);
        verify(cartMapper).toResponse(userCart, pageable);
    }

    @Test
    void getCart_newCart_createsAndReturnsCart() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.empty());
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(eq(userCart), eq(pageable))).thenReturn(cartResponse);

        buyerCartService.getCart(USER_ID_STR, pageable);

        verify(cartRepository).save(any(Cart.class));
        verify(cartMapper).toResponse(userCart, pageable);
    }

    @Test
    void getCart_invalidUserId_throwsBadRequestException() {
        assertThrows(BadRequestException.class,
                () -> buyerCartService.getCart("invalid", PageRequest.of(0, 10)));
        verify(userRepository, never()).findById(any());
    }

    *//* ================================================ applyCoupon =============================================== *//*
    @Test
    void applyCoupon_validCoupon_appliesCoupon() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(couponService.validateCoupon(eq("SAVE10"), eq(user), any(), any())).thenReturn(coupon);
        when(couponService.calculateDiscount(eq(coupon), any(), any())).thenReturn(BigDecimal.TEN);
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        AppliedCouponResponse result = buyerCartService.applyCoupon(USER_ID_STR, "SAVE10");

        assertEquals(BigDecimal.TEN, result.getDiscount());
        assertEquals(coupon, userCart.getCoupon());
        verify(cartRepository).save(userCart);
    }

    @Test
    void applyCoupon_invalidCoupon_throwsException() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(couponService.validateCoupon(eq("INVALID"), eq(user), any(), any()))
                .thenThrow(new RuntimeException("Invalid coupon"));

        assertThrows(RuntimeException.class,
                () -> buyerCartService.applyCoupon(USER_ID_STR, "INVALID"));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void applyCoupon_invalidUserId_throwsBadRequestException() {
        assertThrows(BadRequestException.class,
                () -> buyerCartService.applyCoupon("invalid", "SAVE10"));
        verify(userRepository, never()).findById(any());
    }

    *//* ================================================ removeCoupon =============================================== *//*
    @Test
    void removeCoupon_withCoupon_removesCoupon() {
        userCart.setCoupon(coupon);
        userCart.setTotalDiscount(BigDecimal.TEN);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.removeCoupon(USER_ID_STR);

        assertNull(userCart.getCoupon());
        assertEquals(BigDecimal.ZERO, userCart.getTotalDiscount());
        verify(cartRepository).save(userCart);
    }

    @Test
    void removeCoupon_noCoupon_doesNothing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.removeCoupon(USER_ID_STR);

        assertNull(userCart.getCoupon());
        verify(cartRepository).save(userCart);
    }

    @Test
    void removeCoupon_invalidUserId_throwsBadRequestException() {
        assertThrows(BadRequestException.class,
                () -> buyerCartService.removeCoupon("invalid"));
        verify(userRepository, never()).findById(any());
    }

    *//* ============================================ calculateTotalAmount =========================================== *//*
    @Test
    void calculateTotalAmount_withItems_returnsTotal() {
        userCart.getItems().add(CartItem.builder()
                .id(1L)
                .product(product1)
                .quantity(2)
                .cart(userCart)
                .build());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));

        BigDecimal result = buyerCartService.calculateTotalAmount(USER_ID_STR);

        // 2 × 100.00 = 200.00
        // shipping: base 10 + 1 extra = 11.00
        // total = 211.00
        assertEquals(new BigDecimal("211.00"), result);
    }

    @Test
    void calculateTotalAmount_emptyCart_returnsZero() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));

        BigDecimal result = buyerCartService.calculateTotalAmount(USER_ID_STR);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateTotalAmount_invalidUserId_throwsBadRequestException() {
        assertThrows(BadRequestException.class,
                () -> buyerCartService.calculateTotalAmount("invalid"));
        verify(userRepository, never()).findById(any());
    }

    *//* ============================================= calculateDiscount ============================================ *//*
    @Test
    void calculateDiscount_withCoupon_returnsDiscount() {
        userCart.setCoupon(coupon);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(couponService.calculateDiscount(eq(coupon), any(), any())).thenReturn(BigDecimal.TEN);

        BigDecimal result = buyerCartService.calculateDiscount(USER_ID_STR);

        assertEquals(BigDecimal.TEN, result);
    }

    @Test
    void calculateDiscount_noCoupon_returnsZero() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));

        BigDecimal result = buyerCartService.calculateDiscount(USER_ID_STR);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateDiscount_invalidUserId_throwsBadRequestException() {
        assertThrows(BadRequestException.class,
                () -> buyerCartService.calculateDiscount("invalid"));
        verify(userRepository, never()).findById(any());
    }

    *//* ================================================ validateIdentifier ========================================= *//*
    @Test
    void validateIdentifier_validUserId_passes() {
        assertDoesNotThrow(() -> buyerCartService.validateIdentifier("100"));
    }

    @Test
    void validateIdentifier_invalidUserId_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> buyerCartService.validateIdentifier("abc"));
    }

    *//* ================================================ mergeCartOnLogin (old) ===================================== *//*
    @Test
    void mergeCartOnLogin_validSessionAndUser_mergesCart() {
        Product p = Product.builder().id(1L).price(BigDecimal.TEN).quantity(10).build();
        CartItem anonItem = CartItem.builder().id(1L).product(p).quantity(2).cart(anonymousCart).build();
        anonymousCart.getItems().add(anonItem);
        Coupon c = Coupon.builder().id(1L).code("SAVE10").build();
        anonymousCart.setCoupon(c);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(anonymousCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(couponService.validateCoupon(eq("SAVE10"), eq(user), any(), any())).thenReturn(c);
        when(couponService.calculateDiscount(eq(c), any(), eq(new BigDecimal("20.00")))).thenReturn(BigDecimal.TEN);
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.mergeCartOnLogin(VALID_SESSION_ID, 1L);

        assertEquals(1, userCart.getItems().size());
        assertEquals(2, userCart.getItems().get(0).getQuantity());
        assertEquals(c, userCart.getCoupon());
        assertEquals(new BigDecimal("20.00"), userCart.getSubtotalPrice());
        verify(cartRepository).delete(anonymousCart);
    }

    @Test
    void mergeCartOnLogin_noAnonymousCart_doesNothing() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.empty());
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.mergeCartOnLogin(VALID_SESSION_ID, 1L);

        assertTrue(userCart.getItems().isEmpty());
        verify(cartRepository, never()).delete(any());
        verify(cartRepository).save(userCart);
    }

    @Test
    void mergeCartOnLogin_existingItem_updatesQuantity() {
        Product p = Product.builder().id(1L).price(BigDecimal.TEN).quantity(10).build();
        CartItem anonItem = CartItem.builder().id(1L).product(p).quantity(2).cart(anonymousCart).build();
        anonymousCart.getItems().add(anonItem);
        CartItem userItem = CartItem.builder().id(2L).product(p).quantity(3).cart(userCart).build();
        userCart.getItems().add(userItem);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(anonymousCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.mergeCartOnLogin(VALID_SESSION_ID, 1L);

        assertEquals(5, userCart.getItems().get(0).getQuantity());
        verify(cartRepository).delete(anonymousCart);
    }

    @Test
    void mergeCartOnLogin_insufficientStock_skipsItem() {
        Product p = Product.builder().id(1L).price(BigDecimal.TEN).quantity(1).build();
        CartItem anonItem = CartItem.builder().id(1L).product(p).quantity(15).cart(anonymousCart).build();
        anonymousCart.getItems().add(anonItem);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(anonymousCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.mergeCartOnLogin(VALID_SESSION_ID, 1L);

        assertTrue(userCart.getItems().isEmpty());
        verify(cartRepository).delete(anonymousCart);
    }

    @Test
    void mergeCartOnLogin_invalidCoupon_skipsCoupon() {
        Product p = Product.builder().id(1L).price(BigDecimal.TEN).quantity(10).build();
        CartItem anonItem = CartItem.builder().id(1L).product(p).quantity(2).cart(anonymousCart).build();
        anonymousCart.getItems().add(anonItem);
        Coupon c = Coupon.builder().id(1L).code("SAVE10").build();
        anonymousCart.setCoupon(c);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(anonymousCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(couponService.validateCoupon(eq("SAVE10"), eq(user), any(), any()))
                .thenThrow(new RuntimeException("Invalid coupon"));
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.mergeCartOnLogin(VALID_SESSION_ID, 1L);

        assertEquals(1, userCart.getItems().size());
        assertNull(userCart.getCoupon());
        verify(cartRepository).delete(anonymousCart);
    }

    @Test
    void mergeCartOnLogin_nonExistentProduct_skipsItem() {
        Product p = Product.builder().id(1L).price(BigDecimal.TEN).quantity(10).build();
        CartItem anonItem = CartItem.builder().id(1L).product(p).quantity(2).cart(anonymousCart).build();
        anonymousCart.getItems().add(anonItem);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(anonymousCart));
        when(productRepository.findById(1L)).thenReturn(Optional.empty());
        mockSaveAndReturnId(userCart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        buyerCartService.mergeCartOnLogin(VALID_SESSION_ID, 1L);

        assertTrue(userCart.getItems().isEmpty());
        verify(cartRepository).delete(anonymousCart);
    }
}*/

/*package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.mappers.CartMapper;
import com.datasaz.ecommerce.models.request.CartRequest;
import com.datasaz.ecommerce.models.response.AppliedCouponResponse;
import com.datasaz.ecommerce.models.response.CartResponse;
import com.datasaz.ecommerce.repositories.CartRepository;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.Cart;
import com.datasaz.ecommerce.repositories.entities.CartItem;
import com.datasaz.ecommerce.repositories.entities.Coupon;
import com.datasaz.ecommerce.repositories.entities.Product;
import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.services.interfaces.ICouponService;
import com.datasaz.ecommerce.utilities.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuyerCartServiceTest {

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

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private BuyerCartService buyerCartService;

    private User user;
    private Cart userCart;
    private Cart anonymousCart;
    private Product product;
    private CartResponse cartResponse;
    private final String sessionId = "123e4567-e89b-12d3-a456-426614174000";
    private final String userId = "1";

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).build();
        product = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(BigDecimal.valueOf(10.00))
                .quantity(10)
                .build();

        userCart = Cart.builder()
                .id(1L)
                .user(user)
                .totalPrice(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        anonymousCart = Cart.builder()
                .id(2L)
                .sessionId(sessionId)
                .totalPrice(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        cartResponse = CartResponse.builder().build();
    }

    @Test
    void addToCart_validRequest_addsItem() {
        CartRequest cartRequest = new CartRequest(1L, 2);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartMapper.toResponse(userCart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(userCart);

        CartResponse result = buyerCartService.addToCart(userId, cartRequest);

        assertNotNull(result);
        assertEquals(1, userCart.getItems().size());
        assertEquals(2, userCart.getItems().get(0).getQuantity());
        assertEquals(BigDecimal.valueOf(20.00), userCart.getTotalPrice());
        verify(cartRepository).save(userCart);
        verify(cartMapper).toResponse(userCart);
    }

    @Test
    void addToCart_invalidUserId_throwsUserNotFoundException() {
        CartRequest cartRequest = new CartRequest(1L, 2);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> buyerCartService.addToCart("1", cartRequest));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void mergeCartOnLogin_validSessionAndUser_mergesCart() {
        CartItem anonymousItem = CartItem.builder()
                .id(1L)
                .product(product)
                .quantity(2)
                .price(product.getPrice())
                .productName(product.getName())
                .cart(anonymousCart)
                .build();
        anonymousCart.getItems().add(anonymousItem);
        Coupon coupon = Coupon.builder().id(1L).code("SAVE10").build();
        anonymousCart.setCoupon(coupon);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(cartRepository.findBySessionIdWithItemsAndCoupon(sessionId)).thenReturn(Optional.of(anonymousCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(couponService.validateCoupon(eq("SAVE10"), eq(user), any(BigDecimal.class), any())).thenReturn(coupon);
        when(couponService.calculateDiscount(coupon, userCart.getItems(), BigDecimal.valueOf(20.00))).thenReturn(BigDecimal.TEN);
        when(cartMapper.toResponse(userCart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(userCart);

        CartResponse result = buyerCartService.mergeCartOnLogin(sessionId, 1L);

        assertNotNull(result);
        assertEquals(1, userCart.getItems().size());
        assertEquals(2, userCart.getItems().get(0).getQuantity());
        assertEquals(coupon, userCart.getCoupon());
        assertEquals(BigDecimal.valueOf(10.00), userCart.getTotalPrice());
        verify(cartRepository).delete(anonymousCart);
        verify(cartRepository).save(userCart);
        verify(cartMapper).toResponse(userCart);
    }

    @Test
    void mergeCartOnLogin_invalidSessionId_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> buyerCartService.mergeCartOnLogin("invalid-uuid", 1L));
        verify(userRepository, never()).findById(any());
    }

    @Test
    void mergeCartOnLogin_nonExistentUser_throwsUserNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> buyerCartService.mergeCartOnLogin(sessionId, 1L));
        verify(cartRepository, never()).findBySessionIdWithItemsAndCoupon(any());
    }

    @Test
    void updateCartItem_validRequest_updatesQuantity() {
        CartItem cartItem = CartItem.builder().id(1L).product(product).quantity(2).price(product.getPrice()).productName(product.getName()).cart(userCart).build();
        userCart.getItems().add(cartItem);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartMapper.toResponse(userCart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(userCart);

        CartResponse result = buyerCartService.updateCartItem(userId, 1L, 5);

        assertNotNull(result);
        assertEquals(5, userCart.getItems().get(0).getQuantity());
        assertEquals(BigDecimal.valueOf(50.00), userCart.getTotalPrice());
        verify(cartRepository).save(userCart);
        verify(cartMapper).toResponse(userCart);
    }

    @Test
    void applyCoupon_validCoupon_appliesCoupon() {
        Coupon coupon = Coupon.builder().id(1L).code("SAVE10").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(couponService.validateCoupon(eq("SAVE10"), eq(user), any(BigDecimal.class), any())).thenReturn(coupon);
        when(couponService.calculateDiscount(coupon, userCart.getItems(), BigDecimal.ZERO)).thenReturn(BigDecimal.TEN);
        when(cartMapper.toResponse(userCart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(userCart);

        AppliedCouponResponse result = buyerCartService.applyCoupon(userId, "SAVE10");

        assertNotNull(result);
        assertEquals(BigDecimal.TEN, result.getDiscount());
        assertEquals(coupon, userCart.getCoupon());
        verify(cartRepository).save(userCart);
        verify(cartMapper).toResponse(userCart);
    }

    @Test
    void getCart_returnsCartResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(cartMapper.toResponse(userCart, PageRequest.of(0, 10))).thenReturn(cartResponse);

        CartResponse result = buyerCartService.getCart(userId, PageRequest.of(0, 10));

        assertNotNull(result);
        verify(cartMapper).toResponse(userCart, PageRequest.of(0, 10));
    }
}*/

//import com.datasaz.ecommerce.exceptions.BadRequestException;
//import com.datasaz.ecommerce.exceptions.CartNotFoundException;
//import com.datasaz.ecommerce.exceptions.InsufficientStockException;
//import com.datasaz.ecommerce.exceptions.ProductNotFoundException;
//import com.datasaz.ecommerce.mappers.CartMapper;
//import com.datasaz.ecommerce.models.request.CartRequest;
//import com.datasaz.ecommerce.models.response.AppliedCouponResponse;
//import com.datasaz.ecommerce.models.response.CartItemResponse;
//import com.datasaz.ecommerce.models.response.CartResponse;
//import com.datasaz.ecommerce.repositories.CartRepository;
//import com.datasaz.ecommerce.repositories.ProductRepository;
//import com.datasaz.ecommerce.repositories.UserRepository;
//import com.datasaz.ecommerce.repositories.entities.*;
//import com.datasaz.ecommerce.services.interfaces.ICouponService;
//import com.datasaz.ecommerce.utilities.CurrentUserService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class BuyerCartServiceTest {
//
//    @Mock
//    private CartRepository cartRepository;
//
//    @Mock
//    private ProductRepository productRepository;
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private CartMapper cartMapper;
//
//    @Mock
//    private ICouponService couponService;
//
//    @Mock
//    private CurrentUserService currentUserService;
//
//    @InjectMocks
//    private BuyerCartService buyerCartService;
//
//    private Cart sessionCart;
//    private Cart userCart;
//    private Product product;
//    private User user;
//    private Company company;
//    private Coupon coupon;
//    private CartItem cartItem;
//    private CartResponse sessionCartResponse;
//    private CartResponse userCartResponse;
//    private static final String VALID_SESSION_ID = "eafa58da-f94b-4e68-90c5-169a7cd1b1c1";
//
//    @BeforeEach
//    void setUp() {
//        // Reset mocks to avoid test interference
//        Mockito.reset(cartRepository, productRepository, userRepository, cartMapper, couponService, currentUserService);
//
//        company = new Company();
//        company.setId(1L);
//        company.setName("Test Company");
//
//        user = new User();
//        user.setId(1L);
//        user.setEmailAddress("buyer@test.com");
//        user.setCompany(company);
//
//        product = new Product();
//        product.setId(1L);
//        product.setPrice(new BigDecimal("99.99"));
//        product.setName("Test Product");
//        product.setQuantity(10);
//        product.setAuthor(user);
//        product.setCompany(company);
//
//        cartItem = CartItem.builder()
//                .id(1L)
//                .product(product)
//                .productName(product.getName())
//                .price(product.getPrice())
//                .quantity(2)
//                .cart(null) // Set later to avoid circular reference
//                .build();
//
//        sessionCart = Cart.builder()
//                .id(1L)
//                .sessionId(VALID_SESSION_ID)
//                .user(null)
//                .items(new ArrayList<>())
//                .totalPrice(new BigDecimal("199.98"))
//                .discount(BigDecimal.ZERO)
//                .lastModified(LocalDateTime.now())
//                .version(1L)
//                .build();
//        cartItem.setCart(sessionCart);
//        sessionCart.getItems().add(cartItem);
//
//        userCart = Cart.builder()
//                .id(2L)
//                .sessionId(null)
//                .user(user)
//                .items(new ArrayList<>())
//                .totalPrice(BigDecimal.ZERO)
//                .discount(BigDecimal.ZERO)
//                .lastModified(LocalDateTime.now())
//                .version(1L)
//                .build();
//
//        coupon = Coupon.builder()
//                .code("SAVE10")
//                .state(CouponState.ACTIVE)
//                .category(CouponCategory.GENERAL)
//                .couponScope(CouponScope.ORDER)
//                .couponType(CouponType.PERCENTAGE)
//                .discountPercentage(new BigDecimal("10"))
//                .startFrom(LocalDateTime.now().minusDays(1))
//                .endAt(LocalDateTime.now().plusDays(1))
//                .maxUsesPerUser(1)
//                .maxUses(10)
//                .author(user)
//                .couponTrackings(new HashSet<>())
//                .version(1L)
//                .build();
//
//        sessionCartResponse = CartResponse.builder()
//                .id(1L)
//                .sessionId(VALID_SESSION_ID)
//                .userId(null)
//                .items(List.of(CartItemResponse.builder()
//                        .id(1L)
//                        .productId(1L)
//                        .productName("Test Product")
//                        .price(new BigDecimal("99.99"))
//                        .quantity(2)
//                        .build()))
//                .subtotal(new BigDecimal("199.98"))
//                .discountAmount(BigDecimal.ZERO)
//                .totalAmount(new BigDecimal("199.98"))
//                .build();
//
//        userCartResponse = CartResponse.builder()
//                .id(2L)
//                .sessionId(null)
//                .userId(user.getId())
//                .items(new ArrayList<>())
//                .subtotal(BigDecimal.ZERO)
//                .discountAmount(BigDecimal.ZERO)
//                .totalAmount(BigDecimal.ZERO)
//                .build();
//    }

/// /    @Nested
/// /    class AddToCartTests {
/// /        @Test
/// /        void addToCart_newCart_success() {
/// /            CartRequest cartRequest = CartRequest.builder()
/// /                    .productId(1L)
/// /                    .quantity(1)
/// /                    .build();
/// /            Cart newCart = Cart.builder()
/// /                    .id(3L)
/// /                    .sessionId(VALID_SESSION_ID)
/// /                    .items(new ArrayList<>())
/// /                    .totalPrice(BigDecimal.ZERO)
/// /                    .discount(BigDecimal.ZERO)
/// /                    .lastModified(LocalDateTime.now())
/// /                    .version(1L)
/// /                    .build();
/// /            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());
/// /            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
/// /            when(cartRepository.save(any(Cart.class))).thenReturn(newCart).thenReturn(newCart);
/// /            when(cartMapper.toResponse(any(Cart.class))).thenReturn(sessionCartResponse);
/// /
/// /            CartResponse response = buyerCartService.addToCart(VALID_SESSION_ID, cartRequest);
/// /
/// /            assertNotNull(response);
/// /            assertEquals(VALID_SESSION_ID, response.getSessionId());
/// /            assertNull(response.getUserId());
/// /            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
/// /            verify(cartRepository, times(2)).save(any(Cart.class));
/// /            verify(cartMapper).toResponse(any(Cart.class));
/// /        }
//
//        @Test
//        void addToCart_existingCart_success() {
//            CartRequest cartRequest = CartRequest.builder()
//                    .productId(1L)
//                    .quantity(1)
//                    .build();
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
//            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//            when(cartRepository.save(any(Cart.class))).thenReturn(sessionCart);
//            when(cartMapper.toResponse(sessionCart)).thenReturn(sessionCartResponse);
//
//            CartResponse response = buyerCartService.addToCart(VALID_SESSION_ID, cartRequest);
//
//            assertNotNull(response);
//            assertEquals(VALID_SESSION_ID, response.getSessionId());
//            assertNull(response.getUserId());
//            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
//            verify(cartRepository).save(sessionCart);
//            verify(cartMapper).toResponse(sessionCart);
//        }
//
//        @Test
//        void addToCart_invalidSessionId_throwsBadRequestException() {
//            CartRequest cartRequest = CartRequest.builder()
//                    .productId(1L)
//                    .quantity(1)
//                    .build();
//            assertThrows(BadRequestException.class,
//                    () -> buyerCartService.addToCart("invalid-session", cartRequest));
//            verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(anyString());
//            verify(cartRepository, never()).save(any(Cart.class));
//        }
//
//        @Test
//        void addToCart_productNotFound_throwsProductNotFoundException() {
//            CartRequest cartRequest = CartRequest.builder()
//                    .productId(1L)
//                    .quantity(1)
//                    .build();
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
//            when(productRepository.findById(1L)).thenReturn(Optional.empty());
//
//            assertThrows(ProductNotFoundException.class,
//                    () -> buyerCartService.addToCart(VALID_SESSION_ID, cartRequest));
//            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
//            verify(cartRepository, never()).save(any(Cart.class));
//        }
//
//        @Test
//        void addToCart_insufficientStock_throwsInsufficientStockException() {
//            Cart emptyCart = Cart.builder()
//                    .id(1L)
//                    .sessionId(VALID_SESSION_ID)
//                    .user(null)
//                    .items(new ArrayList<>())
//                    .totalPrice(BigDecimal.ZERO)
//                    .discount(BigDecimal.ZERO)
//                    .lastModified(LocalDateTime.now())
//                    .version(1L)
//                    .build();
//            CartRequest cartRequest = CartRequest.builder()
//                    .productId(1L)
//                    .quantity(17)
//                    .build();
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(emptyCart));
//            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//
//            InsufficientStockException exception = assertThrows(InsufficientStockException.class,
//                    () -> buyerCartService.addToCart(VALID_SESSION_ID, cartRequest));
//
//            assertEquals("Insufficient stock for product ID: 1, requested: 17, available: 10", exception.getMessage());
//            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
//            verify(cartRepository, never()).save(any(Cart.class));
//        }
//    }
//
//    @Nested
//    class AddToCartForUserTests {
//        @Test
//        void addToCartForUser_newCart_success() {
//            CartRequest cartRequest = CartRequest.builder()
//                    .productId(1L)
//                    .quantity(1)
//                    .build();
//            when(currentUserService.getCurrentUser()).thenReturn(user);
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.empty());
//            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//            when(cartRepository.save(any(Cart.class))).thenReturn(userCart).thenReturn(userCart);
//            when(cartMapper.toResponse(any(Cart.class))).thenReturn(userCartResponse);
//
//            CartResponse response = buyerCartService.addToCartForUser(cartRequest);
//
//            assertNotNull(response);
//            assertNull(response.getSessionId());
//            assertEquals(user.getId(), response.getUserId());
//            verify(cartRepository, times(2)).save(any(Cart.class));
//            verify(cartMapper).toResponse(any(Cart.class));
//        }
//
//        @Test
//        void addToCartForUser_existingCart_success() {
//            CartRequest cartRequest = CartRequest.builder()
//                    .productId(1L)
//                    .quantity(1)
//                    .build();
//            userCart.getItems().add(cartItem);
//            when(currentUserService.getCurrentUser()).thenReturn(user);
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//            when(cartRepository.save(any(Cart.class))).thenReturn(userCart);
//            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
//
//            CartResponse response = buyerCartService.addToCartForUser(cartRequest);
//
//            assertNotNull(response);
//            assertNull(response.getSessionId());
//            assertEquals(user.getId(), response.getUserId());
//            verify(cartRepository).save(userCart);
//            verify(cartMapper).toResponse(userCart);
//        }
//
//        @Test
//        void addToCartForUser_productNotFound_throwsProductNotFoundException() {
//            CartRequest cartRequest = CartRequest.builder()
//                    .productId(1L)
//                    .quantity(1)
//                    .build();
//            when(currentUserService.getCurrentUser()).thenReturn(user);
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//            when(productRepository.findById(1L)).thenReturn(Optional.empty());
//
//            assertThrows(ProductNotFoundException.class,
//                    () -> buyerCartService.addToCartForUser(cartRequest));
//            verify(cartRepository, never()).save(any(Cart.class));
//        }
//    }
//
//    @Nested
//    class UpdateCartItemTests {
//        @Test
//        void updateCartItem_validRequest_success() {
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
//            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//            when(cartRepository.save(sessionCart)).thenReturn(sessionCart);
//            when(cartMapper.toResponse(sessionCart)).thenReturn(sessionCartResponse);
//
//            CartResponse response = buyerCartService.updateCartItem(VALID_SESSION_ID, 1L, 3);
//
//            assertNotNull(response);
//            assertEquals(VALID_SESSION_ID, response.getSessionId());
//            assertEquals(3, sessionCart.getItems().get(0).getQuantity());
//            verify(cartRepository).save(sessionCart);
//            verify(cartMapper).toResponse(sessionCart);
//        }
//
//        @Test
//        void updateCartItem_cartNotFound_throwsCartNotFoundException() {
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());
//
//            assertThrows(CartNotFoundException.class,
//                    () -> buyerCartService.updateCartItem(VALID_SESSION_ID, 1L, 3));
//            verify(cartRepository, never()).save(any(Cart.class));
//        }
//
//        @Test
//        void updateCartItem_invalidSessionId_throwsBadRequestException() {
//            BadRequestException exception = assertThrows(BadRequestException.class,
//                    () -> buyerCartService.updateCartItem("invalid-session", 1L, 3));
//
//            assertEquals("Invalid session ID format", exception.getMessage());
//            verify(cartRepository, never()).save(any(Cart.class));
//        }
//    }
//
//    @Nested
//    class UpdateCartItemForUserTests {
//        @Test
//        void updateCartItemForUser_validRequest_success() {
//            userCart.getItems().add(cartItem);
//            when(currentUserService.getCurrentUser()).thenReturn(user);
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//            when(cartRepository.save(userCart)).thenReturn(userCart);
//            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
//
//            CartResponse response = buyerCartService.updateCartItemForUser(1L, 3);
//
//            assertNotNull(response);
//            assertNull(response.getSessionId());
//            assertEquals(user.getId(), response.getUserId());
//            assertEquals(3, userCart.getItems().get(0).getQuantity());
//            verify(cartRepository).save(userCart);
//            verify(cartMapper).toResponse(userCart);
//        }
//
//        @Test
//        void updateCartItemForUser_cartNotFound_throwsCartNotFoundException() {
//            when(currentUserService.getCurrentUser()).thenReturn(user);
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.empty());
//
//            assertThrows(CartNotFoundException.class,
//                    () -> buyerCartService.updateCartItemForUser(1L, 3));
//            verify(cartRepository, never()).save(any(Cart.class));
//        }
//    }
//
//    @Nested
//    class RemoveFromCartTests {
//        @Test
//        void removeFromCart_validRequest_success() {
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
//            when(cartRepository.save(sessionCart)).thenReturn(sessionCart);
//            when(cartMapper.toResponse(sessionCart)).thenReturn(sessionCartResponse);
//
//            CartResponse response = buyerCartService.removeFromCart(VALID_SESSION_ID, 1L);
//
//            assertNotNull(response);
//            assertEquals(VALID_SESSION_ID, response.getSessionId());
//            assertTrue(sessionCart.getItems().isEmpty());
//            verify(cartRepository).save(sessionCart);
//            verify(cartMapper).toResponse(sessionCart);
//        }
//
//        @Test
//        void removeFromCart_cartNotFound_throwsCartNotFoundException() {
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());
//
//            assertThrows(CartNotFoundException.class,
//                    () -> buyerCartService.removeFromCart(VALID_SESSION_ID, 1L));
//            verify(cartRepository, never()).save(any(Cart.class));
//        }
//    }
//
//    @Nested
//    class RemoveFromCartForUserTests {
//        @Test
//        void removeFromCartForUser_validRequest_success() {
//            userCart.getItems().add(cartItem);
//            when(currentUserService.getCurrentUser()).thenReturn(user);
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//            when(cartRepository.save(userCart)).thenReturn(userCart);
//            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
//
//            CartResponse response = buyerCartService.removeFromCartForUser(1L);
//
//            assertNotNull(response);
//            assertNull(response.getSessionId());
//            assertTrue(userCart.getItems().isEmpty());
//            verify(cartRepository).save(userCart);
//            verify(cartMapper).toResponse(userCart);
//        }
//    }
//
//    @Nested
//    class ClearCartTests {
//        @Test
//        void clearCart_validRequest_success() {
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
//            when(cartRepository.save(sessionCart)).thenReturn(sessionCart);
//            when(cartMapper.toResponse(sessionCart)).thenReturn(sessionCartResponse);
//
//            CartResponse response = buyerCartService.clearCart(VALID_SESSION_ID);
//
//            assertNotNull(response);
//            assertEquals(VALID_SESSION_ID, response.getSessionId());
//            assertTrue(sessionCart.getItems().isEmpty());
//            verify(cartRepository).save(sessionCart);
//            verify(cartMapper).toResponse(sessionCart);
//        }
//
//        @Test
//        void clearCart_cartNotFound_throwsCartNotFoundException() {
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());
//
//            assertThrows(CartNotFoundException.class,
//                    () -> buyerCartService.clearCart(VALID_SESSION_ID));
//            verify(cartRepository, never()).save(any(Cart.class));
//        }
//    }
//
//    @Nested
//    class ClearCartForUserTests {
//        @Test
//        void clearCartForUser_validRequest_success() {
//            userCart.getItems().add(cartItem);
//            when(currentUserService.getCurrentUser()).thenReturn(user);
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//            when(cartRepository.save(userCart)).thenReturn(userCart);
//            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
//
//            CartResponse response = buyerCartService.clearCartForUser();
//
//            assertNotNull(response);
//            assertNull(response.getSessionId());
//            assertTrue(userCart.getItems().isEmpty());
//            verify(cartRepository).save(userCart);
//            verify(cartMapper).toResponse(userCart);
//        }
//    }
//
//    @Nested
//    class GetCartTests {
//        @Test
//        void getCart_validSessionId_success() {
//            Pageable pageable = PageRequest.of(0, 10);
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
//            when(cartMapper.toResponse(sessionCart, pageable)).thenReturn(sessionCartResponse);
//
//            CartResponse response = buyerCartService.getCart(VALID_SESSION_ID, pageable);
//
//            assertNotNull(response);
//            assertEquals(VALID_SESSION_ID, response.getSessionId());
//            assertNull(response.getUserId());
//            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
//            verify(cartMapper).toResponse(sessionCart, pageable);
//        }
//
//        @Test
//        void getCart_cartNotFound_throwsCartNotFoundException() {
//            Pageable pageable = PageRequest.of(0, 10);
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());
//
//            assertThrows(CartNotFoundException.class,
//                    () -> buyerCartService.getCart(VALID_SESSION_ID, pageable));
//            verify(cartMapper, never()).toResponse(any(Cart.class), any(Pageable.class));
//        }
//
//        @Test
//        void getCart_invalidSessionId_throwsBadRequestException() {
//            Pageable pageable = PageRequest.of(0, 10);
//            BadRequestException exception = assertThrows(BadRequestException.class,
//                    () -> buyerCartService.getCart("invalid-session", pageable));
//
//            assertEquals("Invalid session ID format", exception.getMessage());
//            verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(anyString());
//        }
//    }
//
//    @Nested
//    class GetCartForUserTests {
//        @Test
//        void getCartForUser_validRequest_success() {
//            Pageable pageable = PageRequest.of(0, 10);
//            when(currentUserService.getCurrentUser()).thenReturn(user);
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//            when(cartMapper.toResponse(userCart, pageable)).thenReturn(userCartResponse);
//
//            CartResponse response = buyerCartService.getCartForUser(pageable);
//
//            assertNotNull(response);
//            assertNull(response.getSessionId());
//            assertEquals(user.getId(), response.getUserId());
//            verify(cartRepository).findByUserWithItems(user);
//            verify(cartMapper).toResponse(userCart, pageable);
//        }
//
//        @Test
//        void getCartForUser_noCart_throwsCartNotFoundException() {
//            Pageable pageable = PageRequest.of(0, 10);
//            when(currentUserService.getCurrentUser()).thenReturn(user);
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.empty());
//
//            assertThrows(CartNotFoundException.class,
//                    () -> buyerCartService.getCartForUser(pageable));
//            verify(cartRepository, never()).save(any(Cart.class));
//            verify(cartMapper, never()).toResponse(any(Cart.class), any(Pageable.class));
//        }
//    }
//
//    @Nested
//    class GetCartByUsersIdTests {
//        @Test
//        void getCartByUsersId_validUserId_success() {
//            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
//
//            CartResponse response = buyerCartService.getCartByUsersId(1L);
//
//            assertNotNull(response);
//            assertNull(response.getSessionId());
//            assertEquals(user.getId(), response.getUserId());
//            verify(cartRepository).findByUserWithItems(user);
//            verify(cartMapper).toResponse(userCart);
//        }
//
//        @Test
//        void getCartByUsersId_userNotFound_throwsException() {
//            when(userRepository.findById(1L)).thenReturn(Optional.empty());
//
//            assertThrows(Exception.class, () -> buyerCartService.getCartByUsersId(1L));
//            verify(cartRepository, never()).findByUserWithItems(any(User.class));
//        }
//    }
//
//    @Nested
//    class MergeCartOnLoginTests {
//        @Test
//        void mergeCartOnLogin_validSessionId_success() {
//            when(currentUserService.getCurrentUser()).thenReturn(user);
//            when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//            when(cartRepository.save(userCart)).thenReturn(userCart);
//            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
//
//            CartResponse response = buyerCartService.mergeCartOnLogin(VALID_SESSION_ID);
//
//            assertNotNull(response);
//            assertNull(response.getSessionId());
//            assertEquals(user.getId(), response.getUserId());
//            verify(cartRepository).findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID);
//            verify(cartRepository).findByUserWithItems(user);
//            verify(cartRepository).save(userCart);
//            verify(cartMapper).toResponse(userCart);
//        }
//
//        @Test
//        void mergeCartOnLogin_noAnonymousCart_success() {
//            when(currentUserService.getCurrentUser()).thenReturn(user);
//            when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.empty());
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//            when(cartRepository.save(userCart)).thenReturn(userCart);
//            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
//
//            CartResponse response = buyerCartService.mergeCartOnLogin(VALID_SESSION_ID);
//
//            assertNotNull(response);
//            assertNull(response.getSessionId());
//            assertEquals(user.getId(), response.getUserId());
//            verify(cartRepository).findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID);
//            verify(cartRepository).findByUserWithItems(user);
//            verify(cartRepository).save(userCart);
//            verify(cartMapper).toResponse(userCart);
//        }
//
//        @Test
//        void mergeCartOnLogin_invalidSessionId_throwsBadRequestException() {
//            assertThrows(BadRequestException.class,
//                    () -> buyerCartService.mergeCartOnLogin("invalid-session"));
//            verify(cartRepository, never()).findBySessionIdWithItemsAndCoupon(anyString());
//            verify(cartRepository, never()).save(any(Cart.class));
//        }
//    }
//
//    @Nested
//    class ApplyCouponTests {
//        @Test
//        void applyCoupon_validCoupon_success() {
//            System.out.println("Testing applyCoupon_validCoupon_success with sessionId: " + VALID_SESSION_ID + ", cart: " + sessionCart);
//            when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
//            when(couponService.validateCoupon(eq("SAVE10"), isNull(), eq(new BigDecimal("199.98")), eq(sessionCart.getItems()))).thenReturn(coupon);
//            when(couponService.calculateDiscount(eq(coupon), eq(sessionCart.getItems()), eq(new BigDecimal("199.98")))).thenReturn(new BigDecimal("20.00"));
//            when(cartRepository.save(sessionCart)).thenReturn(sessionCart);
//            when(cartMapper.toResponse(sessionCart)).thenReturn(sessionCartResponse);
//
//            AppliedCouponResponse response = buyerCartService.applyCoupon(VALID_SESSION_ID, "SAVE10");
//
//            assertNotNull(response);
//            assertEquals(new BigDecimal("20.00"), response.getDiscount());
//            assertNotNull(response.getCartResponse());
//            verify(cartRepository).findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID);
//            verify(couponService).validateCoupon(eq("SAVE10"), isNull(), eq(new BigDecimal("199.98")), eq(sessionCart.getItems()));
//            verify(couponService).calculateDiscount(eq(coupon), eq(sessionCart.getItems()), eq(new BigDecimal("199.98")));
//            verify(cartRepository).save(sessionCart);
//            verify(cartMapper).toResponse(sessionCart);
//        }
//
//        @Test
//        void applyCoupon_cartNotFound_throwsCartNotFoundException() {
//            when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.empty());
//
//            assertThrows(CartNotFoundException.class,
//                    () -> buyerCartService.applyCoupon(VALID_SESSION_ID, "SAVE10"));
//            verify(cartRepository).findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID);
//            verify(cartRepository, never()).save(any(Cart.class));
//            verify(cartMapper, never()).toResponse(any(Cart.class));
//            verify(couponService, never()).validateCoupon(anyString(), any(), any(), any());
//        }
//
//        @Test
//        void applyCoupon_invalidCoupon_throwsBadRequestException() {
//            System.out.println("Testing applyCoupon_invalidCoupon_throwsBadRequestException with sessionId: " + VALID_SESSION_ID + ", cart: " + sessionCart);
//            when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
//            when(couponService.validateCoupon(eq("INVALID"), isNull(), eq(new BigDecimal("199.98")), eq(sessionCart.getItems())))
//                    .thenThrow(BadRequestException.builder().message("Invalid coupon code").build());
//
//            assertThrows(BadRequestException.class,
//                    () -> buyerCartService.applyCoupon(VALID_SESSION_ID, "INVALID"));
//            verify(cartRepository).findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID);
//            verify(couponService).validateCoupon(eq("INVALID"), isNull(), eq(new BigDecimal("199.98")), eq(sessionCart.getItems()));
//            verify(cartRepository, never()).save(any(Cart.class));
//            verify(cartMapper, never()).toResponse(any(Cart.class));
//        }
//    }
//
//    @Nested
//    class ApplyCouponForUserTests {
//        @Test
//        void applyCouponForUser_validCoupon_success() {
//            userCart.getItems().add(cartItem);
//            when(currentUserService.getCurrentUser()).thenReturn(user);
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//            when(couponService.validateCoupon("SAVE10", user, new BigDecimal("199.98"), userCart.getItems())).thenReturn(coupon);
//            when(couponService.calculateDiscount(coupon, userCart.getItems(), new BigDecimal("199.98"))).thenReturn(new BigDecimal("20.00"));
//            when(cartRepository.save(userCart)).thenReturn(userCart);
//            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
//
//            AppliedCouponResponse response = buyerCartService.applyCouponForUser("SAVE10");
//
//            assertNotNull(response);
//            assertEquals(new BigDecimal("20.00"), response.getDiscount());
//            verify(cartRepository).save(userCart);
//            verify(cartMapper).toResponse(userCart);
//        }
//    }
//
//    @Nested
//    class RemoveCouponTests {
//        @Test
//        void removeCoupon_validSessionId_success() {
//            System.out.println("Testing removeCoupon_validSessionId_success with sessionId: " + VALID_SESSION_ID + ", cart: " + sessionCart);
//            sessionCart.setCoupon(coupon);
//            when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
//            when(cartRepository.save(sessionCart)).thenReturn(sessionCart);
//            when(cartMapper.toResponse(sessionCart)).thenReturn(sessionCartResponse);
//
//            CartResponse response = buyerCartService.removeCoupon(VALID_SESSION_ID);
//
//            assertNotNull(response);
//            assertEquals(VALID_SESSION_ID, response.getSessionId());
//            assertNull(sessionCart.getCoupon());
//            verify(cartRepository).findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID);
//            verify(cartRepository).save(sessionCart);
//            verify(cartMapper).toResponse(sessionCart);
//        }
//
//        @Test
//        void removeCoupon_cartNotFound_throwsCartNotFoundException() {
//            when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.empty());
//
//            assertThrows(CartNotFoundException.class,
//                    () -> buyerCartService.removeCoupon(VALID_SESSION_ID));
//            verify(cartRepository).findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID);
//            verify(cartRepository, never()).save(any(Cart.class));
//            verify(cartMapper, never()).toResponse(any(Cart.class));
//        }
//
//        @Test
//        void removeCoupon_noCoupon_success() {
//            System.out.println("Testing removeCoupon_noCoupon_success with sessionId: " + VALID_SESSION_ID + ", cart: " + sessionCart);
//            sessionCart.setCoupon(null);
//            when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
//            when(cartRepository.save(sessionCart)).thenReturn(sessionCart);
//            when(cartMapper.toResponse(sessionCart)).thenReturn(sessionCartResponse);
//
//            CartResponse response = buyerCartService.removeCoupon(VALID_SESSION_ID);
//
//            assertNotNull(response);
//            assertEquals(VALID_SESSION_ID, response.getSessionId());
//            assertNull(sessionCart.getCoupon());
//            verify(cartRepository).findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID);
//            verify(cartRepository).save(sessionCart);
//            verify(cartMapper).toResponse(sessionCart);
//        }
//    }
//
//    @Nested
//    class RemoveCouponForUserTests {
//        @Test
//        void removeCouponForUser_validRequest_success() {
//            userCart.setCoupon(coupon);
//            when(currentUserService.getCurrentUser()).thenReturn(user);
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//            when(cartRepository.save(userCart)).thenReturn(userCart);
//            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
//
//            CartResponse response = buyerCartService.removeCouponForUser();
//
//            assertNotNull(response);
//            assertNull(response.getSessionId());
//            assertNull(userCart.getCoupon());
//            verify(cartRepository).save(userCart);
//            verify(cartMapper).toResponse(userCart);
//        }
//    }
//
//    @Nested
//    class CalculateTotalAmountTests {
//        @Test
//        void calculateTotalAmount_validUserId_success() {
//            userCart.setTotalPrice(new BigDecimal("199.98"));
//            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//
//            BigDecimal total = buyerCartService.calculateTotalAmount(1L);
//
//            assertEquals(new BigDecimal("199.98"), total);
//            verify(cartRepository).findByUserWithItems(user);
//        }
//
//        @Test
//        void calculateTotalAmount_userNotFound_throwsException() {
//            when(userRepository.findById(1L)).thenReturn(Optional.empty());
//
//            assertThrows(Exception.class, () -> buyerCartService.calculateTotalAmount(1L));
//            verify(cartRepository, never()).findByUserWithItems(any(User.class));
//        }
//    }
//
//    @Nested
//    class CalculateDiscountTests {
//        @Test
//        void calculateDiscount_validUserId_success() {
//            userCart.setDiscount(new BigDecimal("20.00"));
//            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//
//            BigDecimal discount = buyerCartService.calculateDiscount(1L);
//
//            assertEquals(new BigDecimal("20.00"), discount);
//            verify(cartRepository).findByUserWithItems(user);
//        }
//    }
//}

//package com.datasaz.ecommerce.services.implementations;
//
//import com.datasaz.ecommerce.exceptions.BadRequestException;
//import com.datasaz.ecommerce.exceptions.CartNotFoundException;
//import com.datasaz.ecommerce.exceptions.InsufficientStockException;
//import com.datasaz.ecommerce.exceptions.ProductNotFoundException;
//import com.datasaz.ecommerce.mappers.CartMapper;
//import com.datasaz.ecommerce.models.request.CartRequest;
//import com.datasaz.ecommerce.models.response.AppliedCouponResponse;
//import com.datasaz.ecommerce.models.response.CartItemResponse;
//import com.datasaz.ecommerce.models.response.CartResponse;
//import com.datasaz.ecommerce.repositories.CartRepository;
//import com.datasaz.ecommerce.repositories.ProductRepository;
//import com.datasaz.ecommerce.repositories.UserRepository;
//import com.datasaz.ecommerce.repositories.entities.*;
//import com.datasaz.ecommerce.services.interfaces.ICouponService;
//import com.datasaz.ecommerce.utilities.CurrentUserService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.mockito.junit.jupiter.MockitoSettings;
//import org.mockito.quality.Strictness;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
/// /@MockitoSettings(strictness = Strictness.LENIENT) // Temporary to avoid UnnecessaryStubbingException
//class BuyerCartServiceTest {
//
//    @Mock
//    private CartRepository cartRepository;
//
//    @Mock
//    private ProductRepository productRepository;
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private CartMapper cartMapper;
//
//    @Mock
//    private ICouponService couponService;
//
//    @Mock
//    private CurrentUserService currentUserService;
//
//    @InjectMocks
//    private BuyerCartService buyerCartService;
//
//    private Cart sessionCart;
//    private Cart userCart;
//    private Product product;
//    private User user;
//    private Company company;
//    private Coupon coupon;
//    private CartItem cartItem;
//    private CartResponse sessionCartResponse;
//    private CartResponse userCartResponse;
//    private static final String VALID_SESSION_ID = "eafa58da-f94b-4e68-90c5-169a7cd1b1c1";
//
//    @BeforeEach
//    void setUp() {
//        // Reset mocks to avoid test interference
//        Mockito.reset(cartRepository, productRepository, userRepository, cartMapper, couponService, currentUserService);
//
//        company = new Company();
//        company.setId(1L);
//        company.setName("Test Company");
//
//        user = new User();
//        user.setId(1L);
//        user.setEmailAddress("buyer@test.com");
//        user.setCompany(company);
//
//        product = new Product();
//        product.setId(1L);
//        product.setPrice(new BigDecimal("99.99"));
//        product.setName("Test Product");
//        product.setQuantity(10);
//        product.setAuthor(user);
//        product.setCompany(company);
//
//        cartItem = CartItem.builder()
//                .id(1L)
//                .product(product)
//                .productName(product.getName())
//                .price(product.getPrice())
//                .quantity(2)
//                .cart(null) // Set later to avoid circular reference
//                .build();
//
//        sessionCart = Cart.builder()
//                .id(1L)
//                .sessionId(VALID_SESSION_ID)
//                .user(null)
//                .items(new ArrayList<>())
//                .totalPrice(new BigDecimal("199.98"))
//                .discount(BigDecimal.ZERO)
//                .lastModified(LocalDateTime.now())
//                .version(1L) // Added to ensure valid cart state
//                .build();
//        cartItem.setCart(sessionCart);
//        sessionCart.getItems().add(cartItem);
//
//        userCart = Cart.builder()
//                .id(2L)
//                .sessionId(null)
//                .user(user)
//                .items(new ArrayList<>())
//                .totalPrice(BigDecimal.ZERO)
//                .discount(BigDecimal.ZERO)
//                .lastModified(LocalDateTime.now())
//                .version(1L)
//                .build();
//
//        coupon = Coupon.builder()
//                .code("SAVE10")
//                .state(CouponState.ACTIVE)
//                .category(CouponCategory.GENERAL)
//                .couponScope(CouponScope.ORDER)
//                .couponType(CouponType.PERCENTAGE)
//                .discountPercentage(new BigDecimal("10"))
//                .startFrom(LocalDateTime.now().minusDays(1))
//                .endAt(LocalDateTime.now().plusDays(1))
//                .maxUsesPerUser(1)
//                .maxUses(10)
//                .author(user)
//                .couponTrackings(new HashSet<>())
//                .version(1L)
//                .build();
//
//        sessionCartResponse = CartResponse.builder()
//                .id(1L)
//                .sessionId(VALID_SESSION_ID)
//                .user(null)
//                .items(List.of(CartItemResponse.builder()
//                        .id(1L)
//                        .productId(1L)
//                        .productName("Test Product")
//                        .price(new BigDecimal("99.99"))
//                        .quantity(2)
//                        .build()))
//                .subtotal(new BigDecimal("199.98"))
//                .discountAmount(BigDecimal.ZERO)
//                .totalAmount(new BigDecimal("199.98"))
//                .build();
//
//        userCartResponse = CartResponse.builder()
//                .id(2L)
//                .sessionId(null)
//                .user(user)
//                .items(new ArrayList<>())
//                .subtotal(BigDecimal.ZERO)
//                .discountAmount(BigDecimal.ZERO)
//                .totalAmount(BigDecimal.ZERO)
//                .build();
//    }
//
//    @Nested
//    class AddToCartTests {
//        @Test
//        void addToCart_newCart_success() {
//            CartRequest cartRequest = CartRequest.builder()
//                    .productId(1L)
//                    .quantity(1)
//                    .build();
//            Cart newCart = Cart.builder()
//                    .id(3L)
//                    .sessionId(VALID_SESSION_ID)
//                    .items(new ArrayList<>())
//                    .totalPrice(BigDecimal.ZERO)
//                    .discount(BigDecimal.ZERO)
//                    .lastModified(LocalDateTime.now())
//                    .version(1L)
//                    .build();
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());
//            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//            when(cartRepository.save(any(Cart.class))).thenReturn(newCart).thenReturn(newCart);
//            when(cartMapper.toResponse(any(Cart.class))).thenReturn(sessionCartResponse);
//
//            CartResponse response = buyerCartService.addToCart(VALID_SESSION_ID, cartRequest);
//
//            assertNotNull(response);
//            assertEquals(VALID_SESSION_ID, response.getSessionId());
//            assertNull(response.getUser());
//            verify(cartRepository, times(2)).save(any(Cart.class));
//            verify(cartMapper).toResponse(any(Cart.class));
//        }
//
//        @Test
//        void addToCart_existingCart_success() {
//            CartRequest cartRequest = CartRequest.builder()
//                    .productId(1L)
//                    .quantity(1)
//                    .build();
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
//            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//            when(cartRepository.save(any(Cart.class))).thenReturn(sessionCart);
//            when(cartMapper.toResponse(sessionCart)).thenReturn(sessionCartResponse);
//
//            CartResponse response = buyerCartService.addToCart(VALID_SESSION_ID, cartRequest);
//
//            assertNotNull(response);
//            assertEquals(VALID_SESSION_ID, response.getSessionId());
//            assertNull(response.getUser());
//            verify(cartRepository).save(sessionCart);
//            verify(cartMapper).toResponse(sessionCart);
//        }
//
//        @Test
//        void addToCart_invalidSessionId_throwsBadRequestException() {
//            CartRequest cartRequest = CartRequest.builder()
//                    .productId(1L)
//                    .quantity(1)
//                    .build();
//            assertThrows(BadRequestException.class,
//                    () -> buyerCartService.addToCart("invalid-session", cartRequest));
//            verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(anyString());
//            verify(cartRepository, never()).save(any(Cart.class));
//        }
//
//        @Test
//        void addToCart_productNotFound_throwsProductNotFoundException() {
//            CartRequest cartRequest = CartRequest.builder()
//                    .productId(1L)
//                    .quantity(1)
//                    .build();
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
//            when(productRepository.findById(1L)).thenReturn(Optional.empty());
//
//            assertThrows(ProductNotFoundException.class,
//                    () -> buyerCartService.addToCart(VALID_SESSION_ID, cartRequest));
//            verify(cartRepository, never()).save(any(Cart.class));
//        }
//
//        @Test
//        void addToCart_insufficientStock_throwsInsufficientStockException() {
//            CartRequest cartRequest = CartRequest.builder()
//                    .productId(1L)
//                    .quantity(19)
//                    .build();
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
//            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//
//            InsufficientStockException exception = assertThrows(InsufficientStockException.class,
//                    () -> buyerCartService.addToCart(VALID_SESSION_ID, cartRequest));
//
//            assertEquals("Insufficient stock for product ID: 1, requested: 21, available: 10", exception.getMessage());
//            verify(cartRepository, never()).save(any(Cart.class));
//        }
//    }
//
//    @Nested
//    class AddToCartForUserTests {
//        @Test
//        void addToCartForUser_newCart_success() {
//            CartRequest cartRequest = CartRequest.builder()
//                    .productId(1L)
//                    .quantity(1)
//                    .build();
//            when(currentUserService.getCurrentUser()).thenReturn(user);
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.empty());
//            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//            when(cartRepository.save(any(Cart.class))).thenReturn(userCart).thenReturn(userCart);
//            when(cartMapper.toResponse(any(Cart.class))).thenReturn(userCartResponse);
//
//            CartResponse response = buyerCartService.addToCartForUser(cartRequest);
//
//            assertNotNull(response);
//            assertNull(response.getSessionId());
//            assertEquals(user, response.getUser());
//            verify(cartRepository, times(2)).save(any(Cart.class));
//            verify(cartMapper).toResponse(any(Cart.class));
//        }
//
//        @Test
//        void addToCartForUser_existingCart_success() {
//            CartRequest cartRequest = CartRequest.builder()
//                    .productId(1L)
//                    .quantity(1)
//                    .build();
//            userCart.getItems().add(cartItem);
//            when(currentUserService.getCurrentUser()).thenReturn(user);
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//            when(cartRepository.save(any(Cart.class))).thenReturn(userCart);
//            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
//
//            CartResponse response = buyerCartService.addToCartForUser(cartRequest);
//
//            assertNotNull(response);
//            assertNull(response.getSessionId());
//            assertEquals(user, response.getUser());
//            verify(cartRepository).save(userCart);
//            verify(cartMapper).toResponse(userCart);
//        }
//
//        @Test
//        void addToCartForUser_productNotFound_throwsProductNotFoundException() {
//            CartRequest cartRequest = CartRequest.builder()
//                    .productId(1L)
//                    .quantity(1)
//                    .build();
//            when(currentUserService.getCurrentUser()).thenReturn(user);
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//            when(productRepository.findById(1L)).thenReturn(Optional.empty());
//
//            assertThrows(ProductNotFoundException.class,
//                    () -> buyerCartService.addToCartForUser(cartRequest));
//            verify(cartRepository, never()).save(any(Cart.class));
//        }
//    }
//
//    @Nested
//    class UpdateCartItemTests {
//        @Test
//        void updateCartItem_validRequest_success() {
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
//            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//            when(cartRepository.save(sessionCart)).thenReturn(sessionCart);
//            when(cartMapper.toResponse(sessionCart)).thenReturn(sessionCartResponse);
//
//            CartResponse response = buyerCartService.updateCartItem(VALID_SESSION_ID, 1L, 3);
//
//            assertNotNull(response);
//            assertEquals(VALID_SESSION_ID, response.getSessionId());
//            assertEquals(3, sessionCart.getItems().get(0).getQuantity());
//            verify(cartRepository).save(sessionCart);
//            verify(cartMapper).toResponse(sessionCart);
//        }
//
//        @Test
//        void updateCartItem_cartNotFound_throwsCartNotFoundException() {
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());
//
//            assertThrows(CartNotFoundException.class,
//                    () -> buyerCartService.updateCartItem(VALID_SESSION_ID, 1L, 3));
//            verify(cartRepository, never()).save(any(Cart.class));
//        }
//
//        @Test
//        void updateCartItem_invalidSessionId_throwsBadRequestException() {
//            BadRequestException exception = assertThrows(BadRequestException.class,
//                    () -> buyerCartService.updateCartItem("invalid-session", 1L, 3));
//
//            assertEquals("Invalid session ID format", exception.getMessage());
//            verify(cartRepository, never()).save(any(Cart.class));
//        }
//    }
//
//    @Nested
//    class UpdateCartItemForUserTests {
//        @Test
//        void updateCartItemForUser_validRequest_success() {
//            userCart.getItems().add(cartItem);
//            when(currentUserService.getCurrentUser()).thenReturn(user);
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//            when(cartRepository.save(userCart)).thenReturn(userCart);
//            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
//
//            CartResponse response = buyerCartService.updateCartItemForUser(1L, 3);
//
//            assertNotNull(response);
//            assertNull(response.getSessionId());
//            assertEquals(user, response.getUser());
//            assertEquals(3, userCart.getItems().get(0).getQuantity());
//            verify(cartRepository).save(userCart);
//            verify(cartMapper).toResponse(userCart);
//        }
//
//        @Test
//        void updateCartItemForUser_cartNotFound_throwsCartNotFoundException() {
//            when(currentUserService.getCurrentUser()).thenReturn(user);
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.empty());
//
//            assertThrows(CartNotFoundException.class,
//                    () -> buyerCartService.updateCartItemForUser(1L, 3));
//            verify(cartRepository, never()).save(any(Cart.class));
//        }
//    }
//
//    @Nested
//    class RemoveFromCartTests {
//        @Test
//        void removeFromCart_validRequest_success() {
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
//            when(cartRepository.save(sessionCart)).thenReturn(sessionCart);
//            when(cartMapper.toResponse(sessionCart)).thenReturn(sessionCartResponse);
//
//            CartResponse response = buyerCartService.removeFromCart(VALID_SESSION_ID, 1L);
//
//            assertNotNull(response);
//            assertEquals(VALID_SESSION_ID, response.getSessionId());
//            assertTrue(sessionCart.getItems().isEmpty());
//            verify(cartRepository).save(sessionCart);
//            verify(cartMapper).toResponse(sessionCart);
//        }
//
//        @Test
//        void removeFromCart_cartNotFound_throwsCartNotFoundException() {
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());
//
//            assertThrows(CartNotFoundException.class,
//                    () -> buyerCartService.removeFromCart(VALID_SESSION_ID, 1L));
//            verify(cartRepository, never()).save(any(Cart.class));
//        }
//    }
//
//    @Nested
//    class RemoveFromCartForUserTests {
//        @Test
//        void removeFromCartForUser_validRequest_success() {
//            userCart.getItems().add(cartItem);
//            when(currentUserService.getCurrentUser()).thenReturn(user);
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//            when(cartRepository.save(userCart)).thenReturn(userCart);
//            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
//
//            CartResponse response = buyerCartService.removeFromCartForUser(1L);
//
//            assertNotNull(response);
//            assertNull(response.getSessionId());
//            assertTrue(userCart.getItems().isEmpty());
//            verify(cartRepository).save(userCart);
//            verify(cartMapper).toResponse(userCart);
//        }
//    }
//
//    @Nested
//    class ClearCartTests {
//        @Test
//        void clearCart_validRequest_success() {
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
//            when(cartRepository.save(sessionCart)).thenReturn(sessionCart);
//            when(cartMapper.toResponse(sessionCart)).thenReturn(sessionCartResponse);
//
//            CartResponse response = buyerCartService.clearCart(VALID_SESSION_ID);
//
//            assertNotNull(response);
//            assertEquals(VALID_SESSION_ID, response.getSessionId());
//            assertTrue(sessionCart.getItems().isEmpty());
//            verify(cartRepository).save(sessionCart);
//            verify(cartMapper).toResponse(sessionCart);
//        }
//
//        @Test
//        void clearCart_cartNotFound_throwsCartNotFoundException() {
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());
//
//            assertThrows(CartNotFoundException.class,
//                    () -> buyerCartService.clearCart(VALID_SESSION_ID));
//            verify(cartRepository, never()).save(any(Cart.class));
//        }
//    }
//
//    @Nested
//    class ClearCartForUserTests {
//        @Test
//        void clearCartForUser_validRequest_success() {
//            userCart.getItems().add(cartItem);
//            when(currentUserService.getCurrentUser()).thenReturn(user);
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//            when(cartRepository.save(userCart)).thenReturn(userCart);
//            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
//
//            CartResponse response = buyerCartService.clearCartForUser();
//
//            assertNotNull(response);
//            assertNull(response.getSessionId());
//            assertTrue(userCart.getItems().isEmpty());
//            verify(cartRepository).save(userCart);
//            verify(cartMapper).toResponse(userCart);
//        }
//    }
//
//    @Nested
//    class GetCartTests {
//        @Test
//        void getCart_validSessionId_success() {
//            Pageable pageable = PageRequest.of(0, 10);
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
//            when(cartMapper.toResponse(sessionCart, pageable)).thenReturn(sessionCartResponse);
//
//            CartResponse response = buyerCartService.getCart(VALID_SESSION_ID, pageable);
//
//            assertNotNull(response);
//            assertEquals(VALID_SESSION_ID, response.getSessionId());
//            assertNull(response.getUser());
//            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
//            verify(cartMapper).toResponse(sessionCart, pageable);
//        }
//
//        @Test
//        void getCart_cartNotFound_throwsCartNotFoundException() {
//            Pageable pageable = PageRequest.of(0, 10);
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());
//
//            assertThrows(CartNotFoundException.class,
//                    () -> buyerCartService.getCart(VALID_SESSION_ID, pageable));
//            verify(cartMapper, never()).toResponse(any(Cart.class), any(Pageable.class));
//        }
//
//        @Test
//        void getCart_invalidSessionId_throwsBadRequestException() {
//            Pageable pageable = PageRequest.of(0, 10);
//            BadRequestException exception = assertThrows(BadRequestException.class,
//                    () -> buyerCartService.getCart("invalid-session", pageable));
//
//            assertEquals("Invalid session ID format", exception.getMessage());
//            verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(anyString());
//        }
//    }
//
//    @Nested
//    class GetCartForUserTests {
//        @Test
//        void getCartForUser_validRequest_success() {
//            Pageable pageable = PageRequest.of(0, 10);
//            when(currentUserService.getCurrentUser()).thenReturn(user);
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//            when(cartMapper.toResponse(userCart, pageable)).thenReturn(userCartResponse);
//
//            CartResponse response = buyerCartService.getCartForUser(pageable);
//
//            assertNotNull(response);
//            assertNull(response.getSessionId());
//            assertEquals(user, response.getUser());
//            verify(cartRepository).findByUserWithItems(user);
//            verify(cartMapper).toResponse(userCart, pageable);
//        }
//
//        @Test
//        void getCartForUser_noCart_throwsCartNotFoundException() {
//            Pageable pageable = PageRequest.of(0, 10);
//            when(currentUserService.getCurrentUser()).thenReturn(user);
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.empty());
//
//            assertThrows(CartNotFoundException.class,
//                    () -> buyerCartService.getCartForUser(pageable));
//            verify(cartRepository, never()).save(any(Cart.class));
//            verify(cartMapper, never()).toResponse(any(Cart.class), any(Pageable.class));
//        }
//    }
//
//    @Nested
//    class GetCartByUsersIdTests {
//        @Test
//        void getCartByUsersId_validUserId_success() {
//            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
//
//            CartResponse response = buyerCartService.getCartByUsersId(1L);
//
//            assertNotNull(response);
//            assertNull(response.getSessionId());
//            assertEquals(user, response.getUser());
//            verify(cartRepository).findByUserWithItems(user);
//            verify(cartMapper).toResponse(userCart);
//        }
//
//        @Test
//        void getCartByUsersId_userNotFound_throwsException() {
//            when(userRepository.findById(1L)).thenReturn(Optional.empty());
//
//            assertThrows(Exception.class, () -> buyerCartService.getCartByUsersId(1L));
//            verify(cartRepository, never()).findByUserWithItems(any(User.class));
//        }
//    }
//
//    @Nested
//    class MergeCartOnLoginTests {
//        /*@Test
//        void mergeCartOnLogin_validSessionId_success() {
//            when(currentUserService.getCurrentUser()).thenReturn(user);
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//            when(cartRepository.save(userCart)).thenReturn(userCart);
//            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
//
//            CartResponse response = buyerCartService.mergeCartOnLogin(VALID_SESSION_ID);
//
//            assertNotNull(response);
//            assertNull(response.getSessionId());
//            assertEquals(user, response.getUser());
//            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
//            verify(cartRepository).findByUserWithItems(user);
//            verify(cartRepository).save(userCart);
//            verify(cartMapper).toResponse(userCart);
//        }
//
//        @Test
//        void mergeCartOnLogin_noAnonymousCart_success() {
//            when(currentUserService.getCurrentUser()).thenReturn(user);
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//            when(cartRepository.save(userCart)).thenReturn(userCart);
//            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
//
//            CartResponse response = buyerCartService.mergeCartOnLogin(VALID_SESSION_ID);
//
//            assertNotNull(response);
//            assertNull(response.getSessionId());
//            assertEquals(user, response.getUser());
//            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
//            verify(cartRepository).findByUserWithItems(user);
//            verify(cartRepository).save(userCart);
//            verify(cartMapper).toResponse(userCart);
//        }*/
//
//        @Test
//        void mergeCartOnLogin_invalidSessionId_throwsBadRequestException() {
//            assertThrows(BadRequestException.class,
//                    () -> buyerCartService.mergeCartOnLogin("invalid-session"));
//            verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(anyString());
//            verify(cartRepository, never()).save(any(Cart.class));
//        }
//    }
//
//    /*@Nested
//    class ApplyCouponTests {
//        @Test
//        void applyCoupon_validCoupon_success() {
//            System.out.println("Testing applyCoupon_validCoupon_success with sessionId: " + VALID_SESSION_ID + ", cart: " + sessionCart);
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
//            when(couponService.validateCoupon(eq("SAVE10"), isNull(), eq(new BigDecimal("199.98")), eq(sessionCart.getItems()))).thenReturn(coupon);
//            when(couponService.calculateDiscount(eq(coupon), eq(sessionCart.getItems()), eq(new BigDecimal("199.98")))).thenReturn(new BigDecimal("20.00"));
//            when(cartRepository.save(sessionCart)).thenReturn(sessionCart);
//            when(cartMapper.toResponse(sessionCart)).thenReturn(sessionCartResponse);
//
//            AppliedCouponResponse response = buyerCartService.applyCoupon(VALID_SESSION_ID, "SAVE10");
//
//            assertNotNull(response);
//            assertEquals(new BigDecimal("20.00"), response.getDiscount());
//            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
//            verify(couponService).validateCoupon(eq("SAVE10"), isNull(), eq(new BigDecimal("199.98")), eq(sessionCart.getItems()));
//            verify(couponService).calculateDiscount(eq(coupon), eq(sessionCart.getItems()), eq(new BigDecimal("199.98")));
//            verify(cartRepository).save(sessionCart);
//            verify(cartMapper).toResponse(sessionCart);
//        }
//
//        @Test
//        void applyCoupon_cartNotFound_throwsCartNotFoundException() {
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());
//
//            assertThrows(CartNotFoundException.class,
//                    () -> buyerCartService.applyCoupon(VALID_SESSION_ID, "SAVE10"));
//            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
//            verify(cartRepository, never()).save(any(Cart.class));
//            verify(cartMapper, never()).toResponse(any(Cart.class));
//            verify(couponService, never()).validateCoupon(anyString(), any(), any(), any());
//        }
//
//        @Test
//        void applyCoupon_invalidCoupon_throwsBadRequestException() {
//            System.out.println("Testing applyCoupon_invalidCoupon_throwsBadRequestException with sessionId: " + VALID_SESSION_ID + ", cart: " + sessionCart);
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
//            when(couponService.validateCoupon(eq("INVALID"), isNull(), eq(new BigDecimal("199.98")), eq(sessionCart.getItems())))
//                    .thenThrow(BadRequestException.builder().message("Invalid coupon code").build());
//
//            assertThrows(BadRequestException.class,
//                    () -> buyerCartService.applyCoupon(VALID_SESSION_ID, "INVALID"));
//            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
//            verify(couponService).validateCoupon(eq("INVALID"), isNull(), eq(new BigDecimal("199.98")), eq(sessionCart.getItems()));
//            verify(cartRepository, never()).save(any(Cart.class));
//            verify(cartMapper, never()).toResponse(any(Cart.class));
//        }
//    }*/
//
//    @Nested
//    class ApplyCouponForUserTests {
//        @Test
//        void applyCouponForUser_validCoupon_success() {
//            userCart.getItems().add(cartItem);
//            when(currentUserService.getCurrentUser()).thenReturn(user);
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//            when(couponService.validateCoupon("SAVE10", user, new BigDecimal("199.98"), userCart.getItems())).thenReturn(coupon);
//            when(couponService.calculateDiscount(coupon, userCart.getItems(), new BigDecimal("199.98"))).thenReturn(new BigDecimal("20.00"));
//            when(cartRepository.save(userCart)).thenReturn(userCart);
//            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
//
//            AppliedCouponResponse response = buyerCartService.applyCouponForUser("SAVE10");
//
//            assertNotNull(response);
//            assertEquals(new BigDecimal("20.00"), response.getDiscount());
//            verify(cartRepository).save(userCart);
//            verify(cartMapper).toResponse(userCart);
//        }
//    }
//
//    /*@Nested
//    class RemoveCouponTests {
//        @Test
//        void removeCoupon_validSessionId_success() {
//            System.out.println("Testing removeCoupon_validSessionId_success with sessionId: " + VALID_SESSION_ID + ", cart: " + sessionCart);
//            sessionCart.setCoupon(coupon);
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
//            when(cartRepository.save(sessionCart)).thenReturn(sessionCart);
//            when(cartMapper.toResponse(sessionCart)).thenReturn(sessionCartResponse);
//
//            CartResponse response = buyerCartService.removeCoupon(VALID_SESSION_ID);
//
//            assertNotNull(response);
//            assertEquals(VALID_SESSION_ID, response.getSessionId());
//            assertNull(sessionCart.getCoupon());
//            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
//            verify(cartRepository).save(sessionCart);
//            verify(cartMapper).toResponse(sessionCart);
//        }
//
//        @Test
//        void removeCoupon_cartNotFound_throwsCartNotFoundException() {
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());
//
//            assertThrows(CartNotFoundException.class,
//                    () -> buyerCartService.removeCoupon(VALID_SESSION_ID));
//            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
//            verify(cartRepository, never()).save(any(Cart.class));
//            verify(cartMapper, never()).toResponse(any(Cart.class));
//        }
//
//        @Test
//        void removeCoupon_noCoupon_success() {
//            System.out.println("Testing removeCoupon_noCoupon_success with sessionId: " + VALID_SESSION_ID + ", cart: " + sessionCart);
//            sessionCart.setCoupon(null);
//            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
//            when(cartRepository.save(sessionCart)).thenReturn(sessionCart);
//            when(cartMapper.toResponse(sessionCart)).thenReturn(sessionCartResponse);
//
//            CartResponse response = buyerCartService.removeCoupon(VALID_SESSION_ID);
//
//            assertNotNull(response);
//            assertEquals(VALID_SESSION_ID, response.getSessionId());
//            assertNull(sessionCart.getCoupon());
//            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
//            verify(cartRepository).save(sessionCart);
//            verify(cartMapper).toResponse(sessionCart);
//        }
//    }*/
//
//    @Nested
//    class RemoveCouponForUserTests {
//        @Test
//        void removeCouponForUser_validRequest_success() {
//            userCart.setCoupon(coupon);
//            when(currentUserService.getCurrentUser()).thenReturn(user);
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//            when(cartRepository.save(userCart)).thenReturn(userCart);
//            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
//
//            CartResponse response = buyerCartService.removeCouponForUser();
//
//            assertNotNull(response);
//            assertNull(response.getSessionId());
//            assertNull(userCart.getCoupon());
//            verify(cartRepository).save(userCart);
//            verify(cartMapper).toResponse(userCart);
//        }
//    }
//
//    @Nested
//    class CalculateTotalAmountTests {
//        @Test
//        void calculateTotalAmount_validUserId_success() {
//            userCart.setTotalPrice(new BigDecimal("199.98"));
//            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//
//            BigDecimal total = buyerCartService.calculateTotalAmount(1L);
//
//            assertEquals(new BigDecimal("199.98"), total);
//            verify(cartRepository).findByUserWithItems(user);
//        }
//
//        @Test
//        void calculateTotalAmount_userNotFound_throwsException() {
//            when(userRepository.findById(1L)).thenReturn(Optional.empty());
//
//            assertThrows(Exception.class, () -> buyerCartService.calculateTotalAmount(1L));
//            verify(cartRepository, never()).findByUserWithItems(any(User.class));
//        }
//    }
//
//    @Nested
//    class CalculateDiscountTests {
//        @Test
//        void calculateDiscount_validUserId_success() {
//            userCart.setDiscount(new BigDecimal("20.00"));
//            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
//            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
//
//            BigDecimal discount = buyerCartService.calculateDiscount(1L);
//
//            assertEquals(new BigDecimal("20.00"), discount);
//            verify(cartRepository).findByUserWithItems(user);
//        }
//    }
//}



/*package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.CartNotFoundException;
import com.datasaz.ecommerce.exceptions.InsufficientStockException;
import com.datasaz.ecommerce.exceptions.ProductNotFoundException;
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
import com.datasaz.ecommerce.utilities.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuyerCartServiceTest {

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

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private BuyerCartService buyerCartService;

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

    @BeforeEach
    void setUp() {
        // Reset mocks to avoid interference
        reset(cartRepository, productRepository, userRepository, cartMapper, couponService, currentUserService);

        company = new Company();
        company.setId(1L);

        user = new User();
        user.setId(1L);
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
                .cart(null) // Will be set after sessionCart is created
                .build();

        sessionCart = Cart.builder()
                .id(1L)
                .sessionId(VALID_SESSION_ID)
                .user(null)
                .items(new ArrayList<>())
                .totalPrice(new BigDecimal("199.98"))
                .discount(BigDecimal.ZERO)
                .lastModified(LocalDateTime.now())
                .build();
        sessionCart.getItems().add(cartItem);
        cartItem.setCart(sessionCart);

        userCart = Cart.builder()
                .id(2L)
                .sessionId(null)
                .user(user)
                .items(new ArrayList<>())
                .totalPrice(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .lastModified(LocalDateTime.now())
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
                .user(null)
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
                .user(user)
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
                    .build();
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(cartRepository.save(any(Cart.class))).thenReturn(newCart).thenReturn(newCart);
            when(cartMapper.toResponse(any(Cart.class))).thenReturn(sessionCartResponse);

            CartResponse response = buyerCartService.addToCart(VALID_SESSION_ID, cartRequest);

            assertNotNull(response);
            assertEquals(VALID_SESSION_ID, response.getSessionId());
            assertNull(response.getUser());
            verify(cartRepository, times(2)).save(any(Cart.class));
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

            CartResponse response = buyerCartService.addToCart(VALID_SESSION_ID, cartRequest);

            assertNotNull(response);
            assertEquals(VALID_SESSION_ID, response.getSessionId());
            assertNull(response.getUser());
            verify(cartRepository).save(sessionCart);
            verify(cartMapper).toResponse(sessionCart);
        }

        @Test
        void addToCart_invalidSessionId_throwsBadRequestException() {
            CartRequest cartRequest = CartRequest.builder()
                    .productId(1L)
                    .quantity(1)
                    .build();
            assertThrows(BadRequestException.class,
                    () -> buyerCartService.addToCart("invalid-session", cartRequest));
            verify(cartRepository, never()).save(any(Cart.class));
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
                    () -> buyerCartService.addToCart(VALID_SESSION_ID, cartRequest));
            verify(cartRepository, never()).save(any(Cart.class));
        }

        @Test
        void addToCart_insufficientStock_throwsInsufficientStockException() {
            CartRequest cartRequest = CartRequest.builder()
                    .productId(1L)
                    .quantity(19)
                    .build();
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            InsufficientStockException exception = assertThrows(InsufficientStockException.class,
                    () -> buyerCartService.addToCart(VALID_SESSION_ID, cartRequest));

            assertEquals("Insufficient stock for product ID: 1, requested: 21, available: 10", exception.getMessage());
            verify(cartRepository, never()).save(any(Cart.class));
        }
    }

    @Nested
    class AddToCartForUserTests {
        @Test
        void addToCartForUser_newCart_success() {
            CartRequest cartRequest = CartRequest.builder()
                    .productId(1L)
                    .quantity(1)
                    .build();
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.empty());
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(cartRepository.save(any(Cart.class))).thenReturn(userCart).thenReturn(userCart);
            when(cartMapper.toResponse(any(Cart.class))).thenReturn(userCartResponse);

            CartResponse response = buyerCartService.addToCartForUser(cartRequest);

            assertNotNull(response);
            assertNull(response.getSessionId());
            assertEquals(user, response.getUser());
            verify(cartRepository, times(2)).save(any(Cart.class));
            verify(cartMapper).toResponse(any(Cart.class));
        }

        @Test
        void addToCartForUser_existingCart_success() {
            CartRequest cartRequest = CartRequest.builder()
                    .productId(1L)
                    .quantity(1)
                    .build();
            userCart.getItems().add(cartItem);
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(cartRepository.save(any(Cart.class))).thenReturn(userCart);
            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);

            CartResponse response = buyerCartService.addToCartForUser(cartRequest);

            assertNotNull(response);
            assertNull(response.getSessionId());
            assertEquals(user, response.getUser());
            verify(cartRepository).save(userCart);
            verify(cartMapper).toResponse(userCart);
        }

        @Test
        void addToCartForUser_productNotFound_throwsProductNotFoundException() {
            CartRequest cartRequest = CartRequest.builder()
                    .productId(1L)
                    .quantity(1)
                    .build();
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
            when(productRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ProductNotFoundException.class,
                    () -> buyerCartService.addToCartForUser(cartRequest));
            verify(cartRepository, never()).save(any(Cart.class));
        }
    }

    @Nested
    class UpdateCartItemTests {
        @Test
        void updateCartItem_validRequest_success() {
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(cartRepository.save(sessionCart)).thenReturn(sessionCart);
            when(cartMapper.toResponse(sessionCart)).thenReturn(sessionCartResponse);

            CartResponse response = buyerCartService.updateCartItem(VALID_SESSION_ID, 1L, 3);

            assertNotNull(response);
            assertEquals(VALID_SESSION_ID, response.getSessionId());
            assertEquals(3, sessionCart.getItems().get(0).getQuantity());
            verify(cartRepository).save(sessionCart);
            verify(cartMapper).toResponse(sessionCart);
        }

        @Test
        void updateCartItem_cartNotFound_throwsCartNotFoundException() {
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());

            assertThrows(CartNotFoundException.class,
                    () -> buyerCartService.updateCartItem(VALID_SESSION_ID, 1L, 3));
            verify(cartRepository, never()).save(any(Cart.class));
        }

        @Test
        void updateCartItem_invalidSessionId_throwsBadRequestException() {
            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> buyerCartService.updateCartItem("invalid-session", 1L, 3));

            assertEquals("Invalid session ID format", exception.getMessage());
            verify(cartRepository, never()).save(any(Cart.class));
        }
    }

    @Nested
    class UpdateCartItemForUserTests {
        @Test
        void updateCartItemForUser_validRequest_success() {
            userCart.getItems().add(cartItem);
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(cartRepository.save(userCart)).thenReturn(userCart);
            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);

            CartResponse response = buyerCartService.updateCartItemForUser(1L, 3);

            assertNotNull(response);
            assertNull(response.getSessionId());
            assertEquals(user, response.getUser());
            assertEquals(3, userCart.getItems().get(0).getQuantity());
            verify(cartRepository).save(userCart);
            verify(cartMapper).toResponse(userCart);
        }

        @Test
        void updateCartItemForUser_cartNotFound_throwsCartNotFoundException() {
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.empty());

            assertThrows(CartNotFoundException.class,
                    () -> buyerCartService.updateCartItemForUser(1L, 3));
            verify(cartRepository, never()).save(any(Cart.class));
        }
    }

    @Nested
    class RemoveFromCartTests {
        @Test
        void removeFromCart_validRequest_success() {
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
            when(cartRepository.save(sessionCart)).thenReturn(sessionCart);
            when(cartMapper.toResponse(sessionCart)).thenReturn(sessionCartResponse);

            CartResponse response = buyerCartService.removeFromCart(VALID_SESSION_ID, 1L);

            assertNotNull(response);
            assertEquals(VALID_SESSION_ID, response.getSessionId());
            assertTrue(sessionCart.getItems().isEmpty());
            verify(cartRepository).save(sessionCart);
            verify(cartMapper).toResponse(sessionCart);
        }

        @Test
        void removeFromCart_cartNotFound_throwsCartNotFoundException() {
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());

            assertThrows(CartNotFoundException.class,
                    () -> buyerCartService.removeFromCart(VALID_SESSION_ID, 1L));
            verify(cartRepository, never()).save(any(Cart.class));
        }
    }

    @Nested
    class RemoveFromCartForUserTests {
        @Test
        void removeFromCartForUser_validRequest_success() {
            userCart.getItems().add(cartItem);
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
            when(cartRepository.save(userCart)).thenReturn(userCart);
            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);

            CartResponse response = buyerCartService.removeFromCartForUser(1L);

            assertNotNull(response);
            assertNull(response.getSessionId());
            assertTrue(userCart.getItems().isEmpty());
            verify(cartRepository).save(userCart);
            verify(cartMapper).toResponse(userCart);
        }
    }

    @Nested
    class ClearCartTests {
        @Test
        void clearCart_validRequest_success() {
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
            when(cartRepository.save(sessionCart)).thenReturn(sessionCart);
            when(cartMapper.toResponse(sessionCart)).thenReturn(sessionCartResponse);

            CartResponse response = buyerCartService.clearCart(VALID_SESSION_ID);

            assertNotNull(response);
            assertEquals(VALID_SESSION_ID, response.getSessionId());
            assertTrue(sessionCart.getItems().isEmpty());
            verify(cartRepository).save(sessionCart);
            verify(cartMapper).toResponse(sessionCart);
        }

        @Test
        void clearCart_cartNotFound_throwsCartNotFoundException() {
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());

            assertThrows(CartNotFoundException.class,
                    () -> buyerCartService.clearCart(VALID_SESSION_ID));
            verify(cartRepository, never()).save(any(Cart.class));
        }
    }

    @Nested
    class ClearCartForUserTests {
        @Test
        void clearCartForUser_validRequest_success() {
            userCart.getItems().add(cartItem);
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
            when(cartRepository.save(userCart)).thenReturn(userCart);
            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);

            CartResponse response = buyerCartService.clearCartForUser();

            assertNotNull(response);
            assertNull(response.getSessionId());
            assertTrue(userCart.getItems().isEmpty());
            verify(cartRepository).save(userCart);
            verify(cartMapper).toResponse(userCart);
        }
    }

    @Nested
    class GetCartTests {
        @Test
        void getCart_validSessionId_success() {
            Pageable pageable = PageRequest.of(0, 10);
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
            when(cartMapper.toResponse(sessionCart, pageable)).thenReturn(sessionCartResponse);

            CartResponse response = buyerCartService.getCart(VALID_SESSION_ID, pageable);

            assertNotNull(response);
            assertEquals(VALID_SESSION_ID, response.getSessionId());
            assertNull(response.getUser());
            verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
            verify(cartMapper).toResponse(sessionCart, pageable);
        }

        @Test
        void getCart_cartNotFound_throwsCartNotFoundException() {
            Pageable pageable = PageRequest.of(0, 10);
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());

            assertThrows(CartNotFoundException.class,
                    () -> buyerCartService.getCart(VALID_SESSION_ID, pageable));
            verify(cartMapper, never()).toResponse(any(Cart.class), any(Pageable.class));
        }

        @Test
        void getCart_invalidSessionId_throwsBadRequestException() {
            Pageable pageable = PageRequest.of(0, 10);
            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> buyerCartService.getCart("invalid-session", pageable));

            assertEquals("Invalid session ID format", exception.getMessage());
            verify(cartRepository, never()).findBySessionIdWithItemsAndCoupons(anyString());
        }
    }

    @Nested
    class GetCartForUserTests {
        @Test
        void getCartForUser_validRequest_success() {
            Pageable pageable = PageRequest.of(0, 10);
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
            when(cartMapper.toResponse(userCart, pageable)).thenReturn(userCartResponse);

            CartResponse response = buyerCartService.getCartForUser(pageable);

            assertNotNull(response);
            assertNull(response.getSessionId());
            assertEquals(user, response.getUser());
            verify(cartRepository).findByUserWithItems(user);
            verify(cartMapper).toResponse(userCart, pageable);
        }

        @Test
        void getCartForUser_noCart_throwsCartNotFoundException() {
            Pageable pageable = PageRequest.of(0, 10);
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.empty());

            assertThrows(CartNotFoundException.class,
                    () -> buyerCartService.getCartForUser(pageable));
            verify(cartRepository, never()).save(any(Cart.class));
            verify(cartMapper, never()).toResponse(any(Cart.class), any(Pageable.class));
        }
    }

    @Nested
    class GetCartByUsersIdTests {
        @Test
        void getCartByUsersId_validUserId_success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);

            CartResponse response = buyerCartService.getCartByUsersId(1L);

            assertNotNull(response);
            assertNull(response.getSessionId());
            assertEquals(user, response.getUser());
            verify(cartRepository).findByUserWithItems(user);
            verify(cartMapper).toResponse(userCart);
        }

        @Test
        void getCartByUsersId_userNotFound_throwsException() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(Exception.class, () -> buyerCartService.getCartByUsersId(1L));
            verify(cartRepository, never()).findByUserWithItems(any(User.class));
        }
    }

    *//*@Nested
    class MergeCartOnLoginTests {
        @Test
        void mergeCartOnLogin_validSessionId_success() {
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(sessionCart));
            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
            when(cartRepository.save(userCart)).thenReturn(userCart);
            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);

            CartResponse response = buyerCartService.mergeCartOnLogin(VALID_SESSION_ID);

            assertNotNull(response);
            assertNull(response.getSessionId());
            assertEquals(user, response.getUser());
            verify(cartRepository).save(userCart);
            verify(cartMapper).toResponse(userCart);
        }

        @Test
        void mergeCartOnLogin_noAnonymousCart_success() {
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.empty());
            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
            when(cartRepository.save(userCart)).thenReturn(userCart);
            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);

            CartResponse response = buyerCartService.mergeCartOnLogin(VALID_SESSION_ID);

            assertNotNull(response);
            assertNull(response.getSessionId());
            assertEquals(user, response.getUser());
            verify(cartRepository).save(userCart);
            verify(cartMapper).toResponse(userCart);
        }

        @Test
        void mergeCartOnLogin_invalidSessionId_throwsBadRequestException() {
            assertThrows(BadRequestException.class,
                    () -> buyerCartService.mergeCartOnLogin("invalid-session"));
        }
    }*//*

 *//*@Nested
    class ApplyCouponTests {
        @Test
        void applyCoupon_validCoupon_success() {
            System.out.println("Testing applyCoupon_validCoupon_success with sessionId: " + VALID_SESSION_ID + ", cart: " + sessionCart);
            when(cartRepository.findBySessionIdWithItemsAndCoupons(eq(VALID_SESSION_ID))).thenReturn(Optional.of(sessionCart));
            when(couponService.validateCoupon(eq("SAVE10"), isNull(), eq(new BigDecimal("199.98")), eq(sessionCart.getItems()))).thenReturn(coupon);
            when(couponService.calculateDiscount(eq(coupon), eq(sessionCart.getItems()), eq(new BigDecimal("199.98")))).thenReturn(new BigDecimal("20.00"));
            when(cartRepository.save(same(sessionCart))).thenReturn(sessionCart);
            when(cartMapper.toResponse(same(sessionCart))).thenReturn(sessionCartResponse);

            AppliedCouponResponse response = buyerCartService.applyCoupon(VALID_SESSION_ID, "SAVE10");

            assertNotNull(response);
            assertEquals(new BigDecimal("20.00"), response.getDiscount());
            verify(cartRepository).save(same(sessionCart));
            verify(cartMapper).toResponse(same(sessionCart));
        }

        @Test
        void applyCoupon_cartNotFound_throwsCartNotFoundException() {
            System.out.println("Testing applyCoupon_cartNotFound_throwsCartNotFoundException with sessionId: " + VALID_SESSION_ID);
            when(cartRepository.findBySessionIdWithItemsAndCoupons(eq(VALID_SESSION_ID))).thenReturn(Optional.empty());

            assertThrows(CartNotFoundException.class,
                    () -> buyerCartService.applyCoupon(VALID_SESSION_ID, "SAVE10"));
            verify(cartRepository, never()).save(any(Cart.class));
            verify(cartMapper, never()).toResponse(any(Cart.class));
        }

        @Test
        void applyCoupon_invalidCoupon_throwsBadRequestException() {
            System.out.println("Testing applyCoupon_invalidCoupon_throwsBadRequestException with sessionId: " + VALID_SESSION_ID + ", cart: " + sessionCart);
            when(cartRepository.findBySessionIdWithItemsAndCoupons(eq(VALID_SESSION_ID))).thenReturn(Optional.of(sessionCart));
            when(couponService.validateCoupon(eq("INVALID"), isNull(), eq(new BigDecimal("199.98")), eq(sessionCart.getItems())))
                    .thenThrow(BadRequestException.builder().message("Invalid coupon code").build());

            assertThrows(BadRequestException.class,
                    () -> buyerCartService.applyCoupon(VALID_SESSION_ID, "INVALID"));
            verify(cartRepository, never()).save(any(Cart.class));
            verify(cartMapper, never()).toResponse(any(Cart.class));
        }
    }*//*

    @Nested
    class ApplyCouponForUserTests {
        @Test
        void applyCouponForUser_validCoupon_success() {
            userCart.getItems().add(cartItem);
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
            when(couponService.validateCoupon("SAVE10", user, new BigDecimal("199.98"), userCart.getItems())).thenReturn(coupon);
            when(couponService.calculateDiscount(coupon, userCart.getItems(), new BigDecimal("199.98"))).thenReturn(new BigDecimal("20.00"));
            when(cartRepository.save(userCart)).thenReturn(userCart);
            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);

            AppliedCouponResponse response = buyerCartService.applyCouponForUser("SAVE10");

            assertNotNull(response);
            assertEquals(new BigDecimal("20.00"), response.getDiscount());
            verify(cartRepository).save(userCart);
            verify(cartMapper).toResponse(userCart);
        }
    }

    *//*@Nested
    class RemoveCouponTests {
        @Test
        void removeCoupon_validSessionId_success() {
            System.out.println("Testing removeCoupon_validSessionId_success with sessionId: " + VALID_SESSION_ID + ", cart: " + sessionCart);
            sessionCart.setCoupon(coupon);
            when(cartRepository.findBySessionIdWithItemsAndCoupons(eq(VALID_SESSION_ID))).thenReturn(Optional.of(sessionCart));
            when(cartRepository.save(same(sessionCart))).thenReturn(sessionCart);
            when(cartMapper.toResponse(same(sessionCart))).thenReturn(sessionCartResponse);

            CartResponse response = buyerCartService.removeCoupon(VALID_SESSION_ID);

            assertNotNull(response);
            assertEquals(VALID_SESSION_ID, response.getSessionId());
            assertNull(sessionCart.getCoupon());
            verify(cartRepository).save(same(sessionCart));
            verify(cartMapper).toResponse(same(sessionCart));
        }

        @Test
        void removeCoupon_cartNotFound_throwsCartNotFoundException() {
            System.out.println("Testing removeCoupon_cartNotFound_throwsCartNotFoundException with sessionId: " + VALID_SESSION_ID);
            when(cartRepository.findBySessionIdWithItemsAndCoupons(eq(VALID_SESSION_ID))).thenReturn(Optional.empty());

            assertThrows(CartNotFoundException.class,
                    () -> buyerCartService.removeCoupon(VALID_SESSION_ID));
            verify(cartRepository, never()).save(any(Cart.class));
            verify(cartMapper, never()).toResponse(any(Cart.class));
        }

        @Test
        void removeCoupon_noCoupon_success() {
            System.out.println("Testing removeCoupon_noCoupon_success with sessionId: " + VALID_SESSION_ID + ", cart: " + sessionCart);
            sessionCart.setCoupon(null);
            when(cartRepository.findBySessionIdWithItemsAndCoupons(eq(VALID_SESSION_ID))).thenReturn(Optional.of(sessionCart));
            when(cartRepository.save(same(sessionCart))).thenReturn(sessionCart);
            when(cartMapper.toResponse(same(sessionCart))).thenReturn(sessionCartResponse);

            CartResponse response = buyerCartService.removeCoupon(VALID_SESSION_ID);

            assertNotNull(response);
            assertEquals(VALID_SESSION_ID, response.getSessionId());
            assertNull(sessionCart.getCoupon());
            verify(cartRepository).save(same(sessionCart));
            verify(cartMapper).toResponse(same(sessionCart));
        }
    }*//*

    @Nested
    class RemoveCouponForUserTests {
        @Test
        void removeCouponForUser_validRequest_success() {
            userCart.setCoupon(coupon);
            when(currentUserService.getCurrentUser()).thenReturn(user);
            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
            when(cartRepository.save(userCart)).thenReturn(userCart);
            when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);

            CartResponse response = buyerCartService.removeCouponForUser();

            assertNotNull(response);
            assertNull(response.getSessionId());
            assertNull(userCart.getCoupon());
            verify(cartRepository).save(userCart);
            verify(cartMapper).toResponse(userCart);
        }
    }

    @Nested
    class CalculateTotalAmountTests {
        @Test
        void calculateTotalAmount_validUserId_success() {
            userCart.setTotalPrice(new BigDecimal("199.98"));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));

            BigDecimal total = buyerCartService.calculateTotalAmount(1L);

            assertEquals(new BigDecimal("199.98"), total);
            verify(cartRepository).findByUserWithItems(user);
        }

        @Test
        void calculateTotalAmount_userNotFound_throwsException() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(Exception.class, () -> buyerCartService.calculateTotalAmount(1L));
            verify(cartRepository, never()).findByUserWithItems(any(User.class));
        }
    }

    @Nested
    class CalculateDiscountTests {
        @Test
        void calculateDiscount_validUserId_success() {
            userCart.setDiscount(new BigDecimal("20.00"));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));

            BigDecimal discount = buyerCartService.calculateDiscount(1L);

            assertEquals(new BigDecimal("20.00"), discount);
            verify(cartRepository).findByUserWithItems(user);
        }
    }
}*/

/*package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.BadRequestException;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

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
public class BuyerCartServiceTest {

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

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BuyerCartService buyerCartService;

    private Cart cart;
    private Cart userCart;
    private Product product;
    private User user;
    private Company company;
    private Coupon coupon;
    private CartItem cartItem;
    private CartResponse sessionCartResponse;
    private CartResponse userCartResponse;
    private static final String VALID_SESSION_ID = "123e4567-e89b-12d3-a456-426614174000";

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        user = new User();
        user.setId(1L);
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
                .build();

        cart = Cart.builder()
                .id(1L)
                .sessionId(VALID_SESSION_ID)
                .user(null)
                .items(new ArrayList<>(List.of(cartItem)))
                .totalPrice(new BigDecimal("199.98"))
                .discount(BigDecimal.ZERO)
                .lastModified(LocalDateTime.now())
                .build();
        cartItem.setCart(cart);

        userCart = Cart.builder()
                .id(2L)
                .sessionId(null)
                .user(user)
                .items(new ArrayList<>())
                .totalPrice(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .lastModified(LocalDateTime.now())
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
                .user(null)
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
                .user(user)
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
    }

    @Test
    void addToCart_validRequest_success() {
        CartRequest cartRequest = new CartRequest();
        cartRequest.setProductId(1L);
        cartRequest.setQuantity(1);

        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartMapper.toResponse(cart)).thenReturn(sessionCartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = buyerCartService.addToCart(VALID_SESSION_ID, cartRequest);

        assertNotNull(response);
        assertEquals(VALID_SESSION_ID, response.getSessionId());
        assertNull(response.getUser());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void addToCartForUser_validRequest_success() {
        CartRequest cartRequest = new CartRequest();
        cartRequest.setProductId(1L);
        cartRequest.setQuantity(1);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("buyer@test.com");
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse("buyer@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(userCart);

        CartResponse response = buyerCartService.addToCartForUser(cartRequest);

        assertNotNull(response);
        assertNull(response.getSessionId());
        assertEquals(user, response.getUser());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void addToCart_invalidSessionId_throwsBadRequestException() {
        CartRequest cartRequest = new CartRequest();
        cartRequest.setProductId(1L);
        cartRequest.setQuantity(1);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                buyerCartService.addToCart("invalid-session", cartRequest));

        assertEquals("Invalid session ID format", exception.getMessage());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void updateCartItem_validRequest_success() {
        when(cartRepository.findBySessionId(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartMapper.toResponse(cart)).thenReturn(sessionCartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = buyerCartService.updateCartItem(VALID_SESSION_ID, 1L, 3);

        assertNotNull(response);
        assertEquals(VALID_SESSION_ID, response.getSessionId());
        assertNull(response.getUser());
        assertEquals(3, cart.getItems().get(0).getQuantity());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void updateCartItemForUser_validRequest_success() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("buyer@test.com");
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse("buyer@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        userCart.getItems().add(cartItem);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(userCart);

        CartResponse response = buyerCartService.updateCartItemForUser(1L, 3);

        assertNotNull(response);
        assertNull(response.getSessionId());
        assertEquals(user, response.getUser());
        assertEquals(3, userCart.getItems().get(0).getQuantity());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void updateCartItem_invalidSessionId_throwsBadRequestException() {
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                buyerCartService.updateCartItem("invalid-session", 1L, 3));

        assertEquals("Invalid session ID format", exception.getMessage());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void removeFromCart_validRequest_success() {
        when(cartRepository.findBySessionId(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(cart)).thenReturn(sessionCartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = buyerCartService.removeFromCart(VALID_SESSION_ID, 1L);

        assertNotNull(response);
        assertEquals(VALID_SESSION_ID, response.getSessionId());
        assertNull(response.getUser());
        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void removeFromCartForUser_validRequest_success() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("buyer@test.com");
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse("buyer@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        userCart.getItems().add(cartItem);
        when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(userCart);

        CartResponse response = buyerCartService.removeFromCartForUser(1L);

        assertNotNull(response);
        assertNull(response.getSessionId());
        assertEquals(user, response.getUser());
        assertTrue(userCart.getItems().isEmpty());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void removeFromCart_invalidSessionId_throwsBadRequestException() {
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                buyerCartService.removeFromCart("invalid-session", 1L));

        assertEquals("Invalid session ID format", exception.getMessage());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void clearCart_validRequest_success() {
        when(cartRepository.findBySessionId(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(cart)).thenReturn(sessionCartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = buyerCartService.clearCart(VALID_SESSION_ID);

        assertNotNull(response);
        assertEquals(VALID_SESSION_ID, response.getSessionId());
        assertNull(response.getUser());
        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void clearCartForUser_validRequest_success() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("buyer@test.com");
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse("buyer@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        userCart.getItems().add(cartItem);
        when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(userCart);

        CartResponse response = buyerCartService.clearCartForUser();

        assertNotNull(response);
        assertNull(response.getSessionId());
        assertEquals(user, response.getUser());
        assertTrue(userCart.getItems().isEmpty());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void clearCart_invalidSessionId_throwsBadRequestException() {
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                buyerCartService.clearCart("invalid-session"));

        assertEquals("Invalid session ID format", exception.getMessage());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void getCart_validSessionId_success() {
        Pageable pageable = PageRequest.of(0, 10);
        when(cartRepository.findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(cart, pageable)).thenReturn(sessionCartResponse);

        CartResponse response = buyerCartService.getCart(VALID_SESSION_ID, pageable);

        assertNotNull(response);
        assertEquals(VALID_SESSION_ID, response.getSessionId());
        assertNull(response.getUser());
        verify(cartRepository).findBySessionIdWithItemsAndCoupons(VALID_SESSION_ID);
    }

    @Test
    void getCart_invalidSessionId_throwsBadRequestException() {
        Pageable pageable = PageRequest.of(0, 10);
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                buyerCartService.getCart("invalid-session", pageable));

        assertEquals("Invalid session ID format", exception.getMessage());
        verify(cartRepository, never()).findBySessionIdWithItemsAndCoupon(anyString());
    }

    @Test
    void getCartForUser_validRequest_success() {
        Pageable pageable = PageRequest.of(0, 10);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("buyer@test.com");
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse("buyer@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(cartMapper.toResponse(userCart, pageable)).thenReturn(userCartResponse);

        CartResponse response = buyerCartService.getCartForUser(pageable);

        assertNotNull(response);
        assertNull(response.getSessionId());
        assertEquals(user, response.getUser());
        verify(cartRepository).findByUserWithItems(user);
    }

    @Test
    void getCartByUsersId_validUserId_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);

        CartResponse response = buyerCartService.getCartByUsersId(1L);

        assertNotNull(response);
        assertNull(response.getSessionId());
        assertEquals(user, response.getUser());
        verify(cartRepository).findByUserWithItems(user);
    }

    @Test
    void mergeCartOnLogin_validSessionId_success() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("buyer@test.com");
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse("buyer@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(userCart);

        CartResponse response = buyerCartService.mergeCartOnLogin(VALID_SESSION_ID);

        assertNotNull(response);
        assertNull(response.getSessionId());
        assertEquals(user, response.getUser());
        verify(cartRepository).delete(cart);
        verify(cartRepository).save(userCart);
    }

    @Test
    void mergeCartOnLogin_noAnonymousCart_success() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("buyer@test.com");
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse("buyer@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.empty());
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(userCart);

        CartResponse response = buyerCartService.mergeCartOnLogin(VALID_SESSION_ID);

        assertNotNull(response);
        assertNull(response.getSessionId());
        assertEquals(user, response.getUser());
        verify(cartRepository, never()).delete(any(Cart.class));
        verify(cartRepository).save(userCart);
    }

    @Test
    void mergeCartOnLogin_invalidSessionId_throwsBadRequestException() {
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                buyerCartService.mergeCartOnLogin("invalid-session"));

        assertEquals("Invalid session ID format", exception.getMessage());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void applyCoupon_validCoupon_success() {
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(couponService.validateCoupon("SAVE10", null, new BigDecimal("199.98"), cart.getItems())).thenReturn(coupon);
        when(couponService.calculateDiscount(coupon, cart.getItems(), new BigDecimal("199.98"))).thenReturn(new BigDecimal("20.00"));
        when(cartMapper.toResponse(cart)).thenReturn(sessionCartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        AppliedCouponResponse response = buyerCartService.applyCoupon(VALID_SESSION_ID, "SAVE10");

        assertNotNull(response);
        assertEquals(new BigDecimal("20.00"), response.getDiscount());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void applyCoupon_invalidSessionId_throwsBadRequestException() {
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                buyerCartService.applyCoupon("invalid-session", "SAVE10"));

        assertEquals("Invalid session ID format", exception.getMessage());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void applyCouponForUser_validCoupon_success() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("buyer@test.com");
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse("buyer@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        userCart.getItems().add(cartItem);
        when(couponService.validateCoupon("SAVE10", user, new BigDecimal("199.98"), userCart.getItems())).thenReturn(coupon);
        when(couponService.calculateDiscount(coupon, userCart.getItems(), new BigDecimal("199.98"))).thenReturn(new BigDecimal("20.00"));
        when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(userCart);

        AppliedCouponResponse response = buyerCartService.applyCouponForUser("SAVE10");

        assertNotNull(response);
        assertEquals(new BigDecimal("20.00"), response.getDiscount());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void removeCoupon_validSessionId_success() {
        cart.setCoupon(coupon);
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(cart)).thenReturn(sessionCartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = buyerCartService.removeCoupon(VALID_SESSION_ID);

        assertNotNull(response);
        assertEquals(VALID_SESSION_ID, response.getSessionId());
        assertNull(response.getUser());
        assertNull(cart.getCoupon());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void removeCoupon_invalidSessionId_throwsBadRequestException() {
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                buyerCartService.removeCoupon("invalid-session"));

        assertEquals("Invalid session ID format", exception.getMessage());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void removeCouponForUser_validRequest_success() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("buyer@test.com");
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse("buyer@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        userCart.setCoupon(coupon);
        when(cartMapper.toResponse(userCart)).thenReturn(userCartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(userCart);

        CartResponse response = buyerCartService.removeCouponForUser();

        assertNotNull(response);
        assertNull(response.getSessionId());
        assertEquals(user, response.getUser());
        assertNull(userCart.getCoupon());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void calculateTotalAmount_validUserId_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        userCart.setTotalPrice(new BigDecimal("199.98"));

        BigDecimal total = buyerCartService.calculateTotalAmount(1L);

        assertEquals(new BigDecimal("199.98"), total);
        verify(cartRepository).findByUserWithItems(user);
    }

    @Test
    void calculateDiscount_validUserId_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        userCart.setDiscount(new BigDecimal("20.00"));

        BigDecimal discount = buyerCartService.calculateDiscount(1L);

        assertEquals(new BigDecimal("20.00"), discount);
        verify(cartRepository).findByUserWithItems(user);
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

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
public class BuyerCartServiceTest {

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

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BuyerCartService buyerCartService;

    private Cart cart;
    private Cart userCart;
    private Product product;
    private User user;
    private Company company;
    private Coupon coupon;
    private CartItem cartItem;
    private CartResponse cartResponse;
    private static final String VALID_SESSION_ID = "123e4567-e89b-12d3-a456-426614174000";

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        user = new User();
        user.setId(1L);
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
                .build();

        cart = Cart.builder()
                .id(1L)
                .sessionId(VALID_SESSION_ID)
                .user(null)
                .items(new ArrayList<>(List.of(cartItem)))
                .totalPrice(new BigDecimal("199.98"))
                .discount(BigDecimal.ZERO)
                .lastModified(LocalDateTime.now())
                .build();
        cartItem.setCart(cart);

        userCart = Cart.builder()
                .id(2L)
                .sessionId(null)
                .user(user)
                .items(new ArrayList<>())
                .totalPrice(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .lastModified(LocalDateTime.now())
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

        cartResponse = CartResponse.builder()
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

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("buyer@test.com");
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void addToCart_validRequest_success() {
        CartRequest cartRequest = new CartRequest();
        cartRequest.setProductId(1L);
        cartRequest.setQuantity(1);

        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = buyerCartService.addToCart(VALID_SESSION_ID, cartRequest);

        assertNotNull(response);
        assertEquals(VALID_SESSION_ID, response.getSessionId());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void addToCartForUser_validRequest_success() {
        CartRequest cartRequest = new CartRequest();
        cartRequest.setProductId(1L);
        cartRequest.setQuantity(1);

        when(userRepository.findByEmailAddressAndDeletedFalse("buyer@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartMapper.toResponse(userCart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(userCart);

        CartResponse response = buyerCartService.addToCartForUser(cartRequest);

        assertNotNull(response);
        assertNull(response.getSessionId());
        assertEquals(user.getId(), response.getUserId());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void addToCart_invalidSessionId_throwsBadRequestException() {
        CartRequest cartRequest = new CartRequest();
        cartRequest.setProductId(1L);
        cartRequest.setQuantity(1);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                buyerCartService.addToCart("invalid-session", cartRequest));

        assertEquals("Invalid session ID format", exception.getMessage());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void updateCartItem_validRequest_success() {
        when(cartRepository.findBySessionId(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = buyerCartService.updateCartItem(VALID_SESSION_ID, 1L, 3);

        assertNotNull(response);
        assertEquals(VALID_SESSION_ID, response.getSessionId());
        assertEquals(3, cart.getItems().get(0).getQuantity());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void updateCartItemForUser_validRequest_success() {
        when(userRepository.findByEmailAddressAndDeletedFalse("buyer@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        userCart.getItems().add(cartItem);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartMapper.toResponse(userCart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(userCart);

        CartResponse response = buyerCartService.updateCartItemForUser(1L, 3);

        assertNotNull(response);
        assertNull(response.getSessionId());
        assertEquals(user.getId(), response.getUserId());
        assertEquals(3, userCart.getItems().get(0).getQuantity());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void updateCartItem_invalidSessionId_throwsBadRequestException() {
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                buyerCartService.updateCartItem("invalid-session", 1L, 3));

        assertEquals("Invalid session ID format", exception.getMessage());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void removeFromCart_validRequest_success() {
        when(cartRepository.findBySessionId(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = buyerCartService.removeFromCart(VALID_SESSION_ID, 1L);

        assertNotNull(response);
        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void removeFromCartForUser_validRequest_success() {
        when(userRepository.findByEmailAddressAndDeletedFalse("buyer@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        userCart.getItems().add(cartItem);
        when(cartMapper.toResponse(userCart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(userCart);

        CartResponse response = buyerCartService.removeFromCartForUser(1L);

        assertNotNull(response);
        assertTrue(userCart.getItems().isEmpty());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void removeFromCart_invalidSessionId_throwsBadRequestException() {
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                buyerCartService.removeFromCart("invalid-session", 1L));

        assertEquals("Invalid session ID format", exception.getMessage());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void clearCart_validRequest_success() {
        when(cartRepository.findBySessionId(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = buyerCartService.clearCart(VALID_SESSION_ID);

        assertNotNull(response);
        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void clearCartForUser_validRequest_success() {
        when(userRepository.findByEmailAddressAndDeletedFalse("buyer@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        userCart.getItems().add(cartItem);
        when(cartMapper.toResponse(userCart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(userCart);

        CartResponse response = buyerCartService.clearCartForUser();

        assertNotNull(response);
        assertTrue(userCart.getItems().isEmpty());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void clearCart_invalidSessionId_throwsBadRequestException() {
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                buyerCartService.clearCart("invalid-session"));

        assertEquals("Invalid session ID format", exception.getMessage());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void getCart_validSessionId_success() {
        Pageable pageable = PageRequest.of(0, 10);
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(cart, pageable)).thenReturn(cartResponse);

        CartResponse response = buyerCartService.getCart(VALID_SESSION_ID, pageable);

        assertNotNull(response);
        assertEquals(VALID_SESSION_ID, response.getSessionId());
        verify(cartRepository).findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID);
    }

    @Test
    void getCart_invalidSessionId_throwsBadRequestException() {
        Pageable pageable = PageRequest.of(0, 10);
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                buyerCartService.getCart("invalid-session", pageable));

        assertEquals("Invalid session ID format", exception.getMessage());
        verify(cartRepository, never()).findBySessionIdWithItemsAndCoupon(anyString());
    }

    @Test
    void getCartForUser_validRequest_success() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findByEmailAddressAndDeletedFalse("buyer@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(cartMapper.toResponse(userCart, pageable)).thenReturn(cartResponse);

        CartResponse response = buyerCartService.getCartForUser(pageable);

        assertNotNull(response);
        assertNull(response.getSessionId());
        assertEquals(user.getId(), response.getUserId());
        verify(cartRepository).findByUserWithItems(user);
    }

    @Test
    void getCartByUsersId_validUserId_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(cartMapper.toResponse(userCart)).thenReturn(cartResponse);

        CartResponse response = buyerCartService.getCartByUsersId(1L);

        assertNotNull(response);
        assertNull(response.getSessionId());
        assertEquals(user.getId(), response.getUserId());
        verify(cartRepository).findByUserWithItems(user);
    }

    @Test
    void mergeCartOnLogin_validSessionId_success() {
        when(userRepository.findByEmailAddressAndDeletedFalse("buyer@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartMapper.toResponse(userCart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(userCart);

        CartResponse response = buyerCartService.mergeCartOnLogin(VALID_SESSION_ID);

        assertNotNull(response);
        assertNull(response.getSessionId());
        assertEquals(user.getId(), response.getUserId());
        verify(cartRepository).delete(cart);
        verify(cartRepository).save(userCart);
    }

    @Test
    void mergeCartOnLogin_noAnonymousCart_success() {
        when(userRepository.findByEmailAddressAndDeletedFalse("buyer@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.empty());
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(cartMapper.toResponse(userCart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(userCart);

        CartResponse response = buyerCartService.mergeCartOnLogin(VALID_SESSION_ID);

        assertNotNull(response);
        assertNull(response.getSessionId());
        assertEquals(user.getId(), response.getUserId());
        verify(cartRepository, never()).delete(any(Cart.class));
        verify(cartRepository).save(userCart);
    }

    @Test
    void mergeCartOnLogin_invalidSessionId_throwsBadRequestException() {
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                buyerCartService.mergeCartOnLogin("invalid-session"));

        assertEquals("Invalid session ID format", exception.getMessage());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void applyCoupon_validCoupon_success() {
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(couponService.validateCoupon("SAVE10", null, new BigDecimal("199.98"), cart.getItems())).thenReturn(coupon);
        when(couponService.calculateDiscount(coupon, cart.getItems(), new BigDecimal("199.98"))).thenReturn(new BigDecimal("20.00"));
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        AppliedCouponResponse response = buyerCartService.applyCoupon(VALID_SESSION_ID, "SAVE10");

        assertNotNull(response);
        assertEquals(new BigDecimal("20.00"), response.getDiscount());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void applyCoupon_invalidSessionId_throwsBadRequestException() {
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                buyerCartService.applyCoupon("invalid-session", "SAVE10"));

        assertEquals("Invalid session ID format", exception.getMessage());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void applyCouponForUser_validCoupon_success() {
        when(userRepository.findByEmailAddressAndDeletedFalse("buyer@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        userCart.getItems().add(cartItem);
        when(couponService.validateCoupon("SAVE10", user, new BigDecimal("199.98"), userCart.getItems())).thenReturn(coupon);
        when(couponService.calculateDiscount(coupon, userCart.getItems(), new BigDecimal("199.98"))).thenReturn(new BigDecimal("20.00"));
        when(cartMapper.toResponse(userCart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(userCart);

        AppliedCouponResponse response = buyerCartService.applyCouponForUser("SAVE10");

        assertNotNull(response);
        assertEquals(new BigDecimal("20.00"), response.getDiscount());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void removeCoupon_validSessionId_success() {
        cart.setCoupon(coupon);
        when(cartRepository.findBySessionIdWithItemsAndCoupon(VALID_SESSION_ID)).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = buyerCartService.removeCoupon(VALID_SESSION_ID);

        assertNotNull(response);
        assertNull(cart.getCoupon());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void removeCoupon_invalidSessionId_throwsBadRequestException() {
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                buyerCartService.removeCoupon("invalid-session"));

        assertEquals("Invalid session ID format", exception.getMessage());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void removeCouponForUser_validRequest_success() {
        userCart.setCoupon(coupon);
        when(userRepository.findByEmailAddressAndDeletedFalse("buyer@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        when(cartMapper.toResponse(userCart)).thenReturn(cartResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(userCart);

        CartResponse response = buyerCartService.removeCouponForUser();

        assertNotNull(response);
        assertNull(userCart.getCoupon());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void calculateTotalAmount_validUserId_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        userCart.setTotalPrice(new BigDecimal("199.98"));

        BigDecimal total = buyerCartService.calculateTotalAmount(1L);

        assertEquals(new BigDecimal("199.98"), total);
        verify(cartRepository).findByUserWithItems(user);
    }

    @Test
    void calculateDiscount_validUserId_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.of(userCart));
        userCart.setDiscount(new BigDecimal("20.00"));

        BigDecimal discount = buyerCartService.calculateDiscount(1L);

        assertEquals(new BigDecimal("20.00"), discount);
        verify(cartRepository).findByUserWithItems(user);
    }
}*/


