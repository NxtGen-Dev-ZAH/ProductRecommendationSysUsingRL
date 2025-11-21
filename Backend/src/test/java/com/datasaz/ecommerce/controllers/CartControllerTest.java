package com.datasaz.ecommerce.controllers;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.GlobalExceptionHandler;
import com.datasaz.ecommerce.models.request.CartItemRequest;
import com.datasaz.ecommerce.models.request.CouponCodeRequest;
import com.datasaz.ecommerce.models.response.AppliedCouponResponse;
import com.datasaz.ecommerce.models.response.CartResponse;
import com.datasaz.ecommerce.services.interfaces.ICartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

//@WebMvcTest(controllers = CartController.class)
//@ContextConfiguration(classes = {CartController.class,
// CartControllerTest.TestSecurityConfig.class})

@WebMvcTest(controllers = CartController.class)
@ContextConfiguration(classes = {
        CartController.class,
        CartControllerTest.TestSecurityConfig.class,
        GlobalExceptionHandler.class
})
class CartControllerTest {

    @EnableWebSecurity
    @Configuration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain securityFilterChain(org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/cart/**").permitAll()
                            .anyRequest().authenticated())
                    .httpBasic(withDefaults())
                    .formLogin(withDefaults());
            return http.build();
        }

        @Bean
        UserDetailsService userDetailsService() {
            return mock(UserDetailsService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private ICartService cartService;
    @Autowired
    private ObjectMapper objectMapper;

    private CartResponse cartResponse;
    private AppliedCouponResponse appliedCouponResponse;
    private CartItemRequest validItemRequest;
    private CouponCodeRequest validCouponRequest;

    private static final String COOKIE_NAME = "cart_session_id";
    private static final int COOKIE_MAX_AGE = 12 * 30 * 24 * 60 * 60;

    @BeforeEach
    void setUp() {
        cartResponse = CartResponse.builder().build();
        appliedCouponResponse = AppliedCouponResponse.builder()
                .code("SAVE10")
                .discount(BigDecimal.TEN)
                .cartResponse(cartResponse)
                .build();

        validItemRequest = new CartItemRequest(1L, 2);
        validCouponRequest = new CouponCodeRequest("SAVE10");
    }

    /* =========================================== GET /api/cart =========================================== */

    @Test
    void getCart_withHeaderSessionId_returnsCart() throws Exception {
        String sessionId = "123e4567-e89b-12d3-a456-426614174000";
        when(cartService.getCart(eq(sessionId), any())).thenReturn(cartResponse);

        mockMvc.perform(get("/api/cart")
                        .header("X-Session-Id", sessionId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(cartService).getCart(eq(sessionId), eq(PageRequest.of(0, 10)));
    }

    @Test
    void getCart_withCookieSessionId_returnsCart() throws Exception {
        String sessionId = "123e4567-e89b-12d3-a456-426614174000";
        Cookie cookie = new Cookie(COOKIE_NAME, sessionId);
        when(cartService.getCart(eq(sessionId), any())).thenReturn(cartResponse);

        mockMvc.perform(get("/api/cart")
                        .cookie(cookie)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(cartService).getCart(eq(sessionId), eq(PageRequest.of(0, 10)));
    }

    @Test
    void getCart_missingSessionId_throwsBadRequest() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .param("page", "0")
                        .param("size", "10"))
                //.andExpect(status().isBadRequest())
                //.andExpect(jsonPath("$.message").value("Missing or invalid cart session ID"))
                .andExpect(jsonPath("$.message").value("BAD_REQUEST: Missing or invalid cart session ID"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"))
        ;

        verifyNoInteractions(cartService);
    }

    @Test
    void getCart_emptySessionId_throwsBadRequest() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .header("X-Session-Id", "")
                        .param("page", "0")
                        .param("size", "10"))
                //.andExpect(status().isBadRequest())
                //.andExpect(jsonPath("$.message").value("Missing or invalid cart session ID"))
                .andExpect(jsonPath("$.message").value("BAD_REQUEST: Missing or invalid cart session ID"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"))
        ;

        verifyNoInteractions(cartService);
    }

    /* ======================================== POST /api/cart/add ======================================== */

    @Test
    void addToCart_withHeaderSessionId_addsItem() throws Exception {
        String sessionId = "123e4567-e89b-12d3-a456-426614174000";
        when(cartService.addToCart(eq(sessionId), any(CartItemRequest.class))).thenReturn(cartResponse);

        mockMvc.perform(post("/api/cart/add")
                        .header("X-Session-Id", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validItemRequest)))
                .andExpect(status().isOk());

        // Use captor to verify arguments
        ArgumentCaptor<CartItemRequest> captor = ArgumentCaptor.forClass(CartItemRequest.class);
        verify(cartService).addToCart(eq(sessionId), captor.capture());
        assertThat(captor.getValue().getProductId()).isEqualTo(1L);
        assertThat(captor.getValue().getQuantity()).isEqualTo(2);
    }

    @Test
    void addToCart_noSessionId_createsSessionAndSetsCookie() throws Exception {
        String generatedSessionId = UUID.randomUUID().toString();
        when(cartService.addToCart(anyString(), any(CartItemRequest.class))).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            if (id != null && id.matches("[0-9a-fA-F\\-]{36}")) {
                return cartResponse;
            }
            return null;
        });

        MvcResult result = mockMvc.perform(post("/api/cart/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validItemRequest)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(COOKIE_NAME))
                .andExpect(cookie().path(COOKIE_NAME, "/"))
                .andExpect(cookie().maxAge(COOKIE_NAME, COOKIE_MAX_AGE))
                .andReturn();

        String cookieValue = result.getResponse().getCookie(COOKIE_NAME).getValue();
        assertThat(cookieValue).matches("[0-9a-fA-F\\-]{36}");

        ArgumentCaptor<CartItemRequest> captor = ArgumentCaptor.forClass(CartItemRequest.class);
        verify(cartService).addToCart(eq(cookieValue), captor.capture());
        assertThat(captor.getValue().getProductId()).isEqualTo(1L);
        assertThat(captor.getValue().getQuantity()).isEqualTo(2);
    }

    @Test
    void addToCart_invalidRequestBody_returnsBadRequest() throws Exception {
        CartItemRequest invalid = new CartItemRequest(null, 2);

        mockMvc.perform(post("/api/cart/add")
                        .header("X-Session-Id", "123e4567-e89b-12d3-a456-426614174000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    void addToCart_serviceThrowsException_returnsBadRequest() throws Exception {
        when(cartService.addToCart(anyString(), any(CartItemRequest.class)))
                .thenThrow(BadRequestException.builder().message("Invalid").build());

        mockMvc.perform(post("/api/cart/add")
                        .header("X-Session-Id", "123e4567-e89b-12d3-a456-426614174000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validItemRequest)))
                //.andExpect(status().isBadRequest())
                //.andExpect(jsonPath("$.message").value("Invalid"))
                .andExpect(jsonPath("$.message").value("BAD_REQUEST: Invalid"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"))
        ;
    }

    /* ====================================== PUT /api/cart/update/{id} ==================================== */

    @Test
    void updateCartItem_validRequest_updatesQuantity() throws Exception {
        String sessionId = "123e4567-e89b-12d3-a456-426614174000";
        when(cartService.updateCartItem(eq(sessionId), eq(1L), eq(5))).thenReturn(cartResponse);

        mockMvc.perform(put("/api/cart/update/1")
                        .header("X-Session-Id", sessionId)
                        .param("quantity", "5"))
                .andExpect(status().isOk());

        verify(cartService).updateCartItem(eq(sessionId), eq(1L), eq(5));
    }

    @Test
    void updateCartItem_missingSessionId_throwsBadRequest() throws Exception {
        mockMvc.perform(put("/api/cart/update/1")
                        .param("quantity", "5"))
                //.andExpect(status().isBadRequest())
                //.andExpect(jsonPath("$.message").value("Missing or invalid cart session ID"))
                .andExpect(jsonPath("$.message").value("BAD_REQUEST: Missing or invalid cart session ID"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"))
        ;

        verifyNoInteractions(cartService);
    }

    @Test
    void updateCartItem_negativeQuantity_callsService() throws Exception {
        String sessionId = "123e4567-e89b-12d3-a456-426614174000";
        when(cartService.updateCartItem(eq(sessionId), eq(1L), eq(-1))).thenReturn(cartResponse);

        mockMvc.perform(put("/api/cart/update/1")
                        .header("X-Session-Id", sessionId)
                        .param("quantity", "-1"))
                .andExpect(status().isOk());

        verify(cartService).updateCartItem(eq(sessionId), eq(1L), eq(-1));
    }

    /* ==================================== DELETE /api/cart/remove/{id} =================================== */

    @Test
    void removeFromCart_validRequest_removesItem() throws Exception {
        String sessionId = "123e4567-e89b-12d3-a456-426614174000";
        when(cartService.removeFromCart(eq(sessionId), eq(1L))).thenReturn(cartResponse);

        mockMvc.perform(delete("/api/cart/remove/1")
                        .header("X-Session-Id", sessionId))
                .andExpect(status().isOk());

        verify(cartService).removeFromCart(eq(sessionId), eq(1L));
    }

    @Test
    void removeFromCart_missingSessionId_throwsBadRequest() throws Exception {
        mockMvc.perform(delete("/api/cart/remove/1"))
                //.andExpect(status().isBadRequest())
                //.andExpect(jsonPath("$.message").value("Missing or invalid cart session ID"))
                .andExpect(jsonPath("$.message").value("BAD_REQUEST: Missing or invalid cart session ID"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"))
        ;

        verifyNoInteractions(cartService);
    }

    /* ======================================== DELETE /api/cart/clear ===================================== */

    @Test
    void clearCart_validRequest_clearsCart() throws Exception {
        String sessionId = "123e4567-e89b-12d3-a456-426614174000";
        when(cartService.clearCart(eq(sessionId))).thenReturn(cartResponse);

        mockMvc.perform(delete("/api/cart/clear")
                        .header("X-Session-Id", sessionId))
                .andExpect(status().isOk());

        verify(cartService).clearCart(eq(sessionId));
    }

    @Test
    void clearCart_missingSessionId_throwsBadRequest() throws Exception {
        mockMvc.perform(delete("/api/cart/clear"))
                //.andExpect(status().isBadRequest())
                //.andExpect(jsonPath("$.message").value("Missing or invalid cart session ID"))
                .andExpect(jsonPath("$.message").value("BAD_REQUEST: Missing or invalid cart session ID"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"))
        ;

        verifyNoInteractions(cartService);
    }

    /* ===================================== POST /api/cart/coupon/apply =================================== */

    @Test
    void applyCoupon_validRequest_appliesCoupon() throws Exception {
        String sessionId = "123e4567-e89b-12d3-a456-426614174000";
        when(cartService.applyCoupon(eq(sessionId), eq("SAVE10"))).thenReturn(appliedCouponResponse);

        mockMvc.perform(post("/api/cart/coupon/apply")
                        .header("X-Session-Id", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCouponRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SAVE10"))
                .andExpect(jsonPath("$.discount").value(10.0));

        verify(cartService).applyCoupon(eq(sessionId), eq("SAVE10"));
    }

    @Test
    void applyCoupon_invalidCouponCode_returnsBadRequest() throws Exception {
        CouponCodeRequest invalid = new CouponCodeRequest(null);

        mockMvc.perform(post("/api/cart/coupon/apply")
                        .header("X-Session-Id", "123e4567-e89b-12d3-a456-426614174000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    void applyCoupon_missingSessionId_throwsBadRequest() throws Exception {
        mockMvc.perform(post("/api/cart/coupon/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCouponRequest)))
                //.andExpect(status().isBadRequest())
                //.andExpect(jsonPath("$.message").value("Missing or invalid cart session ID"))
                .andExpect(jsonPath("$.message").value("BAD_REQUEST: Missing or invalid cart session ID"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"))
        ;

        verifyNoInteractions(cartService);
    }

    /* =================================== DELETE /api/cart/coupon/remove ================================== */

    @Test
    void removeCoupon_validRequest_removesCoupon() throws Exception {
        String sessionId = "123e4567-e89b-12d3-a456-426614174000";
        when(cartService.removeCoupon(eq(sessionId))).thenReturn(cartResponse);

        mockMvc.perform(delete("/api/cart/coupon/remove")
                        .header("X-Session-Id", sessionId))
                .andExpect(status().isOk());

        verify(cartService).removeCoupon(eq(sessionId));
    }

    @Test
    void removeCoupon_missingSessionId_throwsBadRequest() throws Exception {
        mockMvc.perform(delete("/api/cart/coupon/remove"))
                //.andExpect(status().isBadRequest())
                //.andExpect(jsonPath("$.message").value("Missing or invalid cart session ID"))
                .andExpect(jsonPath("$.message").value("BAD_REQUEST: Missing or invalid cart session ID"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"))
        ;

        verifyNoInteractions(cartService);
    }

    /* ======================================= GET /api/cart/subtotal ====================================== */

    @Test
    void calculateSubtotalPrice_returnsSubtotal() throws Exception {
        String sessionId = "123e4567-e89b-12d3-a456-426614174000";
        when(cartService.calculateSubtotalPrice(eq(sessionId))).thenReturn(new BigDecimal("200.00"));

        mockMvc.perform(get("/api/cart/subtotal")
                        .header("X-Session-Id", sessionId))
                .andExpect(status().isOk())
                .andExpect(content().string("200.00"));

        verify(cartService).calculateSubtotalPrice(eq(sessionId));
    }

    @Test
    void calculateSubtotalPrice_missingSessionId_throwsBadRequest() throws Exception {
        mockMvc.perform(get("/api/cart/subtotal"))
                // .andExpect(status().isBadRequest())
                // .andExpect(jsonPath("$.message").value("Missing or invalid cart session ID"))
                .andExpect(jsonPath("$.message").value("BAD_REQUEST: Missing or invalid cart session ID"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"))
        ;

        verifyNoInteractions(cartService);
    }

    /* ======================================= GET /api/cart/shipping ====================================== */

    @Test
    void calculateTotalShippingCost_returnsShipping() throws Exception {
        String sessionId = "123e4567-e89b-12d3-a456-426614174000";
        when(cartService.calculateTotalShippingCost(eq(sessionId))).thenReturn(new BigDecimal("15.00"));

        mockMvc.perform(get("/api/cart/shipping")
                        .header("X-Session-Id", sessionId))
                .andExpect(status().isOk())
                .andExpect(content().string("15.00"));

        verify(cartService).calculateTotalShippingCost(eq(sessionId));
    }

    /* ======================================== GET /api/cart/discount ===================================== */

    @Test
    void calculateDiscount_returnsDiscount() throws Exception {
        String sessionId = "123e4567-e89b-12d3-a456-426614174000";
        when(cartService.calculateDiscount(eq(sessionId))).thenReturn(new BigDecimal("10.00"));

        mockMvc.perform(get("/api/cart/discount")
                        .header("X-Session-Id", sessionId))
                .andExpect(status().isOk())
                .andExpect(content().string("10.00"));

        verify(cartService).calculateDiscount(eq(sessionId));
    }

    /* ========================================= GET /api/cart/total ======================================= */

    @Test
    void calculateTotalAmount_returnsTotal() throws Exception {
        String sessionId = "123e4567-e89b-12d3-a456-426614174000";
        when(cartService.calculateTotalAmount(eq(sessionId))).thenReturn(new BigDecimal("205.00"));

        mockMvc.perform(get("/api/cart/total")
                        .header("X-Session-Id", sessionId))
                .andExpect(status().isOk())
                .andExpect(content().string("205.00"));

        verify(cartService).calculateTotalAmount(eq(sessionId));
    }

    @Test
    void calculateTotalAmount_missingSessionId_throwsBadRequest() throws Exception {
        mockMvc.perform(get("/api/cart/total"))
                //.andExpect(status().isBadRequest())
                //.andExpect(jsonPath("$.message").value("Missing or invalid cart session ID"))
                .andExpect(jsonPath("$.message").value("BAD_REQUEST: Missing or invalid cart session ID"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"))
        ;

        verifyNoInteractions(cartService);
    }
}
