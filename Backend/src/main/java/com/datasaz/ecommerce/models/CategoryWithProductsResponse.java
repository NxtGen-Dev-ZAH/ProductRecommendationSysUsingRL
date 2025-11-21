package com.datasaz.ecommerce.models;

import com.datasaz.ecommerce.repositories.entities.Product;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CategoryWithProductsResponse {
    private Long id;
    private String name;
    private String description;
    private List<Product> products;
}
