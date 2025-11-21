package com.datasaz.ecommerce.repositories.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Table(name = "order_shipping")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderShipping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String shippingCarrier;
    private String shippingMethod;
    private String shippingMethodCurrency;
    private String shippingPrice;

    private String trackingUrl;
    private String trackingNumber;
    private String labelUrl;
    private String label;

    private int shippingQuantity; //number of items/packages shipped
    private String shippingWeight;
    private Boolean shippingDimensionRegularOrNot;
    private String shippingDimensionHeight;
    private String shippingDimensionWidth;
    private String shippingDimensionDepth;

    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;

    @Enumerated(EnumType.STRING)
    private ShippingStatus status;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @OneToOne(mappedBy = "orderShipping", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private ShippingTracking tracking;
}
