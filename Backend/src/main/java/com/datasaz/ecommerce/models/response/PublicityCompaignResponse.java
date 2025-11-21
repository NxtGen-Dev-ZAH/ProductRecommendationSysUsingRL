package com.datasaz.ecommerce.models.response;


import com.datasaz.ecommerce.repositories.entities.Product;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class PublicityCompaignResponse {
    //PromotionCompaign promotionCompaign;
    private Long id;
    private String title;
    private String description;
    private BigDecimal discountPercentage;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean active;
    private Set<Product> products;
}



