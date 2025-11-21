//package com.datasaz.ecommerce.controllers;
//
//import com.datasaz.ecommerce.controllers.buyer.OrderController;
//import com.datasaz.ecommerce.models.request.OrderRequest;
//import com.datasaz.ecommerce.models.response.OrderResponse;
//import com.datasaz.ecommerce.repositories.entities.OrderStatus;
//import com.datasaz.ecommerce.services.implementations.OrderService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@WebMvcTest(OrderController.class)
//class OrderControllerTest {
//    @Autowired
//    private MockMvc mockMvc;
//    @MockBean
//    private OrderService orderService;
//
//    private OrderRequest orderRequest;
//    private OrderResponse orderResponse;
//
//    @BeforeEach
//    void setUp() {
//        orderRequest = OrderRequest.builder()
//                .cartSessionId("test-session")
//                .buyerId(1L)
//                .build();
//
//        orderResponse = OrderResponse.builder()
//                .id(1L)
//                .orderStatus(OrderStatus.PENDING)
//                .orderDateTime(LocalDateTime.now())
//                .buyerId(1L)
//                .totalVAT(new BigDecimal("39.996"))
//                .totalAmount(new BigDecimal("239.976"))
//                .items(List.of())
//                .build();
//    }
//
//    @Test
//    void createOrder_validRequest_returnsCreated() throws Exception {
//        when(orderService.createOrder(any(OrderRequest.class))).thenReturn(orderResponse);
//
//        mockMvc.perform(post("/api/orders")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"sessionId\":\"test-session\",\"userId\":1}"))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.id").value(1L));
//    }
//
//    @Test
//    void getOrder_validId_returnsOk() throws Exception {
//        when(orderService.getOrder(1L)).thenReturn(orderResponse);
//
//        mockMvc.perform(get("/api/orders/1"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(1L));
//    }
//
//    @Test
//    void getOrdersByUser_validUserId_returnsOk() throws Exception {
//        when(orderService.getOrdersByUser(1L)).thenReturn(List.of(orderResponse));
//
//        mockMvc.perform(get("/api/orders/user/1"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].id").value(1L));
//    }
//
//    @Test
//    void updateOrderStatus_validStatus_returnsOk() throws Exception {
//        when(orderService.updateOrderStatus(1L, "SHIPPED")).thenReturn(orderResponse);
//
//        mockMvc.perform(put("/api/orders/1/status")
//                        .param("status", "SHIPPED"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(1L));
//    }
//}