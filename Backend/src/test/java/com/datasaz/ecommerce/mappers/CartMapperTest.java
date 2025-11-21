package com.datasaz.ecommerce.mappers;

import com.datasaz.ecommerce.models.response.CartItemResponse;
import com.datasaz.ecommerce.models.response.CartResponse;
import com.datasaz.ecommerce.models.response.CouponResponse;
import com.datasaz.ecommerce.repositories.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class CartMapperTest {

    private CartMapper cartMapper;
    private Product product1, product2;
    private CartItem item1, item2;
    private Cart cart;
    private Coupon coupon;

    @BeforeEach
    void setUp() {
        cartMapper = new CartMapper();

        // Product 1: Regular price
        product1 = Product.builder()
                .id(1L)
                .price(new BigDecimal("100.00"))
                .offerPrice(null)
                .shippingCost(new BigDecimal("5.00"))
                .eachAdditionalItemShippingCost(new BigDecimal("2.00"))
                .quantity(100)
                .build();

        // Product 2: Offer price
        product2 = Product.builder()
                .id(2L)
                .price(new BigDecimal("200.00"))
                .offerPrice(new BigDecimal("150.00"))
                .shippingCost(new BigDecimal("10.00"))
                .eachAdditionalItemShippingCost(null)
                .quantity(50)
                .build();

        // Cart Items
        item1 = CartItem.builder()
                .id(10L)
                .product(product1)
                .quantity(2)
                .build();

        item2 = CartItem.builder()
                .id(20L)
                .product(product2)
                .quantity(3)
                .build();

        // Coupon
        coupon = Coupon.builder()
                .id(1L)
                .code("SAVE10")
                .description("10% off")
                .discountPercentage(new BigDecimal("10.00"))
                .build();

        // User
        User user = User.builder().id(100L).build();

        // Cart
        cart = Cart.builder()
                .id(1L)
                .sessionId("550e8400-e29b-41d4-a716-446655440000")
                .user(user)
                .items(Arrays.asList(item1, item2))
                .coupon(coupon)
                .totalDiscount(new BigDecimal("65.00")) // Will be overridden in calc
                .subtotalPrice(new BigDecimal("999.99")) // Stale – should be ignored
                .build();
    }

    @Test
    void toResponse_NullCart_ReturnsEmptyResponse() {
        CartResponse response = cartMapper.toResponse(null);
        assertThat(response)
                .hasFieldOrPropertyWithValue("id", null)
                .hasFieldOrPropertyWithValue("subtotalPrice", BigDecimal.ZERO)
                .hasFieldOrPropertyWithValue("items", Collections.emptyList());
    }

    @Test
    void toResponse_WithValidCart_MapsAllFields() {
        CartResponse response = cartMapper.toResponse(cart);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getSessionId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        assertThat(response.getUserId()).isEqualTo(100L);
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getCouponResponse().getCode()).isEqualTo("SAVE10");
    }

    @Test
    void toResponse_CalculatesSubtotalCorrectly() {
        // item1: 2 * 100 = 200
        // item2: 3 * 150 = 450
        // Total: 650
        CartResponse response = cartMapper.toResponse(cart);
        assertThat(response.getSubtotalPrice()).isEqualTo(new BigDecimal("650.00"));
    }

    @Test
    void toResponse_CalculatesShippingCorrectly() {
        // item1: base 5 + additional 2 = 7
        // item2: base 10 + 2 * 10 (fallback) = 30
        // Total: 37
        CartResponse response = cartMapper.toResponse(cart);
        assertThat(response.getTotalShippingCost()).isEqualTo(new BigDecimal("37.00"));
    }

    @ParameterizedTest
    @CsvSource({
            "0, 1, 1",  // page 0, size 1 → 1 item
            "1, 1, 1",  // page 1, size 1 → 1 item
            "0, 10, 2", // full
            "5, 10, 0"  // out of range
    })
    void toResponse_WithPagination_AppliesCorrectly(int page, int size, int expectedItems) {
        Pageable pageable = PageRequest.of(page, size);
        CartResponse response = cartMapper.toResponse(cart, pageable);
        assertThat(response.getItems()).hasSize(expectedItems);
    }

    @Test
    void toItemResponse_MapsFields() {
        CartItemResponse response = cartMapper.toItemResponse(item1);
        assertThat(response)
                .hasFieldOrPropertyWithValue("id", 10L)
                .hasFieldOrPropertyWithValue("productId", 1L)
                .hasFieldOrPropertyWithValue("quantity", 2);
    }

    @Test
    void toItemResponse_NullItem_ReturnsNull() {
        assertThat(cartMapper.toItemResponse(null)).isNull();
    }

    @Test
    void toCouponResponse_MapsAllFields() {
        CouponResponse response = cartMapper.toCouponResponse(coupon);
        assertThat(response)
                .hasFieldOrPropertyWithValue("code", "SAVE10")
                .hasFieldOrPropertyWithValue("discountPercentage", new BigDecimal("10.00"));
    }

    @Test
    void toCouponResponse_NullCoupon_ReturnsNull() {
        assertThat(cartMapper.toCouponResponse(null)).isNull();
    }

    @Test
    void subtotalPriceOf_HandlesNulls() {
        Cart emptyCart = Cart.builder().items(null).build();
        assertThat(cartMapper.subtotalPriceOf(emptyCart)).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void totalShippingCost_Of_HandlesNulls() {
        Cart emptyCart = Cart.builder().items(null).build();
        assertThat(cartMapper.totalShippingCostOf(emptyCart)).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void toResponse_UsesCalculatedTotals_NotStaleDBFields() {
        // DB has subtotalPrice = 999.99, but calculation = 650
        CartResponse response = cartMapper.toResponse(cart);
        assertThat(response.getSubtotalPrice()).isEqualTo(new BigDecimal("650.00"));
        assertThat(response.getTotalAmount()).isEqualTo(new BigDecimal("622.00")); // 650 + 37 - 65
    }
}

/*import com.datasaz.ecommerce.models.response.CartItemResponse;
import com.datasaz.ecommerce.models.response.CartResponse;
import com.datasaz.ecommerce.models.response.CouponResponse;
import com.datasaz.ecommerce.repositories.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
class CartMapperTest {

    private CartMapper cartMapper;

    private Cart cart;
    private CartItem cartItem;
    private Coupon coupon;
    private User user;

    @BeforeEach
    void setUp() {
        cartMapper = new CartMapper();

        // Initialize test data
        user = User.builder()
                .id(1L)
                .build();

        coupon = Coupon.builder()
                .id(1L)
                .code("SAVE10")
                .description("10% off")
                .state(CouponState.ACTIVE)
                .category(CouponCategory.GENERAL)
                .couponScope(CouponScope.ITEM)
                .couponType(CouponType.PERCENTAGE)
                .minimumOrderAmount(BigDecimal.valueOf(50))
                .maxUses(100)
                .maxUsesPerUser(1)
                .author(user)
                .startFrom(LocalDateTime.of(2025, 1, 1, 0, 0))
                .endAt(LocalDateTime.of(2025, 12, 31, 23, 59))
                .discountPercentage(BigDecimal.TEN)
                .discountFixedAmount(null)
                .couponTrackings(new HashSet<>())
                .version(0L)
                .build();

        cartItem = CartItem.builder()
                .id(1L)
                .product(Product.builder().id(1L).build())
                .quantity(2)
                .build();

        cart = Cart.builder()
                .id(1L)
                .sessionId("test-session")
                .user(user)
                .items(Arrays.asList(cartItem))
                .coupon(coupon)
                .subtotalPrice(BigDecimal.valueOf(36))
                .totalDiscount(BigDecimal.valueOf(4))
                .version(0L)
                .lastModified(LocalDateTime.now())
                .build();
    }

    // Test toResponse without Pageable
    @Test
    void toResponse_validCart_returnsCartResponse() {
        CartResponse response = cartMapper.toResponse(cart);

        assertNotNull(response);
        assertEquals(cart.getId(), response.getId());
        assertEquals(cart.getSessionId(), response.getSessionId());
        assertEquals(cart.getUser().getId(), response.getUserId());
        assertEquals(1, response.getItems().size());
        assertNotNull(response.getCouponResponse());
        assertEquals(coupon.getCode(), response.getCouponResponse().getCode());
        assertEquals(BigDecimal.valueOf(40), response.getSubtotalPrice()); // 20 * 2
        assertEquals(cart.getTotalDiscount(), response.getTotalDiscount());
        assertEquals(cart.getSubtotalPrice(), response.getTotalAmount());
    }

    // Test toResponse with Pageable
    @Test
    void toResponse_withPageable_returnsPaginatedItems() {
        Pageable pageable = PageRequest.of(0, 1);
        CartResponse response = cartMapper.toResponse(cart, pageable);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertEquals(cartItem.getId(), response.getItems().get(0).getId());
    }

    // Test toResponse with Pageable out of bounds
    @Test
    void toResponse_withOutOfBoundsPageable_returnsEmptyItems() {
        Pageable pageable = PageRequest.of(1, 1); // Page 1, size 1 (out of bounds)
        CartResponse response = cartMapper.toResponse(cart, pageable);

        assertNotNull(response);
        assertTrue(response.getItems().isEmpty());
    }

    // Test toResponse with null cart
    @Test
    void toResponse_nullCart_returnsEmptyResponse() {
        CartResponse response = cartMapper.toResponse(null);

        assertNotNull(response);
        assertNull(response.getId());
        assertNull(response.getSessionId());
        assertNull(response.getUserId());
        assertTrue(response.getItems().isEmpty());
        assertNull(response.getCouponResponse());
        assertEquals(BigDecimal.ZERO, response.getSubtotalPrice());
        assertEquals(BigDecimal.ZERO, response.getTotalDiscount());
        assertEquals(BigDecimal.ZERO, response.getTotalAmount());
    }

    // Test toItemResponse
    @Test
    void toItemResponse_validCartItem_returnsCartItemResponse() {
        CartItemResponse response = cartMapper.toItemResponse(cartItem);

        assertNotNull(response);
        assertEquals(cartItem.getId(), response.getId());
        assertEquals(cartItem.getProduct().getId(), response.getProductId());
        assertEquals(cartItem.getQuantity(), response.getQuantity());
    }

    // Test toItemResponse with null cart item
    @Test
    void toItemResponse_nullCartItem_returnsNull() {
        CartItemResponse response = cartMapper.toItemResponse(null);
        assertNull(response);
    }

    // Test toCouponResponse
    @Test
    void toCouponResponse_validCoupon_returnsCouponResponse() {
        CouponResponse response = cartMapper.toCouponResponse(coupon);

        assertNotNull(response);
        assertEquals(coupon.getId(), response.getId());
        assertEquals(coupon.getCode(), response.getCode());
        assertEquals(coupon.getDescription(), response.getDescription());
        assertEquals(coupon.getState(), response.getState());
        assertEquals(coupon.getCategory(), response.getCategory());
        assertEquals(coupon.getCouponScope(), response.getCouponScope());
        assertEquals(coupon.getCouponType(), response.getCouponType());
        assertEquals(coupon.getMinimumOrderAmount(), response.getMinimumOrderAmount());
        assertEquals(coupon.getMaxUses(), response.getMaxUses());
        assertEquals(coupon.getMaxUsesPerUser(), response.getMaxUsesPerUser());
        assertEquals(coupon.getAuthor().getId(), response.getAuthorId());
        assertEquals(coupon.getStartFrom(), response.getStartFrom());
        assertEquals(coupon.getEndAt(), response.getEndAt());
        assertEquals(coupon.getDiscountPercentage(), response.getDiscountPercentage());
        assertEquals(coupon.getDiscountFixedAmount(), response.getDiscountFixedAmount());
        assertEquals(coupon.getCouponTrackings(), response.getCouponTrackings());
    }

    // Test toCouponResponse with null coupon
    @Test
    void toCouponResponse_nullCoupon_returnsNull() {
        CouponResponse response = cartMapper.toCouponResponse(null);
        assertNull(response);
    }

    // Test toCouponResponse with null fields
    @Test
    void toCouponResponse_couponWithNullFields_returnsCouponResponseWithNulls() {
        Coupon minimalCoupon = Coupon.builder()
                .id(2L)
                .code("SAVE20")
                .state(CouponState.ACTIVE)
                .category(CouponCategory.GENERAL)
                .couponType(CouponType.FIXED)
                .startFrom(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusDays(30))
                .discountFixedAmount(BigDecimal.valueOf(20))
                .build();

        CouponResponse response = cartMapper.toCouponResponse(minimalCoupon);

        assertNotNull(response);
        assertEquals(minimalCoupon.getId(), response.getId());
        assertEquals(minimalCoupon.getCode(), response.getCode());
        assertNull(response.getDescription());
        assertEquals(minimalCoupon.getState(), response.getState());
        assertEquals(minimalCoupon.getCategory(), response.getCategory());
        assertNull(response.getCouponScope());
        assertEquals(minimalCoupon.getCouponType(), response.getCouponType());
        assertNull(response.getMinimumOrderAmount());
        assertEquals(0, response.getMaxUses());
        assertEquals(0, response.getMaxUsesPerUser());
        assertNull(response.getAuthorId());
        assertEquals(minimalCoupon.getStartFrom(), response.getStartFrom());
        assertEquals(minimalCoupon.getEndAt(), response.getEndAt());
        assertNull(response.getDiscountPercentage());
        assertEquals(minimalCoupon.getDiscountFixedAmount(), response.getDiscountFixedAmount());
        assertEquals(Collections.emptySet(), response.getCouponTrackings());
    }

    // Test calculateSubtotal
    @Test
    void calculateSubtotal_validCart_returnsCorrectSubtotalPrice() {
        BigDecimal subtotal = cartMapper.calculateSubtotalPrice(cart);
        assertEquals(BigDecimal.valueOf(40), subtotal); // 20 * 2
    }

    // Test calculateSubtotal with null cart
    @Test
    void calculateSubtotal_Price_nullCart_returnsZero() {
        BigDecimal subtotal = cartMapper.calculateSubtotalPrice(null);
        assertEquals(BigDecimal.ZERO, subtotal);
    }

    // Test calculateSubtotal with empty items
    @Test
    void calculateSubtotal_Price_emptyItems_returnsZero() {
        cart.setItems(Collections.emptyList());
        BigDecimal subtotal = cartMapper.calculateSubtotalPrice(cart);
        assertEquals(BigDecimal.ZERO, subtotal);
    }

    @Test
    void toResponse_withItemsAndPageable_returnsPaginatedResponse() {
        Cart cart = Cart.builder()
                .id(1L)
                .sessionId("eafa58da-f94b-4e68-90c5-169a7cd1b1c1")
                .user(User.builder().id(1L).build())
                .subtotalPrice(new BigDecimal("90.00"))
                .totalShippingCost(new BigDecimal("10.00"))
                .items(Arrays.asList(
                        CartItem.builder()
                                .id(1L)
                                .product(Product.builder().id(5L).build())
                                .quantity(1)
                                .build(),
                        CartItem.builder()
                                .id(2L)
                                .product(Product.builder().id(6L).build())
                                .quantity(1)
                                .build()
                ))
                .build();

        Pageable pageable = PageRequest.of(0, 1);
        CartResponse response = cartMapper.toResponse(cart, pageable);

        assertEquals(1L, response.getId());
        assertEquals("eafa58da-f94b-4e68-90c5-169a7cd1b1c1", response.getSessionId());
        assertEquals(1L, response.getUserId());
        assertEquals(1, response.getItems().size());
        assertEquals(5L, response.getItems().get(0).getProductId());
        assertEquals(new BigDecimal("100.00"), response.getSubtotalPrice());
        assertEquals(new BigDecimal("10.00"), response.getTotalDiscount());
        assertEquals(new BigDecimal("90.00"), response.getTotalAmount());
    }

    // Test calculateSubtotal with null price in cart item
    @Test
    void calculateSubtotal_Price_nullPriceInItem_skipsItem() {
        cart.setItems(Arrays.asList(cartItem));
        BigDecimal subtotal = cartMapper.calculateSubtotalPrice(cart);
        assertEquals(BigDecimal.ZERO, subtotal);
    }
}*/


/*    @Test
    void toResponse_mapsCartToCartResponse() {
        // Arrange
        Users user = new Users();
        user.setId(1L);

        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setPrice(new BigDecimal("99.99"));

        Coupon coupon = new Coupon();
        coupon.setIdentifier("SAVE10");

        CartItem cartItem = new CartItem();
        cartItem.setId(1L);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);
        cartItem.setProductName(product.getName());
        cartItem.setPrice(product.getPrice());

        Cart cart = new Cart();
        cart.setId(1L);
        cart.setSessionId("test-session");
        cart.setUsers(user);
        cart.setCoupon(coupon);
        cart.setItems(List.of(cartItem));
        cart.setDiscount(new BigDecimal("10.00")); // TODO: calculate discount based on coupon
        cart.setTotalPrice(new BigDecimal("199.98"));// TODO: calculate totalPrice based on item price * quantity - discount


        // Act
        CartResponse response = cartMapper.toResponse(cart);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("test-session", response.getSessionId());
        assertEquals(1L, response.getUserId());
        assertEquals("SAVE10", response.getCouponIdentifier());
        assertEquals(1, response.getItems().size());

        CartItemResponse itemResponse = response.getItems().get(0);
        assertEquals(1L, itemResponse.getId());
        assertEquals(1L, itemResponse.getProductId());
        assertEquals("Test Product", itemResponse.getProductName());
        assertEquals(2, itemResponse.getQuantity());
        assertEquals(new BigDecimal("99.99"), itemResponse.getPrice());
    }

    @Test
    void toResponse_nullUser_mapsNullUserId() {
        // Arrange
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setPrice(new BigDecimal("99.99"));

        CartItem cartItem = new CartItem();
        cartItem.setId(1L);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);

        Cart cart = new Cart();
        cart.setId(1L);
        cart.setSessionId("test-session");
        cart.setUsers(null); // Anonymous cart
        cart.setItems(List.of(cartItem));

        // Act
        CartResponse response = cartMapper.toResponse(cart);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("test-session", response.getSessionId());
        assertNull(response.getUserId());
        assertEquals(1, response.getItems().size());
    }
*/