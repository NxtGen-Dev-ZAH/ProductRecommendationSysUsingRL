package com.datasaz.ecommerce.repositories.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String invoiceNumber;

    @Column
    private LocalDateTime issuedAt;

    @Column(precision = 19, scale = 2)
    @Positive
    private BigDecimal subtotal;

    @Column(precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Column(precision = 19, scale = 2)
    @Positive
    private BigDecimal shippingCost;

    @Column(precision = 19, scale = 2)
    @Positive
    private BigDecimal totalVAT;

    @Column(precision = 19, scale = 2)
    @Positive
    private BigDecimal totalAmount;

    @OneToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

//    @Column(nullable = false, precision = 19, scale = 2)
//    @Positive
//    private BigDecimal totalDiscount;

    private String notes;

    @ManyToOne
    @JoinColumn(name = "buyer_user_id", nullable = false)
    private User buyerInvoice;
//
//    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL) //orphanRemoval = true
//    private List<OrderItem> items;

    @Version
    private Long version;

}

