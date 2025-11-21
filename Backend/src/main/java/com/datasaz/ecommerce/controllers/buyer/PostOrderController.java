package com.datasaz.ecommerce.controllers.buyer;

import com.datasaz.ecommerce.models.request.ReturnRequestRequest;
import com.datasaz.ecommerce.models.request.ShippingTrackingRequest;
import com.datasaz.ecommerce.models.response.RefundResponse;
import com.datasaz.ecommerce.models.response.ReturnRequestResponse;
import com.datasaz.ecommerce.models.response.ShippingTrackingResponse;
import com.datasaz.ecommerce.services.interfaces.IPaymentService;
import com.datasaz.ecommerce.services.interfaces.IReturnService;
import com.datasaz.ecommerce.services.interfaces.IShippingService;
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
import java.util.List;
import java.util.Map;

@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/buyer/post-order")
@RequiredArgsConstructor
public class PostOrderController {
    private final IShippingService shippingService;
    private final IReturnService returnService;
    private final IPaymentService paymentService;

    @Operation(summary = "Update shipping tracking information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tracking information updated"),
            @ApiResponse(responseCode = "403", description = "Not authorized"),
            @ApiResponse(responseCode = "404", description = "Order or shipping details not found")
    })
    @PreAuthorize("hasRole('ADMIN')") // Only admins can update tracking
    @PostMapping("/tracking/{orderId}")
    public ResponseEntity<ShippingTrackingResponse> updateTracking(
            @PathVariable Long orderId,
            @Valid @RequestBody ShippingTrackingRequest request) {
        return ResponseEntity.ok(shippingService.updateTracking(orderId, request));
    }

    @Operation(summary = "Get shipping tracking information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tracking information retrieved"),
            @ApiResponse(responseCode = "403", description = "Not authorized"),
            @ApiResponse(responseCode = "404", description = "Order or tracking information not found")
    })
    @PreAuthorize("hasRole('BUYER')")
    @GetMapping("/tracking/{orderId}")
    public ResponseEntity<ShippingTrackingResponse> getTracking(@PathVariable Long orderId) {
        return ResponseEntity.ok(shippingService.getTracking(orderId));
    }

    @Operation(summary = "Create a return request")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Return request created"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "403", description = "Not authorized"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PreAuthorize("hasRole('BUYER')")
    @PostMapping("/return/{orderId}")
    public ResponseEntity<ReturnRequestResponse> createReturnRequest(
            @PathVariable Long orderId,
            @Valid @RequestBody ReturnRequestRequest request) {
        return ResponseEntity.ok(returnService.createReturnRequest(orderId, request));
    }

    @Operation(summary = "Get return requests for an order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Return requests retrieved"),
            @ApiResponse(responseCode = "403", description = "Not authorized"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PreAuthorize("hasRole('BUYER')")
    @GetMapping("/return/{orderId}")
    public ResponseEntity<List<ReturnRequestResponse>> getReturnRequests(@PathVariable Long orderId) {
        return ResponseEntity.ok(returnService.getReturnRequestsByOrder(orderId));
    }

    @Operation(summary = "Approve a return request")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Return request approved"),
            @ApiResponse(responseCode = "404", description = "Return request not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/return/approve/{returnRequestId}")
    public ResponseEntity<ReturnRequestResponse> approveReturnRequest(@PathVariable Long returnRequestId) {
        return ResponseEntity.ok(returnService.approveReturnRequest(returnRequestId));
    }

    @Operation(summary = "Reject a return request")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Return request rejected"),
            @ApiResponse(responseCode = "404", description = "Return request not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/return/reject/{returnRequestId}")
    public ResponseEntity<ReturnRequestResponse> rejectReturnRequest(
            @PathVariable Long returnRequestId,
            @RequestParam String rejectionReason) {
        return ResponseEntity.ok(returnService.rejectReturnRequest(returnRequestId, rejectionReason));
    }

    @Operation(summary = "Process a partial refund")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refund processed"),
            @ApiResponse(responseCode = "400", description = "Invalid refund request"),
            @ApiResponse(responseCode = "404", description = "Return request or payment not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/refund/{returnRequestId}")
    public ResponseEntity<Map<String, Object>> processPartialRefund(@PathVariable Long returnRequestId) {
        Map<String, Object> responseMap = new HashMap<>();
        try {
            RefundResponse refundResponse = paymentService.processPartialRefund(returnRequestId);
            responseMap.put("message", "Refund processed successfully.");
            responseMap.put("refundId", refundResponse.getId());
            responseMap.put("returnRequestId", returnRequestId);
            return ResponseEntity.ok(responseMap);
        } catch (Exception e) {
            log.error("Error processing refund: {}", e.getMessage());
            responseMap.put("error", "Error processing refund: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseMap);
        }
    }
}