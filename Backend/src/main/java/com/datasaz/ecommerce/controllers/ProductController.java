package com.datasaz.ecommerce.controllers;


import com.datasaz.ecommerce.configs.GroupConfig;
import com.datasaz.ecommerce.exceptions.ResourceNotFoundException;
import com.datasaz.ecommerce.models.response.ProductImageResponse;
import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.models.response.ProductVariantResponse;
import com.datasaz.ecommerce.repositories.ProductImageAttachRepository;
import com.datasaz.ecommerce.repositories.ProductImageRepository;
import com.datasaz.ecommerce.repositories.entities.ProductImage;
import com.datasaz.ecommerce.repositories.entities.ProductImageAttach;
import com.datasaz.ecommerce.services.interfaces.IProductService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Controller", description = "APIs for retrieving product information")
public class ProductController {

    private final IProductService productService;

    private final ProductImageRepository productImageRepository;
    private final ProductImageAttachRepository productImageAttachRepository;
    private final GroupConfig groupConfig;

    @Operation(summary = "Get product by ID", description = "Retrieves a product by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @GetMapping("/{id}")
    @RateLimiter(name = "getProduct")
    @Cacheable(value = "products", key = "#id")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        log.info("Fetching product with ID: {}", id);
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @Operation(summary = "Get all products", description = "Retrieves a paginated list of all products")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @GetMapping
    @RateLimiter(name = "getProducts")
    @Cacheable(value = "allProducts", key = "#page + '-' + #size")
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @RequestParam(value = "page", defaultValue = "0") @Parameter(description = "Page number") int page,
            @RequestParam(value = "size", defaultValue = "10") @Parameter(description = "Page size") int size) {
        log.info("Fetching all products, page: {}, size: {}", page, size);
        Page<ProductResponse> products = productService.getAllProducts(page, size);
        return ResponseEntity.ok(products);
    }

//    @Operation(summary = "Search products by name", description = "Searches products by name")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
//            @ApiResponse(responseCode = "429", description = "Too many requests")
//    })
//    @GetMapping("/search")
//    @RateLimiter(name = "searchProducts")
//    @Cacheable(value = "products", key = "#name")
//    public ResponseEntity<List<ProductResponse>> searchProductsByName(@RequestParam("name") String name) {
//        log.info("Searching products by name: {}", name);
//        List<ProductResponse> products = productService.searchProductsByName(name);
//        return ResponseEntity.ok(products);
//    }

    @Operation(summary = "Search products by name", description = "Searches products by name with pagination")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @GetMapping("/search")
    @RateLimiter(name = "searchProducts")
    @Cacheable(value = "products", key = "#name + '-' + #page + '-' + #size")
    public ResponseEntity<Page<ProductResponse>> searchProductsByName(
            @RequestParam("name") String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Searching products by name: {}, page: {}, size: {}", name, page, size);
        if (name == null || name.trim().isEmpty()) {
            log.warn("searchProductsByName: Name parameter is null or empty");
            return ResponseEntity.badRequest().body(Page.empty());
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> products = productService.searchProductsByName(name.trim(), pageable);
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Get products by category", description = "Retrieves products by category ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Category not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @GetMapping("/category/{categoryId}")
    @RateLimiter(name = "getProductsByCategory")
    @Cacheable(value = "productsByCategory", key = "#categoryId + '-' + #page + '-' + #size")
    public ResponseEntity<Page<ProductResponse>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        log.info("Fetching products for category ID: {}, page: {}, size: {}", categoryId, page, size);
        Page<ProductResponse> products = productService.getProductsByCategory(categoryId, page, size);
        return ResponseEntity.ok(products);
    }

//    @Operation(summary = "Get products by company", description = "Retrieves products by company ID")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
//            @ApiResponse(responseCode = "404", description = "Company not found"),
//            @ApiResponse(responseCode = "429", description = "Too many requests")
//    })
//    @GetMapping("/company/{companyId}")
//    @RateLimiter(name = "getProductsByCompany")
//    @Cacheable(value = "productsByCompany", key = "#companyId + '-' + #page + '-' + #size")
//    public ResponseEntity<Page<ProductResponse>> getProductsByCompany(
//            @PathVariable Long companyId,
//            @RequestParam(value = "page", defaultValue = "0") int page,
//            @RequestParam(value = "size", defaultValue = "10") int size) {
//        log.info("Fetching products for company ID: {}, page: {}, size: {}", companyId, page, size);
//        Page<ProductResponse> products = productService.getAuthorOrCompanyProducts(companyId, page, size);
//        return ResponseEntity.ok(products);
//    }

    @Operation(summary = "Get product variants", description = "Retrieves all variants for a product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Variants retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @GetMapping("/{productId}/variants")
    @RateLimiter(name = "getProductVariants")
    @Cacheable(value = "productVariants", key = "#productId")
    public ResponseEntity<List<ProductVariantResponse>> getProductVariants(@PathVariable Long productId) {
        log.info("Fetching variants for product ID: {}", productId);
        ProductResponse product = productService.getProductById(productId);
        return ResponseEntity.ok(product.getVariants());
    }

    @Operation(summary = "Get product primary image", description = "Retrieves the primary image for a product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Primary image retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Product or primary image not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @GetMapping("/{productId}/primary-image")
    @RateLimiter(name = "getProductPrimaryImage")
    @Cacheable(value = "productImages", key = "#productId + '-primary'")
    public ResponseEntity<?> getProductPrimaryImage(@PathVariable Long productId) {
        log.info("Fetching primary image for product ID: {}", productId);
        if (groupConfig.imageStorageMode.equals("database")) {
            ProductImageAttach image = productImageAttachRepository.findByProductIdAndIsPrimaryTrue(productId)
                    .orElseThrow(() -> ResourceNotFoundException.builder().message("Primary image not found for product ID: " + productId).build());
            ByteArrayResource resource = new ByteArrayResource(image.getFileContent());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(image.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.getFileName() + "\"")
                    .body(resource);
        } else {
            ProductImage image = productImageRepository.findByProductIdAndIsPrimaryTrue(productId)
                    .orElseThrow(() -> ResourceNotFoundException.builder().message("Primary image not found for product ID: " + productId).build());
            return ResponseEntity.ok(ProductImageResponse.builder()
                    .id(image.getId())
                    .fileName(image.getFileName())
                    .fileUrl(image.getFileUrl())
                    .contentType(image.getContentType())
                    .fileSize(image.getFileSize())
                    .fileExtension(image.getFileExtension())
                    .isPrimary(image.isPrimary())
                    .displayOrder(image.getDisplayOrder())
                    .createdAt(image.getCreatedAt())
                    .build());
        }
    }

    @Operation(summary = "Get product image", description = "Retrieves a product image by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Image not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @GetMapping("/{productId}/images/{imageId}")
    @RateLimiter(name = "getProductImage")
    @Cacheable(value = "productImages", key = "#productId + '-' + #imageId")
    public ResponseEntity<Resource> getProductImage(
            @PathVariable Long productId,
            @PathVariable Long imageId) {
        log.info("Fetching image {} for product {}", imageId, productId);

        if (groupConfig.imageStorageMode.equals("database")) {
            ProductImageAttach image = productImageAttachRepository.findById(imageId)
                    .orElseThrow(() -> ResourceNotFoundException.builder().message("Image not found with ID: " + imageId).build());

            if (!image.getProduct().getId().equals(productId)) {
                throw ResourceNotFoundException.builder().message("Image does not belong to product ID: " + productId).build();
            }

            ByteArrayResource resource = new ByteArrayResource(image.getFileContent());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(image.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.getFileName() + "\"")
                    .body(resource);
        } else {
            ProductImage image = productImageRepository.findById(imageId)
                    .orElseThrow(() -> ResourceNotFoundException.builder().message("Image not found with ID: " + imageId).build());

            if (!image.getProduct().getId().equals(productId)) {
                throw ResourceNotFoundException.builder().message("Image does not belong to product ID: " + productId).build();
            }

            // For file-based storage, the frontend will use fileUrl directly
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // Image served via fileUrl in ProductResponse
        }
    }

    @GetMapping("/{productId}/images")
    public ResponseEntity<List<byte[]>> getImagesByProductId(@PathVariable Long productId) {
        if (!groupConfig.imageStorageMode.equals("database")) {
            throw new UnsupportedOperationException("Image endpoint only supported in database mode");
        }

        List<ProductImageAttach> images = productImageAttachRepository.findByProductId(productId);
        if (images.isEmpty()) {
            throw ResourceNotFoundException.builder().message("No images found for product ID: " + productId).build();
        }

        List<byte[]> imageContents = images.stream()
                .map(ProductImageAttach::getFileContent)
                .filter(content -> content != null && content.length > 0)
                .toList();

        if (imageContents.isEmpty()) {
            throw ResourceNotFoundException.builder().message("No valid images found for product ID: " + productId).build();
        }

        log.info("Retrieved {} images for product ID: {}", imageContents.size(), productId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .body(imageContents);
    }

    @GetMapping("/images/{imageId}")
    public ResponseEntity<byte[]> getImageByImageId(@PathVariable Long imageId) {
        if (!groupConfig.imageStorageMode.equals("database")) {
            throw new UnsupportedOperationException("Image endpoint only supported in database mode");
        }

        ProductImageAttach image = productImageAttachRepository.findById(imageId)
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Image not found with ID: " + imageId).build());

        if (image.getFileContent() == null || image.getFileContent().length == 0) {
            throw ResourceNotFoundException.builder().message("Image content not found for image ID: " + imageId).build();
        }

        log.info("Retrieved image for image ID: {}", imageId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .body(image.getFileContent());
    }

    @GetMapping("/{productId}/images/thumbnail")
    public ResponseEntity<List<byte[]>> getThumbnailsByProductId(@PathVariable Long productId) {
        if (!groupConfig.imageStorageMode.equals("database")) {
            throw new UnsupportedOperationException("Thumbnail endpoint only supported in database mode");
        }

        List<ProductImageAttach> images = productImageAttachRepository.findByProductId(productId);
        if (images.isEmpty()) {
            throw ResourceNotFoundException.builder().message("No images found for product ID: " + productId).build();
        }

        List<byte[]> thumbnails = images.stream()
                .map(ProductImageAttach::getThumbnailContent)
                .filter(content -> content != null && content.length > 0)
                .toList();

        if (thumbnails.isEmpty()) {
            throw ResourceNotFoundException.builder().message("No thumbnails found for product ID: " + productId).build();
        }

        log.info("Retrieved {} thumbnails for product ID: {}", thumbnails.size(), productId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(thumbnails);
    }

    @GetMapping("/images/{imageId}/thumbnail")
    public ResponseEntity<byte[]> getThumbnailByImageId(@PathVariable Long imageId) {
        if (!groupConfig.imageStorageMode.equals("database")) {
            throw new UnsupportedOperationException("Thumbnail endpoint only supported in database mode");
        }

        ProductImageAttach image = productImageAttachRepository.findById(imageId)
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Image not found with ID: " + imageId).build());

        if (image.getThumbnailContent() == null || image.getThumbnailContent().length == 0) {
            throw ResourceNotFoundException.builder().message("Thumbnail not found for image ID: " + imageId).build();
        }

        log.info("Retrieved thumbnail for image ID: {}", imageId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(image.getThumbnailContent());
    }

    // ===================================================================
    // === FEATURED / HOMEPAGE
    // ===================================================================
    @Operation(summary = "Get featured products", description = "For homepage")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Featured products"))
    @GetMapping("/featured")
    @Cacheable(value = "featuredProducts")
    public ResponseEntity<List<ProductResponse>> getFeaturedProducts(
            @RequestParam(defaultValue = "6") int limit) {
        log.info("Public: Featured products, limit: {}", limit);

        Page<ProductResponse> page = productService.getFeaturedProducts(0, Math.min(limit, 20));
        return ResponseEntity.ok(page.getContent());
    }

    // ===================================================================
    // === NEW ARRIVALS
    // ===================================================================
    @Operation(summary = "Get new arrivals", description = "Recently added")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "New products"))
    @GetMapping("/new-arrivals")
    @Cacheable(value = "newArrivals")
    public ResponseEntity<List<ProductResponse>> getNewArrivals(
            @RequestParam(defaultValue = "8") int limit) {
        log.info("Public: New arrivals, limit: {}", limit);
        Pageable pageable = PageRequest.of(0, Math.min(limit, 20), Sort.by("createdAt").descending());
        Page<ProductResponse> page = productService.getNewArrivalProducts(pageable);
        return ResponseEntity.ok(page.getContent());
    }

    // ===================================================================
    // ===  BEST SELLERS
    // ===================================================================
    @Operation(summary = "Get best sellers", description = "Top sold products")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Best sellers"))
    @GetMapping("/best-sellers")
    @Cacheable(value = "bestSellers")
    public ResponseEntity<List<ProductResponse>> getBestSellers(
            @RequestParam(defaultValue = "8") int limit) {
        log.info("Public: Best sellers, limit: {}", limit);
        Pageable pageable = PageRequest.of(0, Math.min(limit, 20));
        Page<ProductResponse> page = productService.getAllSortedByProductSoldQuantity(pageable);
        return ResponseEntity.ok(page.getContent());
    }

    // ===================================================================
    // ===  ON SALE
    // ===================================================================
    @Operation(summary = "Get products on sale", description = "Offer price < regular price")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "On sale"))
    @GetMapping("/on-sale")
    @Cacheable(value = "onSaleProducts")
    public ResponseEntity<Page<ProductResponse>> getOnSaleProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Public: On sale, page: {}, size: {}", page, size);

        Page<ProductResponse> products = productService.getByOfferPriceLessThanPrice(page, size);
        return ResponseEntity.ok(products);
    }

    // ===================================================================
    // ===  RELATED PRODUCTS
    // ===================================================================
    @Operation(summary = "Get related products", description = "Same category, exclude self")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Related"))
    @GetMapping("/{id}/related")
    @Cacheable(value = "relatedProducts", key = "#id")
    public ResponseEntity<List<ProductResponse>> getRelatedProducts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "4") int limit) {
        log.info("Public: Related for {}, limit: {}", id, limit);
        ProductResponse product = productService.getProductById(id);
        Pageable pageable = PageRequest.of(0, Math.min(limit, 10));
        Page<ProductResponse> related = productService.getRelatedProducts(
                product.getCategoryId(), id, pageable);
        return ResponseEntity.ok(related.getContent());
    }

    // ===================================================================
    // === RECOMMENDATIONS (Authenticated)
    // ===================================================================
    @Operation(summary = "Get personalized recommendations", description = "Requires login")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Recommendations"))
    @GetMapping("/recommendations")
    @Cacheable(value = "recommendations", key = "#auth.name")
    public ResponseEntity<List<ProductResponse>> getRecommendations(
            Authentication auth,
            @RequestParam(defaultValue = "6") int limit) {
        if (auth == null) return ResponseEntity.ok(List.of());
        log.info("Auth: Recommendations for {}", auth.getName());

        Page<ProductResponse> page = productService.getRecommendedProducts(0, Math.min(limit, 20));
        return ResponseEntity.ok(page.getContent());
    }

}


/*

// ===================================================================
    // === IMAGES (DATABASE MODE)
    // ===================================================================
    @Operation(summary = "Get primary image", description = "Public")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @GetMapping("/{productId}/primary-image")
    @RateLimiter(name = "getProductPrimaryImage")
    public ResponseEntity<?> getPrimaryImage(@PathVariable Long productId) {
        return getImageResponse(productId, true);
    }

    @Operation(summary = "Get image by ID", description = "Public")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @GetMapping("/{productId}/images/{imageId}")
    @RateLimiter(name = "getProductImage")
    public ResponseEntity<Resource> getImage(
            @PathVariable Long productId,
            @PathVariable Long imageId) {
        return getImageResource(productId, imageId);
    }

    @Operation(summary = "Get all images (base64)", description = "Only in DB mode")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Images"))
    @GetMapping("/{productId}/images")
    public ResponseEntity<List<byte[]>> getAllImages(@PathVariable Long productId) {
        validateDbMode();
        List<byte[]> images = productImageAttachRepository.findByProductId(productId).stream()
                .map(ProductImageAttach::getFileContent)
                .filter(c -> c != null && c.length > 0)
                .toList();
        if (images.isEmpty()) throw notFound("No images");
        return okWithCache(images);
    }

    @Operation(summary = "Get thumbnail by image ID", description = "DB mode")
    @GetMapping("/images/{imageId}/thumbnail")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable Long imageId) {
        validateDbMode();
        ProductImageAttach img = productImageAttachRepository.findById(imageId)
                .orElseThrow(() -> notFound("Image ID: " + imageId));
        byte[] thumb = img.getThumbnailContent();
        if (thumb == null || thumb.length == 0) throw notFound("Thumbnail");
        return okWithCache(thumb, MediaType.IMAGE_JPEG);
    }

    // ===================================================================
    // === HELPERS
    // ===================================================================
    private ResponseEntity<?> getImageResponse(Long productId, boolean primary) {
        if (groupConfig.imageStorageMode.equals("database")) {
            ProductImageAttach img = (primary
                    ? productImageAttachRepository.findByProductIdAndIsPrimaryTrue(productId)
                    : productImageAttachRepository.findByProductId(productId).stream().findFirst())
                    .orElseThrow(() -> notFound("Image for " + productId));
            ByteArrayResource resource = new ByteArrayResource(img.getFileContent());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(img.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + img.getFileName() + "\"")
                    .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                    .body(resource);
        } else {
            ProductImage img = (primary
                    ? productImageRepository.findByProductIdAndIsPrimaryTrue(productId)
                    : productImageRepository.findByProductId(productId).stream().findFirst())
                    .orElseThrow(() -> notFound("Image for " + productId));
            return ResponseEntity.ok(ProductImageResponse.from(img));
        }
    }

    private ResponseEntity<Resource> getImageResource(Long productId, Long imageId) {
        validateDbMode();
        ProductImageAttach img = productImageAttachRepository.findById(imageId)
                .filter(i -> i.getProduct().getId().equals(productId))
                .orElseThrow(() -> notFound("Image ID: " + imageId));
        ByteArrayResource resource = new ByteArrayResource(img.getFileContent());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(img.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + img.getFileName() + "\"")
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .body(resource);
    }

    private void validateDbMode() {
        if (!groupConfig.imageStorageMode.equals("database")) {
            throw new UnsupportedOperationException("Only supported in database mode");
        }
    }

    private ResourceNotFoundException notFound(String msg) {
        return ResourceNotFoundException.builder().message(msg).build();
    }

    private <T> ResponseEntity<T> okWithCache(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .body(body);
    }

    private ResponseEntity<byte[]> okWithCache(byte[] body, MediaType type) {
        return ResponseEntity.ok()
                .contentType(type)
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .body(body);
    }


 */





/*
import com.datasaz.ecommerce.configs.GroupConfig;
import com.datasaz.ecommerce.exceptions.ResourceNotFoundException;
import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.repositories.ProductImageAttachRepository;
import com.datasaz.ecommerce.repositories.ProductImageRepository;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.entities.ProductImage;
import com.datasaz.ecommerce.repositories.entities.ProductImageAttach;
import com.datasaz.ecommerce.services.interfaces.IProductService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Controller", description = "APIs for retrieving product information")
public class ProductController {

    private final IProductService productService;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductImageAttachRepository productImageAttachRepository;
    private final GroupConfig groupConfig;

    @Operation(summary = "Get product by ID", description = "Retrieves a product by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @GetMapping("/{id}")
    @RateLimiter(name = "getProduct")
    @Cacheable(value = "products", key = "#id")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        log.info("Fetching product with ID: {}", id);
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @Operation(summary = "Get all products", description = "Retrieves a paginated list of all products")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @GetMapping
    @RateLimiter(name = "getProducts")
    @Cacheable(value = "allProducts", key = "#page + '-' + #size")
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        log.info("Fetching all products, page: {}, size: {}", page, size);
        Page<ProductResponse> products = productService.getAllProducts(page, size);
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Search products by name", description = "Searches products by name")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @GetMapping("/search")
    @RateLimiter(name = "searchProducts")
    @Cacheable(value = "products", key = "#name")
    public ResponseEntity<List<ProductResponse>> searchProductsByName(@RequestParam("name") String name) {
        log.info("Searching products by name: {}", name);
        List<ProductResponse> products = productService.searchProductsByName(name);
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Get products by category", description = "Retrieves products by category ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Category not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @GetMapping("/category/{categoryId}")
    @RateLimiter(name = "getProductsByCategory")
    @Cacheable(value = "productsByCategory", key = "#categoryId + '-' + #page + '-' + #size")
    public ResponseEntity<Page<ProductResponse>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        log.info("Fetching products for category ID: {}, page: {}, size: {}", categoryId, page, size);
        Page<ProductResponse> products = productService.getProductsByCategory(categoryId, page, size);
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Get product image", description = "Retrieves a product image by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Image not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @GetMapping("/{productId}/images/{imageId}")
    @RateLimiter(name = "getProductImage")
    @Cacheable(value = "productImages", key = "#productId + '-' + #imageId")
    public ResponseEntity<Resource> getProductImage(
            @PathVariable Long productId,
            @PathVariable Long imageId) {
        log.info("Fetching image {} for product {}", imageId, productId);

        if (groupConfig.imageStorageMode.equals("database")) {
            ProductImageAttach image = productImageAttachRepository.findById(imageId)
                    .orElseThrow(() -> ResourceNotFoundException.builder().message("Image not found with ID: " + imageId).build());

            if (!image.getProduct().getId().equals(productId)) {
                throw ResourceNotFoundException.builder().message("Image does not belong to product ID: " + productId).build();
            }

            ByteArrayResource resource = new ByteArrayResource(image.getFileContent());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(image.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.getFileName() + "\"")
                    .body(resource);
        } else {
            ProductImage image = productImageRepository.findById(imageId)
                    .orElseThrow(() -> ResourceNotFoundException.builder().message("Image not found with ID: " + imageId).build());

            if (!image.getProduct().getId().equals(productId)) {
                throw ResourceNotFoundException.builder().message("Image does not belong to product ID: " + productId).build();
            }

            // For file-based storage, the frontend will use fileUrl directly
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // Image served via fileUrl in ProductResponse
        }
    }

    @GetMapping("/{productId}/images")
    public ResponseEntity<List<byte[]>> getImagesByProductId(@PathVariable Long productId) {
        if (!groupConfig.imageStorageMode.equals("database")) {
            throw new UnsupportedOperationException("Image endpoint only supported in database mode");
        }

        List<ProductImageAttach> images = productImageAttachRepository.findByProductId(productId);
        if (images.isEmpty()) {
            throw ResourceNotFoundException.builder().message("No images found for product ID: " + productId).build();
        }

        List<byte[]> imageContents = images.stream()
                .map(ProductImageAttach::getFileContent)
                .filter(content -> content != null && content.length > 0)
                .toList();

        if (imageContents.isEmpty()) {
            throw ResourceNotFoundException.builder().message("No valid images found for product ID: " + productId).build();
        }

        log.info("Retrieved {} images for product ID: {}", imageContents.size(), productId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .body(imageContents);
    }

    @GetMapping("/images/{imageId}")
    public ResponseEntity<byte[]> getImageByImageId(@PathVariable Long imageId) {
        if (!groupConfig.imageStorageMode.equals("database")) {
            throw new UnsupportedOperationException("Image endpoint only supported in database mode");
        }

        ProductImageAttach image = productImageAttachRepository.findById(imageId)
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Image not found with ID: " + imageId).build());

        if (image.getFileContent() == null || image.getFileContent().length == 0) {
            throw ResourceNotFoundException.builder().message("Image content not found for image ID: " + imageId).build();
        }

        log.info("Retrieved image for image ID: {}", imageId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .body(image.getFileContent());
    }

    @GetMapping("/{productId}/images/thumbnail")
    public ResponseEntity<List<byte[]>> getThumbnailsByProductId(@PathVariable Long productId) {
        if (!groupConfig.imageStorageMode.equals("database")) {
            throw new UnsupportedOperationException("Thumbnail endpoint only supported in database mode");
        }

        List<ProductImageAttach> images = productImageAttachRepository.findByProductId(productId);
        if (images.isEmpty()) {
            throw ResourceNotFoundException.builder().message("No images found for product ID: " + productId).build();
        }

        List<byte[]> thumbnails = images.stream()
                .map(ProductImageAttach::getThumbnailContent)
                .filter(content -> content != null && content.length > 0)
                .toList();

        if (thumbnails.isEmpty()) {
            throw ResourceNotFoundException.builder().message("No thumbnails found for product ID: " + productId).build();
        }

        log.info("Retrieved {} thumbnails for product ID: {}", thumbnails.size(), productId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(thumbnails);
    }

    @GetMapping("/images/{imageId}/thumbnail")
    public ResponseEntity<byte[]> getThumbnailByImageId(@PathVariable Long imageId) {
        if (!groupConfig.imageStorageMode.equals("database")) {
            throw new UnsupportedOperationException("Thumbnail endpoint only supported in database mode");
        }

        ProductImageAttach image = productImageAttachRepository.findById(imageId)
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Image not found with ID: " + imageId).build());

        if (image.getThumbnailContent() == null || image.getThumbnailContent().length == 0) {
            throw ResourceNotFoundException.builder().message("Thumbnail not found for image ID: " + imageId).build();
        }

        log.info("Retrieved thumbnail for image ID: {}", imageId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(image.getThumbnailContent());
    }

}

*/

/*
import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.services.interfaces.ICategoryService;
import com.datasaz.ecommerce.services.interfaces.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final IProductService productService;
    private final ICategoryService categoryService;




    @GetMapping("/pages")
    public ResponseEntity<Page<ProductResponse>> getAllDeletedFalse(Pageable pageable) {
        return ResponseEntity.ok(productService.findAllProductsDeletedFalse(pageable));
    }


    @GetMapping("/visit-user/searchpageable")
    public Page<ProductResponse> searchProductsByNameCurrentAuthorId(@RequestParam String productName,
                                                                     @RequestParam String email,
                                                                     Pageable pageable) {
        return productService.findProductsByNameAuthorNameDeletedFalse(productName, email, pageable);
    }

    @GetMapping("/visit-user/all-pageable")
    public Page<ProductResponse> searchAllProductsByNameAuthorEmail(@RequestParam String email, Pageable pageable) {
        return productService.findProductsByAuthorEmailAddressDeletedFalse(email, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findProductByIdDeletedFalse(id));
    }

    @GetMapping("/getbysoldquantity")
    public Page<ProductResponse> searchProductsBySoldQuantity(Pageable pageable) {
        return productService.findProductBySoldQuantity(pageable);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<ProductResponse>> getByCategoryId(@PathVariable Long categoryId, Pageable pageable) {
        if (!categoryService.checkCategoryExisitsById(categoryId)) {
            throw new IllegalArgumentException("Category not found");
        }
        return ResponseEntity.ok(productService.findProductsByCategoryId(categoryId, pageable));
    }


}
*/