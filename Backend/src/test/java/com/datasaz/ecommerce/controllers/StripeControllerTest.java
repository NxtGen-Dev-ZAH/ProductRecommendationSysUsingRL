package com.datasaz.ecommerce.controllers;

import com.datasaz.ecommerce.controllers.buyer.StripeController;
import com.datasaz.ecommerce.models.StripePaymentRequest;
import com.datasaz.ecommerce.models.StripePaymentResponse;
import com.datasaz.ecommerce.repositories.entities.Order;
import com.datasaz.ecommerce.services.interfaces.IStripeServiceV1;
import com.stripe.exception.StripeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeControllerTest {

    @Mock
    private IStripeServiceV1 stripeService;

    @InjectMocks
    private StripeController stripeController;

    @BeforeEach
    void setUp() {
        stripeController = new StripeController(stripeService);

    }

    @Test
    @DisplayName("Test createPaymentSession")
    void testCreatePaymentSession() {
        // Given
        StripePaymentRequest stripePaymentRequest = StripePaymentRequest.builder()
                .totalAmount(1000.0)
                .currency("USD")
                .successUrl("http://localhost:8080/success")
                .cancelUrl("http://localhost:8080/cancel")
                .clientId(1L)
                .address("123 Main St")
                .postalCode("12345")
                .build();
        StripePaymentResponse stripePaymentResponse = StripePaymentResponse.builder()
                .sessionId("session_123")
                .build();
        when(stripeService.createPaymentSession(stripePaymentRequest)).thenReturn(stripePaymentResponse);
        StripePaymentResponse result = stripeController.createPaymentSession(stripePaymentRequest).getBody();
        Assertions.assertEquals(stripePaymentResponse, result);
    }

    @Test
    @DisplayName("Test finalizeOrder")
    void testFinalizeOrder() throws StripeException {
        String sessionId = "session_123";
        Order mockOrder = Order.builder()
                .id(1L)
                .totalAmount(BigDecimal.valueOf(1000.0))
                .build();
        when(stripeService.finalizeOrder(sessionId)).thenReturn(mockOrder);
        var result = stripeController.finalizeOrder(sessionId).getBody();
        Assertions.assertEquals(1L, result.get("orderId"));
    }


}
