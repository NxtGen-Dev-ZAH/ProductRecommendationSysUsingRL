package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.models.StripePaymentRequest;
import com.datasaz.ecommerce.models.StripePaymentResponse;
import com.datasaz.ecommerce.repositories.entities.Order;
import com.datasaz.ecommerce.services.interfaces.IOrderService;
import com.datasaz.ecommerce.services.interfaces.IStripeServiceV1;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeServiceV1 implements IStripeServiceV1 {

    @Value("${stripe.apiKey}")
    private String apiKey;

    @Value("${payment.successUrl}")
    private String successUrl;

    @Value("${payment.cancelUrl}")
    private String cancelUrl;

    private final IOrderService orderService;

    public StripePaymentResponse createPaymentSession(StripePaymentRequest request) {
        log.info("Création de la session de paiement Stripe pour le client ID: {}", request.getClientId());
        Stripe.apiKey = apiKey;

        try {
            // Créer les paramètres de session Stripe
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L) // Nombre d'articles
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(request.getCurrency()) // Devise
                                                    .setUnitAmount((long) (request.getTotalAmount() * 100)) // Montant en centimes
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Commande Client #" + request.getClientId())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    // Ajout de métadonnées pour stocker des informations supplémentaires
                    .putMetadata("client_id", String.valueOf(request.getClientId()))
                    .putMetadata("address", request.getAddress()) // Adresse
                    .putMetadata("postal_code", request.getPostalCode()) // Code postal
                    .putMetadata("total_amount", String.valueOf(request.getTotalAmount())) // Montant total
                    //.putMetadata("currency", request.getCurrency()) // Devise utilisée
                    .build();

            // Créer une session Stripe
            Session session = Session.create(params);

            // Préparer la réponse

            // System.out.println("✅ Métadonnées ajoutées : client_id=" + request.getClientId() + ", address=" + request.getAddress() + ", postal_code=" + request.getPostalCode());

            return StripePaymentResponse.builder()
                    .sessionId(session.getId())
                    .paymentUrl(session.getUrl()).build();


        } catch (Exception e) {
            log.error("Erreur Stripe: {}", e.getMessage());
            throw new RuntimeException("Erreur Stripe: " + e.getMessage());
        }
    }

    @Override
    public Order finalizeOrder(String sessionId) throws StripeException {
        Stripe.apiKey = apiKey;
        Session session = Session.retrieve(sessionId);

        if (!session.getPaymentStatus().equalsIgnoreCase("paid")) {
            throw new RuntimeException("Le paiement n'est pas encore complété.");
        }
        Order order = Order.builder().id(Long.parseLong(session.getMetadata().get("client_id")))
                .totalAmount(BigDecimal.valueOf(session.getAmountTotal())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)) // Montant en centimes
                .build();

        return orderService.saveOrder(order);
    }

}

