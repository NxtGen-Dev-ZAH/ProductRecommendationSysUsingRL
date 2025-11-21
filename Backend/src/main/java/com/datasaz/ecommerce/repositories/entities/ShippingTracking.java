package com.datasaz.ecommerce.repositories.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Table(name = "shipping_tracking")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String trackingNumber;

    @Column
    private String carrierStatus;

    @Column
    private LocalDateTime estimatedDeliveryDate;

    @Column
    private LocalDateTime lastUpdated;

    @OneToOne
    @JoinColumn(name = "order_shipping_id", nullable = false)
    private OrderShipping orderShipping;
}