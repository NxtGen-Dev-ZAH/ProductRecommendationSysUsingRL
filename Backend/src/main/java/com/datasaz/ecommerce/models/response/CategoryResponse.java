package com.datasaz.ecommerce.models.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CategoryResponse {

    private Long id;
    private String name;
    private String description;

    private String imageUrl;
    private String imageContent;
    private String imageContentType;

    private LocalDateTime createdDate;

    private Long parentId;
    private List<CategoryResponse> subcategories;
    //private List<Product> products;
}



