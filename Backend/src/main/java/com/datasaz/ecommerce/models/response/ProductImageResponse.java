package com.datasaz.ecommerce.models.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageResponse {
    private Long id;
    private String fileName;
    private String fileUrl;
    private String contentType;
    private long fileSize;
    private String fileExtension;
    private LocalDateTime createdAt;
    private boolean isPrimary;
    private Integer displayOrder;
}