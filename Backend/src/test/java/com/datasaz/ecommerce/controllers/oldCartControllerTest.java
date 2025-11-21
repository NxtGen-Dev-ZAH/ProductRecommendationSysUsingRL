package com.datasaz.ecommerce.controllers;
/*

import com.datasaz.ecommerce.models.Request.CartRequest;
import com.datasaz.ecommerce.models.Response.CartResponse;
import com.datasaz.ecommerce.repositories.entities.Product;
import com.datasaz.ecommerce.repositories.entities.Users;
import com.datasaz.ecommerce.services.interfaces.ICartService;
import com.datasaz.ecommerce.services.interfaces.ICouponService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class CartControllerTest {

    @Mock
    private ICartService cartService;

    @Mock
    private ICouponService couponService;

    @InjectMocks
    private CartController cartController;

    @BeforeEach
    void setUp() {
        cartController = new CartController(cartService, couponService);
    }

    @Test
    void testAddToCart() {
        Users mockUsers = Users.builder().id(1L).emailAddress("email").password("password").resetToken("resetToken").build();
        Product mockProduct = Product.builder().id(1L).name("product").price(BigDecimal.valueOf(100.0)).quantity(10).build();

        CartRequest request = CartRequest.builder()
               // .product(mockProduct)
               // .users(mockUsers)
                 .quantity(2)
                 .build();

         CartResponse response = CartResponse.builder()
                 .id(1L)
                // .product(mockProduct)
                 .users(mockUsers)
                 .discount(BigDecimal.valueOf(0.0))
                 .quantity(2)
                 .build();

         when(cartService.addToCart(request)).thenReturn(response);
         ResponseEntity<CartResponse> result = cartController.addToCart(request);
         Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
         Assertions.assertEquals(response, result.getBody());
         verify(cartService).addToCart(request);
     }
    @Test
    void testRemoveFromCart() {
        Long cartId = 1L;
        doNothing().when(cartService).removeFromCart(cartId);
        ResponseEntity<Void> result = cartController.removeFromCart(cartId);
        Assertions.assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(cartService).removeFromCart(cartId);
    }

    @Test
    void testUpdateCart() {
        Users mockUsers = Users.builder().id(1L).emailAddress("email").password("password").resetToken("resetToken").build();
        Product mockProduct = Product.builder().id(1L).name("product").price(BigDecimal.valueOf(100.0)).quantity(10).build();

        Long cartId = 1L;
        CartRequest request = CartRequest.builder()
             //   .product(mockProduct)
             //   .users(mockUsers)
              //  .discount(BigDecimal.valueOf(0.0))
                .quantity(3)
                .build();

        CartResponse response = CartResponse.builder()
                .id(cartId)
              //  .product(mockProduct)
              //  .users(mockUsers)
                .discount(BigDecimal.valueOf(0.0))
             //   .quantity(3)
                .build();

        when(cartService.updateCart(cartId, request.getQuantity())).thenReturn(response);
        ResponseEntity<CartResponse> result = cartController.updateCart(cartId, request);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertEquals(response, result.getBody());
        verify(cartService).updateCart(cartId, request.getQuantity());
    }
    @Test
    void testGetCart() {
        Users mockUsers = Users.builder().id(1L).emailAddress("email").password("password").resetToken("resetToken").build();
        Product mockProduct = Product.builder().id(1L).name("product").price(BigDecimal.valueOf(100.0)).quantity(10).build();

        CartResponse response = CartResponse.builder()
                .id(1L)
                .product(mockProduct)
                .users(mockUsers)
                .discount(BigDecimal.valueOf(0.0))
                .quantity(2)
                .build();

        when(cartService.getCartByUsers(mockUsers.getId())).thenReturn(List.of(response));
        ResponseEntity<CartResponse> result = cartController.getCart(mockUsers.getId());
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertEquals(List.of(response), result.getBody());
        verify(cartService).getCartByUsers(mockUsers.getId());
    }
    @Test
    void testGetTotalCart() {
        Users mockUsers = Users.builder().id(1L).emailAddress("email").password("password").resetToken("resetToken").build();
        BigDecimal total = new BigDecimal("100.00");
        Map<String,Object> response = new HashMap<>();
        response.put("total",total);
        when(cartService.calculateTotalAmount(mockUsers.getId())).thenReturn(total);
        ResponseEntity<?> result = cartController.getTotalCart(mockUsers.getId());
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertEquals(response, result.getBody());
        verify(cartService).calculateTotalAmount(mockUsers.getId());
    }
    @Test
    void testGetDiscount() {
        Users mockUsers = Users.builder().id(1L).emailAddress("email").password("password").resetToken("resetToken").build();
        BigDecimal discount = new BigDecimal(10.0);
        Map<String,Object> response = new HashMap<>();
        response.put("discount", discount);
        when(cartService.calculateDiscount(mockUsers.getId())).thenReturn(discount);
        ResponseEntity<?> result = cartController.getDiscount(mockUsers.getId());
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertEquals(response, result.getBody());
        verify(cartService).calculateDiscount(mockUsers.getId());
    }
    @Test
    void testClearCart() {
        //Long cartId = 1L;
        Users mockUsers = Users.builder().id(1L).emailAddress("email").password("password").resetToken("resetToken").build();
        doNothing().when(cartService).clearCart(mockUsers.getId());
        ResponseEntity<Void> result = cartController.clearCart(mockUsers.getId());
        Assertions.assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(cartService).clearCart(mockUsers.getId());
    }
}
*/