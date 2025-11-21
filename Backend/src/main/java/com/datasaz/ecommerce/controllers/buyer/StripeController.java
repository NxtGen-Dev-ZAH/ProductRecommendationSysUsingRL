package com.datasaz.ecommerce.controllers.buyer;

import com.datasaz.ecommerce.models.StripePaymentRequest;
import com.datasaz.ecommerce.models.StripePaymentResponse;
import com.datasaz.ecommerce.repositories.entities.Order;
import com.datasaz.ecommerce.services.interfaces.IStripeServiceV1;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/buyer/payment")
@RequiredArgsConstructor
public class StripeController {

    private final IStripeServiceV1 stripeService;

    @PostMapping("/stripe/create-session")
    public ResponseEntity<StripePaymentResponse> createPaymentSession(@RequestBody StripePaymentRequest request) {
        StripePaymentResponse response = stripeService.createPaymentSession(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/stripe/success")
    public ResponseEntity<Map<String, Object>> finalizeOrder(@RequestParam("session_id") String sessionId) {
        Map<String, Object> responseMap = new HashMap<>();
        try {
            Order savedOrder = stripeService.finalizeOrder(sessionId);
            if (savedOrder.getId() == null) {
                responseMap.put("error", "Erreur lors de la sauvegarde de la commande.");
                return ResponseEntity.status(500).body(responseMap);
            }
            responseMap.put("message", "Commande finalisée avec succès.");
            responseMap.put("orderId", savedOrder.getId());
            return ResponseEntity.ok().body(responseMap);
        } catch (Exception e) {
            responseMap.put("error", "Erreur lors de la finalisation de la commande: " + e.getMessage());
            return ResponseEntity.status(500).body(responseMap);
        }
    }
}