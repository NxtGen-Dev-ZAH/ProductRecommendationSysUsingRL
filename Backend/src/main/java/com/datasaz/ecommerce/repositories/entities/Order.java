package com.datasaz.ecommerce.repositories.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Table(name = "orders")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime orderDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_USER_ID", nullable = false)
    private User buyer;

    private List<Long> selectedCartItemIds;

    @ManyToOne
    @JoinColumn(name = "FK_COUPON_ID")
    private Coupon usedCoupon;

    @Column(precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    @Positive
    private BigDecimal totalVAT;

    @Column(nullable = false, precision = 19, scale = 2)
    @Positive
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private OrderShipping orderShipping;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_address_id", nullable = false)
    private Address billingAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_address_id", nullable = false)
    private Address shippingAddress;

//    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
//    private Address orderShippingCredential;
//
//    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
//    private OrderBillingCredential orderBillingCredential;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReturnRequest> returnRequests;

    @Version
    private Long version;

//    @OneToOne(mappedBy = "order")
//    private Payment payment;

//    @ManyToOne
//    @JoinColumn(name = "seller_user_id", nullable = false)
//    private Users seller;

//    @ManyToOne
//    @JoinColumn(name = "seller_company_id")
//    private UserCompany sellerCompany;


}
