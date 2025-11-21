package com.datasaz.ecommerce.models.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder //(access = AccessLevel.PUBLIC)
@AllArgsConstructor
@NoArgsConstructor
public class CategoryRequest {

    @NotBlank
    private String name;
    private String description;
    private Long parentId;

    private String imageContent;
    private String imageContentType;
}
