package com.datasaz.ecommerce.models.response;


import com.datasaz.ecommerce.repositories.entities.Product;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductCustomFieldsResponse {
    //ProductCustomFields productCustomFields;
    private Long id;
    private String fieldKey;
    private String fieldValue;
    private String description;

    private Product product;
}



