package com.datasaz.ecommerce.controllers.seller;

import com.datasaz.ecommerce.models.request.ProductRequest;
import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.services.interfaces.ISellerProductService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/seller/v1/products")
@RequiredArgsConstructor
@Tag(name = "Seller Product Controller", description = "APIs for managing products by sellers")
@PreAuthorize("hasRole('SELLER')")
public class SellerProductController {

    private final ISellerProductService sellerProductService;

    @Operation(summary = "Create a new product", description = "Creates a new product with optional images")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or too many images"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not a seller"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @RateLimiter(name = "manageProduct")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestPart("product") ProductRequest productRequest,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            Authentication authentication) {
        log.info("Creating product for user: {}", authentication.getName());
        ProductResponse response = sellerProductService.createProduct(productRequest, images, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Update a product", description = "Updates an existing product with optional new images")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @PutMapping(value = "/{productId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @RateLimiter(name = "manageProduct")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestPart("product") ProductRequest productRequest,
            @RequestPart(value = "images", required = false) List<MultipartFile> newImages,
            @RequestPart(value = "imagesToRemove", required = false) List<Long> imagesToRemove,
            @RequestPart(value = "primaryImageId", required = false) Long primaryImageId,
            Authentication authentication) {
        log.info("Updating product {} for user: {}", productId, authentication.getName());
        ProductResponse response = sellerProductService.updateProduct(productId, productRequest, newImages, imagesToRemove, primaryImageId, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update product images", description = "Updates images for an existing product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Images updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @PatchMapping(value = "/{productId}/update/images", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @RateLimiter(name = "updateProductImages")
    public ResponseEntity<ProductResponse> updateProductImages(
            @PathVariable Long productId,
            @RequestPart(value = "images", required = false) List<MultipartFile> newImages,
            @RequestPart(value = "imagesToRemove", required = false) List<Long> imagesToRemove,
            @RequestPart(value = "primaryImageId", required = false) Long primaryImageId,
            Authentication authentication) {
        log.info("Updating images for product {} by user: {}", productId, authentication.getName());
        ProductResponse response = sellerProductService.updateProductImages(productId, newImages, imagesToRemove, primaryImageId, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update product quantity", description = "Updates the quantity of an existing product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quantity updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid quantity"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @PatchMapping("/{productId}/quantity")
    @RateLimiter(name = "manageProduct")
    public ResponseEntity<ProductResponse> updateProductQuantity(
            @PathVariable Long productId,
            @RequestPart("quantity") int quantity,
            Authentication authentication) {
        log.info("Updating quantity for product {} by user: {}", productId, authentication.getName());
        ProductResponse response = sellerProductService.updateProductQuantity(productId, quantity);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update product price", description = "Updates the price of an existing product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Price updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid price"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @PatchMapping("/{productId}/price")
    @RateLimiter(name = "manageProduct")
    public ResponseEntity<ProductResponse> updateProductPrice(
            @PathVariable Long productId,
            @RequestPart("price") BigDecimal price,
            Authentication authentication) {
        log.info("Updating price for product {} by user: {}", productId, authentication.getName());
        ProductResponse response = sellerProductService.updateProductPrice(productId, price);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete a product", description = "Soft deletes a product by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @DeleteMapping("/{productId}")
    @RateLimiter(name = "manageProduct")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long productId,
            Authentication authentication) {
        log.info("Deleting product {} for user: {}", productId, authentication.getName());
        sellerProductService.deleteProduct(productId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search products by name", description = "Searches products by name for the authenticated seller")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> findProductsByName(
            @RequestParam("name") String productName,
            Authentication authentication) {
        log.info("Searching products by name: {} for user: {}", productName, authentication.getName());
        List<ProductResponse> products = sellerProductService.findProductsByNameAuthorIdDeletedFalse(productName);
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Get seller products", description = "Retrieves a paginated list of products for the authenticated seller, either associated with their company or authored by the seller.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User is not a seller"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/all-seller-products")
    //@PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Page<ProductResponse>> getSellerProducts(
            @RequestParam(defaultValue = "0") @Min(0) @Parameter(description = "Page number (0-based)") int page,
            @RequestParam(defaultValue = "10") @Min(1) @Parameter(description = "Number of products per page at least 1") int size) {
        log.debug("GET /seller/products - Fetching products for seller, page: {}, size: {}", page, size);
        Page<ProductResponse> products = sellerProductService.getAllAuthorOrCompanyProducts(page, size);
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Get paginated products", description = "Retrieves paginated products for the authenticated seller or company")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Company not found")
    })
    @GetMapping("/all-company-author-products")
    public ResponseEntity<Page<ProductResponse>> getAuthorOrCompanyProducts(
            @RequestParam(value = "companyId", required = false) Long companyId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        log.info("Fetching products for companyId: {}, page: {}, size: {}", companyId, page, size);
        Page<ProductResponse> products = sellerProductService.getAuthorOrCompanyProducts(companyId, page, size);
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Merge products to company", description = "Merges seller's products to a company")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Products merged successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Company or user not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @PostMapping("/merge-to-company/{companyId}")
    @RateLimiter(name = "mergeProductsToCompany")
    public ResponseEntity<Void> mergeProductsToCompany(
            @PathVariable Long companyId,
            @RequestParam("authorEmail") String authorEmail,
            Authentication authentication) {
        log.info("Merging products to company {} for seller {} by admin {}", companyId, authorEmail, authentication.getName());
        sellerProductService.mergeProductsToCompany(companyId, authorEmail, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}

/*
import com.datasaz.ecommerce.models.request.ProductRequest;
import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.services.interfaces.ISellerProductService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/seller/v1/products")
@RequiredArgsConstructor
@Tag(name = "Seller Product Controller", description = "APIs for managing products by sellers")
public class SellerProductController {

    private final ISellerProductService sellerProductService;

    @Operation(summary = "Create a new product", description = "Creates a new product with optional images")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @RateLimiter(name = "manageProduct")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestPart("product") ProductRequest productRequest,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            Authentication authentication) {
        log.info("Creating product for user: {}", authentication.getName());
        ProductResponse response = sellerProductService.createProduct(productRequest, images, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Update a product", description = "Updates an existing product with optional new images")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @PutMapping(value = "/{productId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @RateLimiter(name = "manageProduct")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestPart("product") ProductRequest productRequest,
            @RequestPart(value = "images", required = false) List<MultipartFile> newImages,
            @RequestParam(value = "imagesToRemove", required = false) List<Long> imagesToRemove,
            @RequestParam(value = "primaryImageId", required = false) Long primaryImageId,
            Authentication authentication) {
        log.info("Updating product {} for user: {}", productId, authentication.getName());
        ProductResponse response = sellerProductService.updateProduct(productId, productRequest, newImages, imagesToRemove, primaryImageId, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update product images", description = "Updates images for an existing product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Images updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @PatchMapping(value = "/{productId}/images", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @RateLimiter(name = "updateProductImages")
    public ResponseEntity<ProductResponse> updateProductImages(
            @PathVariable Long productId,
            @RequestPart(value = "images", required = false) List<MultipartFile> newImages,
            @RequestParam(value = "imagesToRemove", required = false) List<Long> imagesToRemove,
            @RequestParam(value = "primaryImageId", required = false) Long primaryImageId,
            Authentication authentication) {
        log.info("Updating images for product {} by user: {}", productId, authentication.getName());
        ProductResponse response = sellerProductService.updateProductImages(productId, newImages, imagesToRemove, primaryImageId, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete a product", description = "Soft deletes a product by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @DeleteMapping("/{productId}")
    @RateLimiter(name = "manageProduct")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long productId,
            Authentication authentication) {
        log.info("Deleting product {} for user: {}", productId, authentication.getName());
        sellerProductService.deleteProduct(productId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search products by name", description = "Searches products by name for the authenticated seller")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> findProductsByName(
            @RequestParam("name") String productName,
            Authentication authentication) {
        log.info("Searching products by name: {} for user: {}", productName, authentication.getName());
        List<ProductResponse> products = sellerProductService.findProductsByNameAuthorIdDeletedFalse(productName);
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Get paginated products", description = "Retrieves paginated products for the authenticated seller or company")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Company not found")
    })
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAuthorOrCompanyProducts(
            @RequestParam(value = "companyId", required = false) Long companyId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        log.info("Fetching products for companyId: {}, page: {}, size: {}", companyId, page, size);
        Page<ProductResponse> products = sellerProductService.getAuthorOrCompanyProducts(companyId, page, size);
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Merge products to company", description = "Merges seller's products to a company")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Products merged successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Company or user not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @PostMapping("/merge-to-company/{companyId}")
    @RateLimiter(name = "mergeProductsToCompany")
    public ResponseEntity<Void> mergeProductsToCompany(
            @PathVariable Long companyId,
            @RequestParam("authorEmail") String authorEmail,
            Authentication authentication) {
        log.info("Merging products to company {} for seller {} by admin {}", companyId, authorEmail, authentication.getName());
        sellerProductService.mergeProductsToCompany(companyId, authorEmail, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
*/

/*
import com.datasaz.ecommerce.models.request.ProductRequest;
import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.repositories.entities.ProductImage;
import com.datasaz.ecommerce.services.interfaces.IProductImageService;
import com.datasaz.ecommerce.services.interfaces.ISellerProductService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/seller/product")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('Role_SELLER')")
public class SellerProductController {

    private final ISellerProductService sellerProductService;
    private final IProductImageService productImageService;

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchByNameCurrentAuthorId(@RequestParam String productName) {
        log.info("searchByNameCurrentAuthorId: Searching products with name: {}", productName);
        return ResponseEntity.ok(sellerProductService.findProductsByNameAuthorIdDeletedFalse(productName));
    }

    @GetMapping("/current-user/searchpageable")
    public ResponseEntity<Page<ProductResponse>> searchProductsByNameCurrentAuthorId(@RequestParam String productName, Pageable pageable) {
        log.info("searchProductsByNameCurrentAuthorId: Searching products with name: {}, page: {}", productName, pageable);
        return ResponseEntity.ok(sellerProductService.findProductsByNameAuthorIdDeletedFalse(productName, pageable));
    }

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimiter(name = "manageProduct")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ProductResponse> createProduct(
            @RequestPart(value = "product", required = true) ProductRequest productRequest,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            Authentication authentication) {
        String email = authentication.getName();
        log.info("createProduct: Creating product for user: {}", email);
        ProductResponse response = sellerProductService.createProduct(productRequest, images, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimiter(name = "manageProduct")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long productId,
            @RequestPart(value = "product", required = true) ProductRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> newImages,
            @RequestParam(value = "imagesToRemove", required = false) List<Long> imagesToRemove,
            @RequestParam(value = "primaryImageId", required = false) Long primaryImageId,
            Authentication authentication) {
        String email = authentication.getName();
        log.info("updateProduct: Updating product {} for user: {}", productId, email);
        ProductResponse response = sellerProductService.updateProduct(productId, request, newImages, imagesToRemove, primaryImageId, email);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{productId}")
    @RateLimiter(name = "manageProduct")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long productId,
            Authentication authentication) {
        String email = authentication.getName();
        log.info("deleteProduct: Deleting product {} for user: {}", productId, email);
        sellerProductService.deleteProduct(productId, email);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/list")
    public ResponseEntity<Page<ProductResponse>> getAuthorOrCompanyProducts(
            @RequestParam(value = "companyId", required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("getAuthorOrCompanyProducts: Fetching products for companyId: {}, page: {}, size: {}", companyId, page, size);
        Page<ProductResponse> products = sellerProductService.getAuthorOrCompanyProducts(companyId, page, size);
        return ResponseEntity.ok(products);
    }

    @PostMapping("/merge/{companyId}/{sellerEmail}")
    @RateLimiter(name = "mergeProductsToCompany")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> mergeProductsToCompany(
            @PathVariable Long companyId,
            @PathVariable String sellerEmail,
            Authentication authentication) {
        String adminEmail = authentication.getName();
        log.info("mergeProductsToCompany: Merging products for seller {} to company {} by admin {}", sellerEmail, companyId, adminEmail);
        sellerProductService.mergeProductsToCompany(companyId, sellerEmail, adminEmail);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimiter(name = "updateProductImages")
    public ResponseEntity<ProductResponse> updateProductImages(
            @PathVariable Long productId,
            @RequestPart(value = "images", required = false) List<MultipartFile> newImages,
            @RequestParam(value = "imagesToRemove", required = false) List<Long> imagesToRemove,
            @RequestParam(value = "primaryImageId", required = false) Long primaryImageId,
            Authentication authentication) {
        String email = authentication.getName();
        log.info("updateProductImages: Updating images for product {} by user: {}", productId, email);
        ProductResponse response = sellerProductService.updateProductImages(productId, newImages, imagesToRemove, primaryImageId, email);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/image/{imageId}")
    @RateLimiter(name = "updateProductImages")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteImage(@PathVariable Long imageId) {
        log.info("deleteImage: Deleting image ID: {}", imageId);
        productImageService.deleteImageById(imageId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{productId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimiter(name = "updateProductImages")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ProductImage> addImage(
            @PathVariable Long productId,
            @RequestPart("image") MultipartFile image,
            Authentication authentication) {
        String email = authentication.getName();
        log.info("addImage: Adding image to product {} by user: {}", productId, email);
        ProductImage uploadedImage = productImageService.uploadImage(productId, image, false);
        return ResponseEntity.status(HttpStatus.CREATED).body(uploadedImage);
    }

}
*/

//    @PutMapping(value = "/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
/// /    public ResponseEntity<ProductResponse> updateProduct(
/// /            @PathVariable Long id,
/// /            @RequestPart("product") ProductRequest productRequest,
/// /            @RequestPart(value = "images", required = false) List<MultipartFile> imageFiles) {
/// /        ProductResponse updatedProduct = productService.updateProduct(id, productRequest, imageFiles);
/// /
/// /        return ResponseEntity.ok(updatedProduct);
/// /    }
//
/// /    @DeleteMapping("/{id}")
/// /    @ResponseStatus(HttpStatus.NO_CONTENT)
/// /    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
/// /        productService.deleteProduct(id);
/// /        return ResponseEntity.noContent().build();
/// /    }
//
//    @PutMapping("/{id}/quantity")
//    public ResponseEntity<ProductResponse> updateProductQuantity(@PathVariable Long id, @RequestBody int quantity) {
//        return ResponseEntity.ok(sellerProductService.updateProductQuantity(id, quantity));
//    }
//
//    @PutMapping("/{id}/price")
//    public ResponseEntity<ProductResponse> updateProductPrice(@PathVariable Long id, @RequestBody BigDecimal price) {
//        return ResponseEntity.ok(sellerProductService.updateProductPrice(id, price));
//    }
//
//
//    @DeleteMapping("/image/{imageId}")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    public ResponseEntity<Void> deleteImage(@PathVariable Long imageId) {
//        productImageService.deleteImageById(imageId);
//        return ResponseEntity.noContent().build();
//    }
//
//    /*
//    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
//    @ResponseStatus(HttpStatus.CREATED)
//    public ResponseEntity<ProductResponse> addProduct(
//            @RequestPart("product") ProductRequest productRequest,
//            @RequestPart(value = "images", required = false) List<MultipartFile> imageFiles) {
//        ProductResponse savedProduct = sellerProductService.saveProduct(productRequest, imageFiles);
//
//        return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
//    }*/
//
//    @PostMapping("/create")
//    @RateLimiter(name = "productCreate")
//    public ResponseEntity<ProductResponse> createProduct(
//            @RequestBody ProductRequest productRequest,
//            @RequestParam(value = "productImageRequests", required = false) List<ProductImageRequest> productImageRequests,
//            Authentication authentication) {
//        String email = authentication.getName();
//        ProductResponse response = sellerProductService.createProduct(productRequest, productImageRequests, email);
//        return ResponseEntity.ok(response);
//    }
//
//    @PutMapping("/{productId}")
//    @RateLimiter(name = "productUpdate")
//    public ResponseEntity<ProductResponse> updateProduct(
//            @PathVariable Long productId,
//            @RequestBody ProductRequest request,
//            @RequestParam(value = "productImageRequests", required = false) List<ProductImageRequest> newImages,
//            @RequestParam(value = "imagesToRemove", required = false) List<Long> imagesToRemove,
//            @RequestParam(value = "primaryImageId", required = false) Long primaryImageId,
//            Authentication authentication) {
//        String email = authentication.getName();
//        ProductResponse response = sellerProductService.updateProduct(productId, request, newImages, imagesToRemove, primaryImageId, email);
//        return ResponseEntity.ok(response);
//    }
//
//    @DeleteMapping("/{productId}")
//    @RateLimiter(name = "productDelete")
//    public ResponseEntity<Void> deleteProduct(
//            @PathVariable Long productId,
//            Authentication authentication) {
//        String email = authentication.getName();
//        sellerProductService.deleteProduct(productId, email);
//        return ResponseEntity.noContent().build();
//    }
//
//    @GetMapping("/list")
//    public ResponseEntity<Page<ProductResponse>> getAuthorOrCompanyProducts(
//            @RequestParam(value = "companyId", required = false) Long companyId,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//        Page<ProductResponse> products = sellerProductService.getAuthorOrCompanyProducts(companyId, page, size);
//        return ResponseEntity.ok(products);
//    }
//
//    @PostMapping("/merge/{companyId}/{sellerEmail}")
//    @RateLimiter(name = "mergeProductsToCompany")
//    public ResponseEntity<Void> mergeProductsToCompany(
//            @PathVariable Long companyId,
//            @PathVariable String sellerEmail,
//            Authentication authentication) {
//        String adminEmail = authentication.getName();
//        sellerProductService.mergeProductsToCompany(companyId, sellerEmail, adminEmail);
//        return ResponseEntity.noContent().build();
//    }
//
//    @PutMapping("/{productId}/images")
//    @RateLimiter(name = "updateProductImages")
//    public ResponseEntity<ProductResponse> updateProductImages(
//            @PathVariable Long productId,
//            @RequestParam(value = "images", required = false) List<ProductImageRequest> newImages,
//            @RequestParam(value = "imagesToRemove", required = false) List<Long> imagesToRemove,
//            @RequestParam(value = "primaryImageId", required = false) Long primaryImageId,
//            Authentication authentication) {
//        String email = authentication.getName();
//        ProductResponse response = sellerProductService.updateProductImages(productId, newImages, imagesToRemove, primaryImageId, email);
//        return ResponseEntity.ok(response);
//    }

/*
@RequiredArgsConstructor
@RestController
@RequestMapping("/seller/product")
@PreAuthorize("hasRole('SELLER')")
public class SellerProductController {

    private final IProductService productService;
    private final ICategoryService categoryService;
    private final IProductImageService productImageService;

//    // QA UNIT: Tested with Postman and it works fine
//    @GetMapping("/all")
//    public List<ProductResponse> getAllProducts() {
//
//        return productService.findAllProducts();
//    }
//
//    @GetMapping
//    public ResponseEntity<Page<ProductResponse>> getAll(Pageable pageable) {
//        return ResponseEntity.ok(productService.findAllProducts(pageable));
//    }
//
//    @GetMapping("/search")
//    public ResponseEntity<List<ProductResponse>> searchByName(@RequestParam String name) {
//        return ResponseEntity.ok(productService.findProductsByName(name));
//    }
//
//    @GetMapping("/searchpageable")
//    public Page<ProductResponse> searchProductsByName(@RequestParam String name, Pageable pageable) {
//        return productService.findProductsByName(name, pageable);
//    }
//
//    // QA UNIT: Tested with Postman and it works fine
//    @GetMapping("/{id}")
//    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
//        return ResponseEntity.ok(productService.findProductById(id));
//    }
//
//    @GetMapping("/getbysoldquantity")
//    public Page<ProductResponse> searchProductsBySoldQuantity(Pageable pageable) {
//        return productService.findProductBySoldQuantity(pageable);
//    }

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ProductResponse> addProduct(
            @RequestPart("product") ProductRequest productRequest,
            @RequestPart(value = "images", required = false) List<MultipartFile> imageFiles) {
        ProductResponse savedProduct = productService.saveProduct(productRequest);
        if (imageFiles != null && !imageFiles.isEmpty()) {
            for (int i = 0; i < imageFiles.size(); i++) {
                productImageService.uploadImage(savedProduct.getId(), imageFiles.get(i), i == 0);
            }
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
    }

    // QA UNIT: Tested with Postman and it works fine
//    @PostMapping
//    @ResponseStatus(HttpStatus.CREATED)
//    public ResponseEntity<ProductResponse> addProduct(@RequestBody ProductRequest productRequest) {
//        return ResponseEntity.ok(productService.saveProduct(productRequest));
//    }
//

    @PutMapping(value = "/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @RequestPart("product") ProductRequest productRequest,
            @RequestPart(value = "images", required = false) List<MultipartFile> imageFiles) {
        ProductResponse updatedProduct = productService.updateProduct(id, productRequest);
        if (imageFiles != null && !imageFiles.isEmpty()) {
            for (int i = 0; i < imageFiles.size(); i++) {
                productImageService.uploadImage(id, imageFiles.get(i), i == 0);
            }
        }
        return ResponseEntity.ok(updatedProduct);
    }

    // QA UNIT: Tested with Postman and it works fine
//    @PutMapping("/{id}")
//    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id, @RequestBody ProductRequest productRequest) {
//        return ResponseEntity.ok(productService.updateProduct(id, productRequest));
//    }


    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // QA UNIT: Tested with Postman and it works fine
//    @DeleteMapping("/{id}")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
//        productService.deleteProduct(id);
//        return ResponseEntity.noContent().build();
//    }

    // QA UNIT: Tested with Postman and it works fine
    @PutMapping("/{id}/quantity")
    public ProductResponse updateProductQuantity(@PathVariable Long id, @RequestBody int quantity) {
        return productService.updateProductQuantity(id, quantity);
    }

    // QA UNIT: Tested with Postman and it works fine
    @PutMapping("/{id}/price")
    public ProductResponse updateProductPrice(@PathVariable Long id, @RequestBody BigDecimal price) {
        return productService.updateProductPrice(id, price);
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