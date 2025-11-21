package com.datasaz.ecommerce.models.response;


import com.datasaz.ecommerce.repositories.entities.Product;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductDescriptionResponse {
    //ProductDescription productDescription;
    private Long id;
    private String description;
    private String metaComments;
    private Product product;
}



