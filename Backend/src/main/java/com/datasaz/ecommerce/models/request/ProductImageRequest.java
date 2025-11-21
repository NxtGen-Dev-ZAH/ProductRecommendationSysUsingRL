package com.datasaz.ecommerce.models.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ProductImageRequest {

    /// / private String fileType;
    // private Long productId;

    @NotBlank(message = "File content is required")
    private String fileContent;

    @NotBlank(message = "File name is required")
    private String fileName;

    private String contentType;

    private Integer displayOrder;
}
