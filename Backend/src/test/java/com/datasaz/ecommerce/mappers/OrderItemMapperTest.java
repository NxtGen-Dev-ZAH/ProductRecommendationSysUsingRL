package com.datasaz.ecommerce.mappers;


import com.datasaz.ecommerce.models.response.OrderItemResponse;
import com.datasaz.ecommerce.repositories.entities.Invoice;
import com.datasaz.ecommerce.repositories.entities.Order;
import com.datasaz.ecommerce.repositories.entities.OrderItem;
import com.datasaz.ecommerce.repositories.entities.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderItemMapperTest {
    private OrderItemMapper orderItemMapper;

    @BeforeEach
    void setUp() {
        orderItemMapper = new OrderItemMapper();
    }

    @Test
    void toResponse_validOrderItem_mapsCorrectly() {
        OrderItem item = OrderItem.builder()
                .id(1L)
                .quantity(2)
                .productName("Test Product")
                .price(new BigDecimal("99.99"))
                .build();

        Product product = new Product();
        product.setId(1L);
        item.setProduct(product);

        Order order = new Order();
        order.setId(1L);
        item.setOrder(order);

        Invoice invoice = new Invoice();
        invoice.setId(1L);
        item.setInvoice(invoice);

        OrderItemResponse response = orderItemMapper.toResponse(item);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(2, response.getQuantity());
        assertEquals("Test Product", response.getProductName());
        assertEquals(new BigDecimal("99.99"), response.getPrice());
        assertEquals(1L, response.getProductId());
        assertEquals(1L, response.getInvoiceId());
    }

//    @Test
//    void toResponse_nullOrderItem_returnsNull() {
//        assertNull(orderItemMapper.toResponse(null));
//    }

    @Test
    void toResponse_nullFields_mapsCorrectly() {
        OrderItem item = OrderItem.builder()
                .id(1L)
                .quantity(2)
                .productName("Test Product")
                .price(new BigDecimal("99.99"))
                .build();

        Product product = new Product();
        product.setId(1L);
        item.setProduct(product);


        OrderItemResponse response = orderItemMapper.toResponse(item);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(2, response.getQuantity());
        assertEquals("Test Product", response.getProductName());
        assertEquals(new BigDecimal("99.99"), response.getPrice());
        assertNull(response.getOrderId());
        assertEquals(1L, response.getProductId());
        assertNull(response.getInvoiceId());
    }
}