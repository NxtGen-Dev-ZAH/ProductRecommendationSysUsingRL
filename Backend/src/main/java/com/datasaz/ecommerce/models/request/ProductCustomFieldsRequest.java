package com.datasaz.ecommerce.models.request;

import com.datasaz.ecommerce.repositories.entities.Product;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductCustomFieldsRequest {

    private String fieldKey;
    private String fieldValue;
    private String description;

    private Product product;
}
