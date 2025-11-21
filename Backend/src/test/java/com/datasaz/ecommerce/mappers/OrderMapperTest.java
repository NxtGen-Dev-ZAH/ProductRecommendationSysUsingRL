package com.datasaz.ecommerce.mappers;


import com.datasaz.ecommerce.models.response.OrderResponse;
import com.datasaz.ecommerce.repositories.entities.Order;
import com.datasaz.ecommerce.repositories.entities.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OrderMapperTest {
    private OrderMapper orderMapper;
    private OrderItemMapper orderItemMapper;

    @BeforeEach
    void setUp() {
        orderItemMapper = new OrderItemMapper();
        orderMapper = new OrderMapper(orderItemMapper);
    }

//    @Test
//    void toResponse_validOrder_mapsCorrectly() {
//        Order order = Order.builder()
//                .id(1L)
//                .orderStatus(OrderStatus.PENDING)
//                .orderDateTime(LocalDateTime.now())
//                .discountAmount(new BigDecimal("10.00"))
//                .totalVAT(new BigDecimal("39.996"))
//                .totalAmount(new BigDecimal("239.976"))
//                .build();
//
//        User buyer = new User();
//        buyer.setId(1L);
//        order.setBuyer(buyer);
//
//
//        Coupon coupon = new Coupon();
//        coupon.setIdentifier("SAVE10");
//        order.setUsedCoupon(coupon);
//
//        OrderItem item = OrderItem.builder()
//                .id(1L)
//                .quantity(2)
//                .productName("Test Product")
//                .price(new BigDecimal("99.99"))
//                .build();
//        Product product = new Product();
//        product.setId(1L);
//        item.setProduct(product);
//        Invoice invoice = new Invoice();
//        invoice.setId(1L);
//        item.setInvoice(invoice);
//        order.setItems(Collections.singletonList(item));
//
//        OrderResponse response = orderMapper.toResponse(order);
//
//        assertNotNull(response);
//        assertEquals(1L, response.getId());
//        assertEquals(OrderStatus.PENDING, response.getOrderStatus());
//        assertEquals(1L, response.getBuyerId());
//        assertEquals(2L, response.getBuyerCompanyId());
//        assertEquals("SAVE10", response.getUsedCouponIdentifier());
//        assertEquals(new BigDecimal("10.00"), response.getDiscountAmount());
//        assertEquals(new BigDecimal("39.996"), response.getTotalVAT());
//        assertEquals(new BigDecimal("239.976"), response.getTotalAmount());
//        assertEquals(1, response.getItems().size());
//        assertEquals(1L, response.getItems().get(0).getId());
//        assertEquals(2, response.getItems().get(0).getQuantity());
//        assertEquals("Test Product", response.getItems().get(0).getProductName());
//        assertEquals(new BigDecimal("99.99"), response.getItems().get(0).getPrice());
//        assertEquals(1L, response.getItems().get(0).getProductId());
//        assertEquals(1L, response.getItems().get(0).getInvoiceId());
//    }
//
//    @Test
//    void toResponse_nullOrder_returnsNull() {
//        assertNull(orderMapper.toResponse(null));
//    }

    @Test
    void toResponse_nullFields_mapsCorrectly() {
        Order order = Order.builder()
                .id(1L)
                .orderStatus(OrderStatus.PENDING)
                .orderDateTime(LocalDateTime.now())
                .build();

        OrderResponse response = orderMapper.toResponse(order);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(OrderStatus.PENDING, response.getOrderStatus());
        assertNull(response.getBuyerId());
        assertNull(response.getBuyerCompanyId());
        assertNull(response.getUsedCouponIdentifier());
        assertNull(response.getDiscountAmount());
        assertNull(response.getTotalVAT());
        assertNull(response.getTotalAmount());
        assertTrue(response.getItems().isEmpty());
    }
}