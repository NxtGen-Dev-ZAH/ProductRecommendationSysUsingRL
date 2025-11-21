package com.datasaz.ecommerce.models.response;


import com.datasaz.ecommerce.repositories.entities.Product;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProductStatisticsResponse {
    //ProductStatistics productStatistics;
    private Long id;
    private LocalDateTime dateAnnounceCreation;
    private LocalDateTime dateAnnounceExpiration;
    private int announceViews;
    private LocalDateTime announceLastViewTime;

    private Product product;
}



