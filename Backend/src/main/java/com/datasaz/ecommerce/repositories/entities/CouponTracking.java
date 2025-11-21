package com.datasaz.ecommerce.repositories.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "coupon_tracking")
@Getter
@Setter
public class CouponTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_COUPON_ID", referencedColumnName = "id", nullable = false)
    private Coupon coupon;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", referencedColumnName = "id")
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CATEGORY_ID", referencedColumnName = "id")
    private Category category;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_ID", referencedColumnName = "id")
    private Product product;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COMPANY_ID", referencedColumnName = "id")
    private Company company;

    @Column(nullable = false)
    private boolean used;

    @PrePersist
    @PreUpdate
    private void validateTracking() {
        CouponCategory category = coupon.getCategory();
        if (category == CouponCategory.USER_SPECIFIC && user == null) {
            throw new IllegalStateException("User must be set for USER_SPECIFIC coupon tracking");
        }
        if (category == CouponCategory.PRODUCT_SPECIFIC && product == null) {
            throw new IllegalStateException("Product must be set for PRODUCT_SPECIFIC coupon tracking");
        }
        if (category == CouponCategory.CATEGORY_SPECIFIC && category == null) {
            throw new IllegalStateException("Category must be set for CATEGORY_SPECIFIC coupon tracking");
        }
        if (category == CouponCategory.COMPANY_SPECIFIC && company == null) {
            throw new IllegalStateException("Company must be set for COMPANY_SPECIFIC coupon tracking");
        }
    }

}
