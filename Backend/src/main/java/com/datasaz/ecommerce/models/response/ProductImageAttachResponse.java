package com.datasaz.ecommerce.models.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageAttachResponse {
    private Long id;
    private String fileName;
    private String fileContent;
    private String thumbnailContent;
    private String contentType;
    private long fileSize;
    private String fileExtension;
    private LocalDateTime createdAt;
    private boolean isPrimary;
    private Integer displayOrder;
}