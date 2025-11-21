package com.datasaz.ecommerce.models.response;

import com.datasaz.ecommerce.repositories.entities.ProductCondition;
import com.datasaz.ecommerce.repositories.entities.ProductSellType;
import com.datasaz.ecommerce.repositories.entities.ProductStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
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
    private Long authorId;
    private Long companyId; // verify if removed
    private Long categoryId;
    private ProductStatus productStatus;
    private ProductSellType productSellType;
    private ProductCondition productCondition;
    private String productConditionComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ProductImageResponse> images;
    private List<ProductImageAttachResponse> imageAttaches;
    private List<ProductVariantResponse> variants;
}

/*
import com.datasaz.ecommerce.repositories.entities.ProductCondition;
import com.datasaz.ecommerce.repositories.entities.ProductSellType;
import com.datasaz.ecommerce.repositories.entities.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {
    //Product product;
    private Long id;
    private String name;
    private BigDecimal price;
    private BigDecimal offerPrice; //The initial price
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

    private Long categoryId;
    private Long authorId;
    private Long companyId;

    private ProductStatus productStatus;
    private ProductSellType productSellType;
    private ProductCondition productCondition;
    private String productConditionComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    //private boolean deleted;

    private List<ProductImageResponse> images;
    private List<ProductVariantResponse> variants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductImageResponse {
        private Long id;
        private String fileName;
        private String fileUrl;
        private String contentType;
        private long fileSize;
        private String fileExtension;
        private LocalDateTime createdAt;
        private boolean isPrimary;
        private int displayOrder;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductVariantResponse {
        private Long id;
        private String name;
        private BigDecimal priceAdjustment;
        private int quantity;
    }

//    private Users author;
//   private Category category;

//    private ProductDescription productDescription;
//    private ProductStatus productStatus;
//    private ProductStatistics productStatistics;
//
//    private List<ProductImage> images = new ArrayList<>();
//
//    private List<ProductCustomFields> customFields;
//      private Set<Users> usersFavourite = new HashSet<>();

//    private List<InvoiceItem> orderItems;
//    private Set<ProductReview> reviews = new HashSet<>();
//    private Set<PublicityCompaign> promotions = new HashSet<>();

}
*/


