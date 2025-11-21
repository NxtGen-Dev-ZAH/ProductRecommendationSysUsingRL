package com.datasaz.ecommerce.services;

import com.datasaz.ecommerce.mappers.OrderMapper;
import com.datasaz.ecommerce.models.request.OrderCheckoutRequest;
import com.datasaz.ecommerce.models.response.OrderResponse;
import com.datasaz.ecommerce.repositories.*;
import com.datasaz.ecommerce.repositories.entities.*;
import com.datasaz.ecommerce.services.implementations.CartService;
import com.datasaz.ecommerce.services.implementations.CouponService;
import com.datasaz.ecommerce.services.implementations.OrderService;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@EnableRetry
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

//    @Test
//    void saveOrderTest() {
//        Order order = Order.builder()
//                .id(1L)
//                .orderDateTime(LocalDateTime.now())
//                .build();
//
//        when(orderRepository.save(order)).thenReturn(order);
//
//        Order savedOrder = orderService.saveOrder(order);
//        assertEquals(1L, savedOrder.getId());
//    }

    @Mock
    private CartRepository cartRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private CartService cartService;
    @Mock
    private CouponService couponService;

    //@InjectMocks private OrderService orderService;

    private OrderCheckoutRequest orderRequest;
    private Cart cart;
    private Product product;
    private User user;
    private Order order;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        orderRequest = OrderCheckoutRequest.builder()
                //.cartSessionId("test-session")
                //.buyerId(1L)
                .build();

        product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setPrice(new BigDecimal("99.99"));
        product.setQuantity(10);
        product.setVersion(1L);

        cart = new Cart();
        cart.setId(1L);
        cart.setSessionId("test-session");
        cart.setItems(new ArrayList<>());

        CartItem cartItem = new CartItem();
        cartItem.setId(1L);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);
        cart.getItems().add(cartItem);

        user = new User();
        user.setId(1L);

        order = Order.builder()
                .id(1L)
                .orderStatus(OrderStatus.PENDING)
                .orderDateTime(LocalDateTime.now())
                .buyer(user)
                .discountAmount(BigDecimal.ZERO)
                .totalVAT(new BigDecimal("39.996"))
                .totalAmount(new BigDecimal("239.976"))
                .build();

        orderResponse = OrderResponse.builder()
                .id(1L)
                .orderStatus(OrderStatus.PENDING)
                .orderDateTime(LocalDateTime.now())
                .buyerId(1L)
                .totalVAT(new BigDecimal("39.996"))
                .totalAmount(new BigDecimal("239.976"))
                .items(List.of())
                .build();
    }

//    @Test
//    void createOrder_validRequest_succeeds() {
//        when(cartRepository.findBySessionId("test-session")).thenReturn(Optional.of(cart));
//        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
//        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
//            Order o = inv.getArgument(0);
//            o.setId(1L);
//            return o;
//        });
//        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
//            Invoice i = inv.getArgument(0);
//            i.setId(1L);
//            return i;
//        });
//        when(orderItemRepository.saveAll(any())).thenReturn(new ArrayList<>());
//        when(productRepository.save(any(Product.class))).thenReturn(product);
//        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
//        when(orderMapper.toResponse(any(Order.class))).thenReturn(orderResponse);
//        doNothing().when(cartService).clearCart("test-session");
//
//        OrderResponse result = orderService.createOrder(orderRequest);
//
//        assertNotNull(result);
//        assertEquals(8, product.getQuantity());
//        assertEquals(new BigDecimal("239.976"), result.getTotalAmount());
//        verify(orderRepository).save(any(Order.class));
//        verify(invoiceRepository).save(any(Invoice.class));
//        verify(orderItemRepository).saveAll(any());
//    }
//
//    @Test
//    void createOrder_optimisticLockConflict_retriesAndSucceeds() {
//        when(cartRepository.findBySessionId("test-session")).thenReturn(Optional.of(cart));
//        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
//        when(productRepository.findById(1L))
//                .thenThrow(new OptimisticLockException())
//                .thenReturn(Optional.of(product));
//        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
//            Order o = inv.getArgument(0);
//            o.setId(1L);
//            return o;
//        });
//        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
//            Invoice i = inv.getArgument(0);
//            i.setId(1L);
//            return i;
//        });
//        when(orderItemRepository.saveAll(any())).thenReturn(new ArrayList<>());
//        when(productRepository.save(any(Product.class))).thenReturn(product);
//        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
//        when(orderMapper.toResponse(any(Order.class))).thenReturn(orderResponse);
//        doNothing().when(cartService).clearCart("test-session");
//
//        OrderResponse result = orderService.createOrder(orderRequest);
//
//        assertNotNull(result);
//        verify(productRepository, times(2)).findById(1L);
//        verify(orderRepository).save(any(Order.class));
//    }

//    @Test
//    void createOrder_cartNotFound_throwsException() {
//        when(cartRepository.findByUserWithItems(user)).thenReturn(Optional.empty());
//
//        assertThrows(CartNotFoundException.class, () -> orderService.createOrder(orderRequest));
//        verify(orderRepository, never()).save(any());
//    }

//    @Test
//    void createOrder_insufficientStock_throwsException() {
//        product.setQuantity(1);
//        when(cartRepository.findBySessionId("test-session")).thenReturn(Optional.of(cart));
//        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
//        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//
//        assertThrows(InsufficientStockException.class, () -> orderService.createOrder(orderRequest));
//        verify(productRepository, never()).save(any(Product.class));
//    }

    @Test
    void getOrder_validId_succeeds() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(new ArrayList<>());
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        OrderResponse result = orderService.getOrder(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(orderRepository).findById(1L);
    }

//    @Test
//    void updateOrderStatus_validStatus_succeeds() {
//        order.setOrderStatus(OrderStatus.PENDING);
//        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
//        when(orderItemRepository.findByOrderId(1L)).thenReturn(new ArrayList<>());
//        when(orderRepository.save(order)).thenReturn(order);
//        when(orderMapper.toResponse(order)).thenReturn(orderResponse);
//
//        OrderResponse result = orderService.updateOrderStatus(1L, "SHIPPED");
//
//        assertNotNull(result);
//        assertEquals(OrderStatus.SHIPPED, order.getOrderStatus());
//        verify(orderRepository).save(order);
//    }
//
//    @Test
//    void updateOrderStatus_invalidStatus_throwsException() {
//        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
//
//        assertThrows(RuntimeException.class, () -> orderService.updateOrderStatus(1L, "INVALID"));
//        verify(orderRepository, never()).save(any());
//    }
}