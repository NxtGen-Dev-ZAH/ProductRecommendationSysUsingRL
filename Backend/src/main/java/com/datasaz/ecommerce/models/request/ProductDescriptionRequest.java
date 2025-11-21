package com.datasaz.ecommerce.models.request;

import com.datasaz.ecommerce.repositories.entities.Product;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductDescriptionRequest {

    private String description;
    private String metaComments;
    private Product product;
}
