package com.datasaz.ecommerce.mappers;

import com.datasaz.ecommerce.configs.GroupConfig;
import com.datasaz.ecommerce.models.request.ProductRequest;
import com.datasaz.ecommerce.models.request.ProductVariantRequest;
import com.datasaz.ecommerce.models.response.ProductImageAttachResponse;
import com.datasaz.ecommerce.models.response.ProductImageResponse;
import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.models.response.ProductVariantResponse;
import com.datasaz.ecommerce.repositories.CategoryRepository;
import com.datasaz.ecommerce.repositories.entities.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final CategoryRepository categoryRepository;
    private final GroupConfig groupConfig;

    public ProductResponse toResponse(Product product) {
        if (product == null) {
            log.warn("toResponse: Product is null");
            throw new IllegalArgumentException("Product cannot be null");
        }

        log.info("toResponse: Converting product ID: {} to response", product.getId());

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .offerPrice(Optional.ofNullable(product.getOfferPrice()).orElse(product.getPrice()))
                .quantity(product.getQuantity())
                .price(product.getShippingCost())
                .price(product.getEachAdditionalItemShippingCost())
                .inventoryLocation(product.getInventoryLocation())
                .warranty(product.getWarranty())
                .brand(product.getBrand())
                .productCode(product.getProductCode())
                .manufacturingPieceNumber(product.getManufacturingPieceNumber())
                .manufacturingDate(product.getManufacturingDate())
                .expirationDate(product.getExpirationDate())
                .EAN(product.getEAN())
                .manufacturingPlace(product.getManufacturingPlace())
                .authorId(Optional.ofNullable(product.getAuthor())
                        .map(User::getId)
                        .orElseThrow(() -> new IllegalArgumentException("Product author cannot be null for ID: " + product.getId())))
                .companyId(Optional.ofNullable(product.getCompany())
                        .map(Company::getId)
                        .orElse(null))
                .categoryId(Optional.ofNullable(product.getCategory())
                        .map(Category::getId)
                        .orElseThrow(() -> new IllegalArgumentException("Product category cannot be null for ID: " + product.getId())))
                .productStatus(product.getProductStatus())
                .productSellType(product.getProductSellType())
                .productCondition(product.getProductCondition())
                .productConditionComment(product.getProductConditionComment())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .images(groupConfig.imageStorageMode.equals("database") ? Collections.emptyList() : mapImages(product.getImages()))
                .imageAttaches(groupConfig.imageStorageMode.equals("database") ? mapImageAttaches(product.getImageAttaches()) : Collections.emptyList())
                .variants(mapVariants(product.getVariants()))
                .build();
    }

    public Product toEntity(ProductRequest productRequest, User author) {
        if (productRequest == null || author == null) {
            log.warn("toEntity: Invalid input - productRequest: {}, author: {}", productRequest, author);
            throw new IllegalArgumentException("ProductRequest and User cannot be null");
        }

        log.info("toEntity: Converting product request for user: {}", author.getEmailAddress());

        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + productRequest.getCategoryId()));

        Product product = Product.builder()
                .name(productRequest.getName())
                .price(productRequest.getPrice())
                .offerPrice(productRequest.getOfferPrice())
                .quantity(productRequest.getQuantity())
                .shippingCost(productRequest.getShippingCost())
                .eachAdditionalItemShippingCost(productRequest.getEachAdditionalItemShippingCost())
                .inventoryLocation(productRequest.getInventoryLocation())
                .warranty(productRequest.getWarranty())
                .brand(productRequest.getBrand())
                .productCode(productRequest.getProductCode())
                .manufacturingPieceNumber(productRequest.getManufacturingPieceNumber())
                .manufacturingDate(productRequest.getManufacturingDate())
                .expirationDate(productRequest.getExpirationDate())
                .EAN(productRequest.getEAN())
                .manufacturingPlace(productRequest.getManufacturingPlace())
                .productStatus(productRequest.getQuantity() > 0 ? ProductStatus.AVAILABLE : ProductStatus.OUT_OF_STOCK)
                .productSellType(productRequest.getProductSellType())
                .productCondition(productRequest.getProductCondition())
                .productConditionComment(productRequest.getProductConditionComment())
                .category(category)
                .author(author)
                .build();

        List<ProductVariant> variants = mapVariantsToEntity(productRequest.getVariants(), product);
        product.setVariants(variants);

        return product;
    }

    private List<ProductImageResponse> mapImages(List<ProductImage> images) {
        if (images == null || images.isEmpty()) {
            log.debug("mapImages: No images provided for product");
            return Collections.emptyList();
        }
        return images.stream()
                .map(image -> ProductImageResponse.builder()
                        .id(image.getId())
                        .fileName(image.getFileName())
                        .fileUrl(image.getFileUrl())
                        .contentType(image.getContentType())
                        .fileSize(image.getFileSize())
                        .fileExtension(image.getFileExtension())
                        .isPrimary(image.isPrimary())
                        .displayOrder(image.getDisplayOrder())
                        .createdAt(image.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private List<ProductImageAttachResponse> mapImageAttaches(List<ProductImageAttach> imageAttaches) {
        if (!Hibernate.isInitialized(imageAttaches)) {
            log.warn("mapImageAttaches: imageAttaches is not initialized");
            return Collections.emptyList();
        }
        if (imageAttaches == null || imageAttaches.isEmpty()) {
            log.debug("mapImageAttaches: No image attaches provided for product");
            return Collections.emptyList();
        }
        return imageAttaches.stream()
                .map(image -> ProductImageAttachResponse.builder()
                        .id(image.getId())
                        .fileName(image.getFileName())
                        .contentType(image.getContentType())
                        .fileSize(image.getFileSize())
                        .fileExtension(image.getFileExtension())
                        .fileContent(image.getFileContent() != null ? Base64.getEncoder().encodeToString(image.getFileContent()) : null)
                        .thumbnailContent(image.getThumbnailContent() != null ? Base64.getEncoder().encodeToString(image.getThumbnailContent()) : null)
                        .isPrimary(image.isPrimary())
                        .displayOrder(image.getDisplayOrder() != null ? image.getDisplayOrder() : 0)
                        .createdAt(image.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private List<ProductVariant> mapVariantsToEntity(List<ProductVariantRequest> variants, Product product) {
        if (variants == null || variants.isEmpty()) {
            log.debug("mapVariantsToEntity: No variants provided for product");
            return Collections.emptyList();
        }
        return variants.stream()
                .map(variant -> ProductVariant.builder()
                        .name(variant.getName())
                        .priceAdjustment(variant.getPriceAdjustment())
                        .quantity(variant.getQuantity())
                        .product(product)
                        .build())
                .collect(Collectors.toList());
    }

    private List<ProductVariantResponse> mapVariants(List<ProductVariant> variants) {
        if (variants == null || variants.isEmpty()) {
            log.debug("mapVariants: No variants provided for product");
            return Collections.emptyList();
        }
        return variants.stream()
                .map(variant -> ProductVariantResponse.builder()
                        .id(variant.getId())
                        .name(variant.getName())
                        .priceAdjustment(variant.getPriceAdjustment())
                        .quantity(variant.getQuantity())
                        .build())
                .collect(Collectors.toList());
    }
}

/*
import com.datasaz.ecommerce.models.request.ProductRequest;
import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.repositories.CategoryRepository;
import com.datasaz.ecommerce.repositories.entities.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final CategoryRepository categoryRepository;

    public ProductResponse toResponse(Product product) {
        if (product == null) {
            log.warn("toResponse: Product is null");
            throw new IllegalArgumentException("Product cannot be null");
        }

        log.info("toResponse: Converting product ID: {} to response", product.getId());

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .offerPrice(Optional.ofNullable(product.getOfferPrice()).orElse(product.getPrice()))
                .quantity(product.getQuantity())
                .inventoryLocation(product.getInventoryLocation())
                .warranty(product.getWarranty())
                .brand(product.getBrand())
                .productCode(product.getProductCode())
                .manufacturingPieceNumber(product.getManufacturingPieceNumber())
                .manufacturingDate(product.getManufacturingDate())
                .EAN(product.getEAN())
                .manufacturingPlace(product.getManufacturingPlace())
                .authorId(Optional.ofNullable(product.getAuthor())
                        .map(User::getId)
                        .orElseThrow(() -> new IllegalArgumentException("Product author cannot be null for ID: " + product.getId())))
                .categoryId(Optional.ofNullable(product.getCategory())
                        .map(Category::getId)
                        .orElseThrow(() -> new IllegalArgumentException("Product category cannot be null for ID: " + product.getId())))
                .productStatus(product.getProductStatus())
                .productSellType(product.getProductSellType())
                .productCondition(product.getProductCondition())
                .productConditionComment(product.getProductConditionComment())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .images(mapImages(product.getImages()))
                .variants(mapVariants(product.getVariants()))
                .build();
    }

    public Product toEntity(ProductRequest productRequest, User author) {
        if (productRequest == null || author == null) {
            log.warn("toEntity: Invalid input - productRequest: {}, author: {}", productRequest, author);
            throw new IllegalArgumentException("ProductRequest and User cannot be null");
        }

        log.info("toEntity: Converting product request for user: {}", author.getEmailAddress());

        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + productRequest.getCategoryId()));

        Product product = Product.builder()
                .name(productRequest.getName())
                .price(productRequest.getPrice())
                .offerPrice(productRequest.getOfferPrice())
                .quantity(productRequest.getQuantity())
                .inventoryLocation(productRequest.getInventoryLocation())
                .warranty(productRequest.getWarranty())
                .brand(productRequest.getBrand())
                .productCode(productRequest.getProductCode())
                .manufacturingPieceNumber(productRequest.getManufacturingPieceNumber())
                .manufacturingDate(productRequest.getManufacturingDate())
                .EAN(productRequest.getEAN())
                .manufacturingPlace(productRequest.getManufacturingPlace())
                .author(author)
                .category(category)
                .productStatus(productRequest.getQuantity() > 0 ? ProductStatus.AVAILABLE : ProductStatus.OUT_OF_STOCK)
                .productSellType(Optional.ofNullable(productRequest.getProductSellType()).orElse(ProductSellType.DIRECT))
                .productCondition(Optional.ofNullable(productRequest.getProductCondition()).orElse(ProductCondition.NEW))
                .productConditionComment(productRequest.getProductConditionComment())
                .build();

        product.setVariants(mapVariantsToEntity(productRequest.getVariants(), product));

        return product;
    }

    private List<ProductResponse.ProductImageResponse> mapImages(List<ProductImage> images) {
        return Optional.ofNullable(images)
                .map(list -> list.stream()
                        .map(img -> ProductResponse.ProductImageResponse.builder()
                                .id(img.getId())
                                .fileName(img.getFileName())
                                .fileUrl(img.getFileUrl())
                                .contentType(img.getContentType())
                                .isPrimary(img.isPrimary())
                                .build())
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    private List<ProductResponse.ProductVariantResponse> mapVariants(List<ProductVariant> variants) {
        return Optional.ofNullable(variants)
                .map(list -> list.stream()
                        .map(var -> ProductResponse.ProductVariantResponse.builder()
                                .id(var.getId())
                                .name(var.getName())
                                .priceAdjustment(var.getPriceAdjustment())
                                .quantity(var.getQuantity())
                                .build())
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    private List<ProductVariant> mapVariantsToEntity(List<ProductRequest.ProductVariantRequest> variants, Product product) {
        return Optional.ofNullable(variants)
                .map(list -> list.stream()
                        .map(var -> ProductVariant.builder()
                                .name(var.getName())
                                .priceAdjustment(var.getPriceAdjustment())
                                .quantity(var.getQuantity())
                                .product(product)
                                .build())
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }
}
*/


/*
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final CategoryRepository categoryRepository;
    //private final UserRepository userRepository = null;

    public ProductResponse toResponse(Product product) {
        log.info("mapToProductResponse: convert product to product response");
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .offerPrice(product.getOfferPrice() != null ? product.getOfferPrice() : product.getPrice())
                .quantity(product.getQuantity())
                .inventoryLocation(product.getInventoryLocation() != null ? product.getInventoryLocation() : null)
                .warranty(product.getWarranty())
                .brand(product.getBrand() != null ? product.getBrand() : null)
                .productCode(product.getProductCode())
                .manufacturingPieceNumber(product.getManufacturingPieceNumber())
                .manufacturingDate(product.getManufacturingDate())
                .EAN(product.getEAN() != null ? product.getEAN() : null)
                .manufacturingPlace(product.getManufacturingPlace())
                .authorId(product.getAuthor().getId())
                .categoryId(product.getCategory().getId())
                .productStatus(product.getProductStatus())
                .productSellType(product.getProductSellType())
                .productCondition(product.getProductCondition())
                .productConditionComment(product.getProductConditionComment())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())

                .images(product.getImages().stream()
                        .map(img -> ProductResponse.ProductImageResponse.builder()
                                .id(img.getId())
                                .fileName(img.getFileName())
                                .fileUrl(img.getFileUrl())
                                .contentType(img.getContentType())
                                .isPrimary(img.isPrimary())
                                .build())
                        .collect(Collectors.toList()))

                .variants(product.getVariants().stream()
                        .map(var -> ProductResponse.ProductVariantResponse.builder()
                                .id(var.getId())
                                .name(var.getName())
                                .priceAdjustment(var.getPriceAdjustment())
                                .quantity(var.getQuantity())
                                .build())
                        .collect(Collectors.toList()))

                .build();
    }

    public Product toEntity(ProductRequest productRequest, User author) {
        log.info("mapToProduct: convert product request to product for user " + author.getEmailAddress());

        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("mapToProduct: Category not found"));
        // User author = userRepository.findById(productRequest.getAuthorId())
        //        .orElseThrow(() -> new IllegalArgumentException("mapToProduct: Author not found"));

        Product product = Product.builder()
                .name(productRequest.getName())
                .price(productRequest.getPrice())
                .offerPrice(productRequest.getOfferPrice())
                .quantity(productRequest.getQuantity())
                .inventoryLocation(productRequest.getInventoryLocation())
                .warranty(productRequest.getWarranty())
                .brand(productRequest.getBrand())
                .productCode(productRequest.getProductCode())
                .manufacturingPieceNumber(productRequest.getManufacturingPieceNumber())
                .manufacturingDate(productRequest.getManufacturingDate())
                .EAN(productRequest.getEAN())
                .manufacturingPlace(productRequest.getManufacturingPlace())
                .author(author)
                .category(category)
                .productStatus(productRequest.getQuantity() > 0 ? ProductStatus.AVAILABLE : ProductStatus.OUT_OF_STOCK)
                .productSellType(productRequest.getProductSellType() == null ? ProductSellType.DIRECT : productRequest.getProductSellType())
                .productCondition(productRequest.getProductCondition() == null ? ProductCondition.NEW : productRequest.getProductCondition())
                .productConditionComment(productRequest.getProductConditionComment())
                .build();

        if (productRequest.getVariants() != null) {
            List<ProductVariant> variants = productRequest.getVariants().stream()
                    .map(var -> ProductVariant.builder()
                            .name(var.getName())
                            .priceAdjustment(var.getPriceAdjustment())
                            .quantity(var.getQuantity())
                            .product(product)
                            .build())
                    .collect(Collectors.toList());
            product.setVariants(variants);
        }

//        if (productRequest.getVariants() != null) {
//            product.getVariants().clear();
//            List<ProductVariant> variants = productRequest.getVariants().stream()
//                    .map(var -> ProductVariant.builder()
//                            .name(var.getName())
//                            .priceAdjustment(var.getPriceAdjustment())
//                            .quantity(var.getQuantity())
//                            .product(product)
//                            .build())
//                    .collect(Collectors.toList());
//            product.getVariants().addAll(variants);
//        }

        return product;
    }

//    public void updateEntityFromRequest(Product product, ProductRequest productRequest, CategoryRepository categoryRepository, UserRepository userRepository) {
//        log.info("updateProductFromRequest: update product from request");
//        Category category = categoryRepository.findById(productRequest.getCategoryId())
//                .orElseThrow(() -> new IllegalArgumentException("updateProductFromRequest: Category not found"));
//        User author = userRepository.findById(productRequest.getAuthorId())
//                .orElseThrow(() -> new IllegalArgumentException("updateProductFromRequest: Author not found"));
//
//        product.setName(productRequest.getName());
//        product.setPrice(productRequest.getPrice());
//        product.setOfferPrice(productRequest.getOfferPrice());
//        product.setQuantity(productRequest.getQuantity());
//        product.setInventoryLocation(productRequest.getInventoryLocation());
//        product.setWarranty(productRequest.getWarranty());
//        product.setBrand(productRequest.getBrand());
//        product.setProductCode(productRequest.getProductCode());
//        product.setManufacturingPieceNumber(productRequest.getManufacturingPieceNumber());
//        product.setManufacturingDate(productRequest.getManufacturingDate());
//        product.setEAN(productRequest.getEAN());
//        product.setManufacturingPlace(productRequest.getManufacturingPlace());
//        product.setAuthor(author);
//        product.setCategory(category);
//    }

}
*/