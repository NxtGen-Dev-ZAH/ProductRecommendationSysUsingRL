package com.datasaz.ecommerce.controllers.buyer;

import com.datasaz.ecommerce.models.request.PaymentRequest;
import com.datasaz.ecommerce.models.response.PaymentResponse;
import com.datasaz.ecommerce.services.interfaces.IPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/buyer/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final IPaymentService paymentService;

    @Operation(summary = "Process a payment for an order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment session created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or payment amount mismatch"),
            @ApiResponse(responseCode = "403", description = "Not authorized to process payment"),
            @ApiResponse(responseCode = "404", description = "Order or shipping details not found")
    })
    @PreAuthorize("hasRole('BUYER')")
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.processPayment(request));
    }

    @Operation(summary = "Finalize a payment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment finalized successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payment method or transaction ID"),
            @ApiResponse(responseCode = "404", description = "Payment or order not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('BUYER')")
    @PostMapping("/finalize")
    public ResponseEntity<Map<String, Object>> finalizePayment(
            @RequestParam("transaction_id") String transactionId,
            @RequestParam("payment_method") String paymentMethod) {
        Map<String, Object> responseMap = new HashMap<>();
        try {
            PaymentResponse response = paymentService.finalizePayment(transactionId, paymentMethod);
            responseMap.put("message", "Payment finalized successfully.");
            responseMap.put("paymentId", response.getId());
            responseMap.put("orderId", response.getOrderId());
            return ResponseEntity.ok(responseMap);
        } catch (Exception e) {
            log.error("Error finalizing payment: {}", e.getMessage());
            responseMap.put("error", "Error finalizing payment: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseMap);
        }
    }
}