package com.datasaz.ecommerce.models.request;

import com.datasaz.ecommerce.repositories.entities.Product;
import com.datasaz.ecommerce.repositories.entities.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProductReviewRequest {

    private String comment;
    private int rating; // 1 to 5 stars
    private LocalDateTime createdAt;
    private User reviewer;
    private Product product;
}
