package com.datasaz.ecommerce.repositories.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

//@Table(name = "cart")
@Table(name = "cart", indexes = {
        @Index(name = "idx_cart_session", columnList = "sessionId"),
        @Index(name = "idx_cart_user", columnList = "user_id")
})
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // For anonymous carts, store a unique session ID; for authenticated users, link to Users
    @Column(unique = true)
    private String sessionId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items;// = new ArrayList<>();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", referencedColumnName = "id")
    private Coupon coupon;

    @Column(precision = 19, scale = 2)
    @PositiveOrZero
    private BigDecimal subtotalPrice;

    @Column(precision = 19, scale = 2)
    @PositiveOrZero
    private BigDecimal totalShippingCost;

    @Column(precision = 19, scale = 2)
    @PositiveOrZero
    private BigDecimal totalDiscount;

    @Column(precision = 19, scale = 2)
    @PositiveOrZero
    private BigDecimal totalAmount;

    @Version
    private Long version;

    @Column(name = "last_modified")
    private LocalDateTime lastModified;

    @PrePersist
    @PreUpdate
    public void updateLastModified() {
        this.lastModified = LocalDateTime.now();
    }
}
