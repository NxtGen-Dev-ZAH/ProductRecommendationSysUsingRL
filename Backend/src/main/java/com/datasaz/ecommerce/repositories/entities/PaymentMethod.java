package com.datasaz.ecommerce.repositories.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Table(name = "payment_method")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private PaymentMethods method;


    // Security Note: Never store full card numbers or CVV.
    // Use tokenization with a provider (e.g., Stripe, Braintree) and store only masked data.
    @Column
    private String cardHolderName;
    @Column
    private String cardNumberMasked;
    @Column
    private String cardCVVMasked;
    @Column
    private String expiryMonth;
    @Column
    private String expiryYear;
    @Column
    private String cardBrand; // VISA, MasterCard, etc.
    @Column
    private String cardLast4;
    @Column
    private String cardCountry;
    @Column
    private String cardCurrency;
    @Column
    private boolean isDefault;
    @Column
    private String token; // If using third-party tokenization (Stripe, etc.)
    @Column
    private LocalDateTime savedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_USER_ID", nullable = false)
    private User user;
}
