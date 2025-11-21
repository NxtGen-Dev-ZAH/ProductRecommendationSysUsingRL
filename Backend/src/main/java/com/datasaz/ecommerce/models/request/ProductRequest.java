package com.datasaz.ecommerce.models.request;

import com.datasaz.ecommerce.repositories.entities.ProductCondition;
import com.datasaz.ecommerce.repositories.entities.ProductSellType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {
    private String name;
    private BigDecimal price;
    private BigDecimal offerPrice;
    private int quantity;
    private BigDecimal shippingCost;
    private BigDecimal eachAdditionalItemShippingCost;
    private String inventoryLocation;
    private String warranty;
    private String brand;
    private String productCode;
    private String manufacturingPieceNumber;
    private LocalDate manufacturingDate;
    private LocalDate expirationDate;
    private String EAN;
    private String manufacturingPlace;
    private Long categoryId;
    private ProductSellType productSellType;
    private ProductCondition productCondition;
    private String productConditionComment;
    private List<ProductVariantRequest> variants;
}

/*
import com.datasaz.ecommerce.repositories.entities.ProductCondition;
import com.datasaz.ecommerce.repositories.entities.ProductSellType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ProductRequest {

    @NotBlank
    @Size(max = 100)
    private String name;
    @NotNull
    @PositiveOrZero
    private BigDecimal price;
    @Positive
    @DecimalMax(value = "9999999999.99")
    private BigDecimal offerPrice; //The initial price
    @NotNull
    @Positive
    private int quantity;
    private String inventoryLocation;
    private String warranty;
    private String brand;
    private String productCode;
    private String manufacturingPieceNumber;
    private LocalDate manufacturingDate;
    private LocalDate expirationDate;
    private String EAN;
    private String manufacturingPlace;

    @Enumerated(EnumType.STRING)
    private ProductSellType productSellType;

    @Enumerated(EnumType.STRING)
    private ProductCondition productCondition;

    private String productConditionComment;

    @NotNull
    private Long categoryId;

    private Long companyId;
//    @NotNull
//    private Long authorId;  // set from authenticated user

    //   private List<ProductImageRequest> images;

    private List<ProductVariantRequest> variants;

//    @Data
//    @Builder
//    @NoArgsConstructor
//    @AllArgsConstructor
//    public static class ProductImageRequest {
//        @NotBlank
//        private String fileName;
//        private String fileUrl;
//        private String contentType;
//        private long fileSize;
//        private String fileExtension;
//        private LocalDateTime createdAt;
//        private boolean isPrimary;
//        private int displayOrder;
//    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductVariantRequest {
        @NotBlank
        @Size(max = 100)
        private String name;
        private BigDecimal priceAdjustment;
        @NotNull
        @Positive
        private int quantity;
    }


//    private ProductStatusRequest status;
//    private ProductStatisticsRequest statistics;
//    private ProductDescriptionRequest description;
//    private Set<UsersRequest> usersFavourite = new HashSet<>();
//    private List<InvoiceItemRequest> orderItems;
//    private Set<ProductReviewRequest> reviews = new HashSet<>();
//    private Set<PublicityCompaignRequest> promotions = new HashSet<>();
//
//    private List<ProductCustomFieldsRequest> customFields;
//    private List<ProductImageRequest> images;
}
*/