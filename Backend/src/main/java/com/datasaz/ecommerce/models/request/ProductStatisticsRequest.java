package com.datasaz.ecommerce.models.request;


import com.datasaz.ecommerce.repositories.entities.Product;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProductStatisticsRequest {

    private LocalDateTime dateAnnounceCreation;
    private LocalDateTime dateAnnounceExpiration;
    private int announceViews;
    private LocalDateTime announceLastViewTime;

    private Product product;
}



