package com.datasaz.ecommerce.repositories.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Table(name = "return_requests")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @NotBlank
    private String reason;

    @Enumerated(EnumType.STRING)
    private ReturnStatus status;

    @Column
    private LocalDateTime requestDate;

    @Column(precision = 19, scale = 2)
    @Positive
    private BigDecimal refundAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "return_request_items",
            joinColumns = @JoinColumn(name = "return_request_id"),
            inverseJoinColumns = @JoinColumn(name = "order_item_id")
    )
    private List<OrderItem> items;

    @Column
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private BigDecimal refundPercentage;

}