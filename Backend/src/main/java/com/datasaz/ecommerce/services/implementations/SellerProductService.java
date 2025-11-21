package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.configs.GroupConfig;
import com.datasaz.ecommerce.exceptions.*;
import com.datasaz.ecommerce.exceptions.response.ExceptionMessages;
import com.datasaz.ecommerce.mappers.ProductMapper;
import com.datasaz.ecommerce.models.request.ProductRequest;
import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.repositories.*;
import com.datasaz.ecommerce.repositories.entities.*;
import com.datasaz.ecommerce.services.interfaces.ISellerProductService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
@Slf4j
public class SellerProductService implements ISellerProductService {

    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;
    private final ProductImageService productImageService;
    private final AuditLogService auditLogService;
    private final CompanyAdminRightsRepository companyAdminRightsRepository;
    private final GroupConfig groupConfig;

    @Override
    @Transactional
    @RateLimiter(name = "manageProduct", fallbackMethod = "fallbackCreateProduct")
    @CacheEvict(value = {"products", "productsByCategory", "allProducts", "productImages"}, allEntries = true)
    public ProductResponse createProduct(ProductRequest productRequest, List<MultipartFile> images, String email) {
        log.info("createProduct: Creating product for user: {}", email);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User not found with email: " + email).build());

        if (!hasRole(user, RoleTypes.SELLER)) {
            log.error("User {} is not authorized to create product", email);
            throw UnauthorizedException.builder()
                    .message("User is not authorized to create product").build();
        }

        if (images != null && images.size() > groupConfig.maxFileCountPerProduct) {
            log.error("Too many images provided: {}, max allowed: {}", images.size(), groupConfig.maxFileCountPerProduct);
            throw BadRequestException.builder()
                    .message("Maximum " + groupConfig.maxFileCountPerProduct + " images allowed per product").build();
        }

        Product product = productMapper.toEntity(productRequest, user);
        product.setCompany(user.getCompany());
        product.setAuthor(user);
        product.setCreatedAt(LocalDateTime.now());
        product.setDeleted(false);

        Product savedProduct = productRepository.save(product);

        if (images != null && !images.isEmpty()) {
            if (groupConfig.imageStorageMode.equals("database")) {
                List<ProductImageAttach> productImages = new ArrayList<>();
                for (int i = 0; i < images.size(); i++) {
                    ProductImageAttach image = productImageService.uploadImageAttach(savedProduct.getId(), images.get(i), i == 0);
                    productImages.add(image);
                }
                product.setImageAttaches(productImages);
                productImages.forEach(image -> image.setProduct(savedProduct));
            } else {
                List<ProductImage> productImages = new ArrayList<>();
                for (int i = 0; i < images.size(); i++) {
                    ProductImage image = productImageService.uploadImage(savedProduct.getId(), images.get(i), i == 0);
                    productImages.add(image);
                }
                product.setImages(productImages);
                productImages.forEach(image -> image.setProduct(savedProduct));
            }
        }

        productRepository.save(savedProduct);
        log.info("Product created: {} by user {}, Images: {}", savedProduct.getName(), email, images != null ? images.size() : 0);

        auditLogService.logAction(email, "CREATE_PRODUCT", "Product: " + savedProduct.getName() + ", Images: " + (images != null ? images.size() : 0));

        return productMapper.toResponse(savedProduct);
    }

    private ProductResponse fallbackCreateProduct(ProductRequest request, List<MultipartFile> images, String email, Throwable t) {
        log.error("Fallback triggered for createProduct: {}", t.getMessage(), t);
        throw new RuntimeException("Failed to create product: " + t.getMessage(), t);
    }

    @Override
    @Transactional
    @RateLimiter(name = "manageProduct", fallbackMethod = "fallbackUpdateProduct")
    @CacheEvict(value = {"products", "productsByCategory", "allProducts", "productImages"}, allEntries = true)
    public ProductResponse updateProduct(Long productId, ProductRequest request, List<MultipartFile> newImages, List<Long> imagesToRemove, Long primaryImageId, String email) {
        log.info("updateProduct: Updating product {} by user {}", productId, email);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User not found with email: " + email).build());
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> ProductNotFoundException.builder()
                        .message("Product not found with id: " + productId).build());

        boolean isCompanyAdmin = hasRole(user, RoleTypes.COMPANY_ADMIN_SELLER) && user.getCompany() != null && product.getCompany() != null && user.getCompany().getId().equals(product.getCompany().getId());
        boolean isIndividualSeller = product.getAuthor() != null && product.getAuthor().getId().equals(user.getId()) && product.getCompany() == null;

        if (!isCompanyAdmin && !isIndividualSeller) {
            log.error("User {} is not authorized to update product {}", email, productId);
            throw UnauthorizedException.builder().message("User is not authorized to update this product").build();
        }

        int currentImageCount = groupConfig.imageStorageMode.equals("database") ? product.getImageAttaches().size() : product.getImages().size();
        if (newImages != null && (currentImageCount - (imagesToRemove != null ? imagesToRemove.size() : 0) + newImages.size()) > groupConfig.maxFileCountPerProduct) {
            log.error("Too many images for product {}: current {}, to remove {}, to add {}, max allowed: {}",
                    productId, currentImageCount, imagesToRemove != null ? imagesToRemove.size() : 0, newImages != null ? newImages.size() : 0, groupConfig.maxFileCountPerProduct);
            throw BadRequestException.builder()
                    .message("Maximum " + groupConfig.maxFileCountPerProduct + " images allowed per product").build();
        }

        if (request.getName() != null) product.setName(request.getName());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getOfferPrice() != null) product.setOfferPrice(request.getOfferPrice());
        if (request.getQuantity() >= 0) {
            product.setQuantity(request.getQuantity());
            product.setProductStatus(request.getQuantity() > 0 ? ProductStatus.AVAILABLE : ProductStatus.OUT_OF_STOCK);
        }
        if (request.getShippingCost() != null) product.setShippingCost(request.getShippingCost());
        if (request.getEachAdditionalItemShippingCost() != null)
            product.setEachAdditionalItemShippingCost(request.getEachAdditionalItemShippingCost());
        if (request.getInventoryLocation() != null) product.setInventoryLocation(request.getInventoryLocation());
        if (request.getWarranty() != null) product.setWarranty(request.getWarranty());
        if (request.getBrand() != null) product.setBrand(request.getBrand());
        if (request.getProductCode() != null) product.setProductCode(request.getProductCode());
        if (request.getManufacturingPieceNumber() != null)
            product.setManufacturingPieceNumber(request.getManufacturingPieceNumber());
        if (request.getManufacturingDate() != null) product.setManufacturingDate(request.getManufacturingDate());
        if (request.getEAN() != null) product.setEAN(request.getEAN());
        if (request.getManufacturingPlace() != null) product.setManufacturingPlace(request.getManufacturingPlace());
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> BadRequestException.builder().message("Category not found with ID: " + request.getCategoryId()).build());
            product.setCategory(category);
        }
        if (request.getVariants() != null) {
            product.getVariants().clear();
            List<ProductVariant> variants = request.getVariants().stream()
                    .map(var -> ProductVariant.builder()
                            .name(var.getName())
                            .priceAdjustment(var.getPriceAdjustment())
                            .quantity(var.getQuantity())
                            .product(product)
                            .build())
                    .collect(Collectors.toList());
            product.setVariants(variants);
        }

        if (groupConfig.imageStorageMode.equals("database")) {
            List<ProductImageAttach> existingImages = product.getImageAttaches();
            if (imagesToRemove != null && !imagesToRemove.isEmpty()) {
                log.info("Removing images with IDs {} for product {}", imagesToRemove, productId);
                existingImages.removeIf(image -> imagesToRemove.contains(image.getId()));
            }

            List<ProductImageAttach> newProductImages = new ArrayList<>();
            if (newImages != null && !newImages.isEmpty()) {
                for (int i = 0; i < newImages.size(); i++) {
                    ProductImageAttach image = productImageService.uploadImageAttach(productId, newImages.get(i), i == 0 && primaryImageId == null);
                    newProductImages.add(image);
                }
            }
            newProductImages.forEach(img -> img.setProduct(product));
            existingImages.addAll(newProductImages);
            updatePrimaryImageAttach(product, primaryImageId, !newProductImages.isEmpty());
        } else {
            List<ProductImage> existingImages = product.getImages();
            if (imagesToRemove != null && !imagesToRemove.isEmpty()) {
                log.info("Removing images with IDs {} for product {}", imagesToRemove, productId);
                existingImages.removeIf(image -> imagesToRemove.contains(image.getId()));
                // ensure if images are deleted from the file-system as well.
            }

            List<ProductImage> newProductImages = new ArrayList<>();
            if (newImages != null && !newImages.isEmpty()) {
                for (int i = 0; i < newImages.size(); i++) {
                    ProductImage image = productImageService.uploadImage(productId, newImages.get(i), i == 0 && primaryImageId == null);
                    newProductImages.add(image);
                }
            }
            newProductImages.forEach(img -> img.setProduct(product));
            existingImages.addAll(newProductImages);
            updatePrimaryImage(product, primaryImageId, !newProductImages.isEmpty());
        }

        product.setUpdatedAt(LocalDateTime.now());
        product.setDeleted(false);
        productRepository.save(product);
        log.info("Product updated: {} by user {}, Images: {}", product.getName(), email, groupConfig.imageStorageMode.equals("database") ? product.getImageAttaches().size() : product.getImages().size());

        auditLogService.logAction(email, "UPDATE_PRODUCT", "Product ID: " + productId + ", Images updated: " + (newImages != null ? newImages.size() : 0) + ", Images removed: " + (imagesToRemove != null ? imagesToRemove.size() : 0));

        return productMapper.toResponse(product);
    }

    /*@Override
    @Transactional
    @RateLimiter(name = "manageProduct", fallbackMethod = "fallbackUpdateProduct")
    @CacheEvict(value = {"products", "productsByCategory", "allProducts", "productImages"}, allEntries = true)
    public ProductResponse updateProduct(Long productId, ProductRequest request, List<MultipartFile> newImages, List<Long> imagesToRemove, Long primaryImageId, String email) {
        log.info("updateProduct: Updating product {} by user {}", productId, email);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User not found with email: " + email).build());
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> ProductNotFoundException.builder()
                        .message("Product not found with id: " + productId).build());

        boolean isCompanyAdmin = hasRole(user, RoleTypes.COMPANY_ADMIN_SELLER) && user.getCompany() != null && product.getCompany() != null && user.getCompany().getId().equals(product.getCompany().getId());
        boolean isIndividualSeller = product.getAuthor() != null && product.getAuthor().getId().equals(user.getId()) && product.getCompany() == null;

        if (!isCompanyAdmin && !isIndividualSeller) {
            log.error("User {} is not authorized to update product {}", email, productId);
            throw UnauthorizedException.builder().message("User is not authorized to update this product").build();
        }

        int currentImageCount = groupConfig.imageStorageMode.equals("database") ? product.getImageAttaches().size() : product.getImages().size();
        if (newImages != null && (currentImageCount - (imagesToRemove != null ? imagesToRemove.size() : 0) + newImages.size()) > groupConfig.maxFileCountPerProduct) {
            log.error("Too many images for product {}: current {}, to remove {}, to add {}, max allowed: {}",
                    productId, currentImageCount, imagesToRemove != null ? imagesToRemove.size() : 0, newImages != null ? newImages.size() : 0, groupConfig.maxFileCountPerProduct);
            throw BadRequestException.builder()
                    .message("Maximum " + groupConfig.maxFileCountPerProduct + " images allowed per product").build();
        }

        if (request.getName() != null) product.setName(request.getName());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getOfferPrice() != null) product.setOfferPrice(request.getOfferPrice());
        if (request.getQuantity() >= 0) {
            product.setQuantity(request.getQuantity());
            product.setProductStatus(request.getQuantity() > 0 ? ProductStatus.AVAILABLE : ProductStatus.OUT_OF_STOCK);
        }
        if (request.getInventoryLocation() != null) product.setInventoryLocation(request.getInventoryLocation());
        if (request.getWarranty() != null) product.setWarranty(request.getWarranty());
        if (request.getBrand() != null) product.setBrand(request.getBrand());
        if (request.getProductCode() != null) product.setProductCode(request.getProductCode());
        if (request.getManufacturingPieceNumber() != null)
            product.setManufacturingPieceNumber(request.getManufacturingPieceNumber());
        if (request.getManufacturingDate() != null) product.setManufacturingDate(request.getManufacturingDate());
        if (request.getEAN() != null) product.setEAN(request.getEAN());
        if (request.getManufacturingPlace() != null) product.setManufacturingPlace(request.getManufacturingPlace());
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> BadRequestException.builder().message("Category not found with ID: " + request.getCategoryId()).build());
            product.setCategory(category);
        }
        if (request.getVariants() != null) {
            product.getVariants().clear();
            List<ProductVariant> variants = request.getVariants().stream()
                    .map(var -> ProductVariant.builder()
                            .name(var.getName())
                            .priceAdjustment(var.getPriceAdjustment())
                            .quantity(var.getQuantity())
                            .product(product)
                            .build())
                    .collect(Collectors.toList());
            product.setVariants(variants);
        }

        if (groupConfig.imageStorageMode.equals("database")) {
            List<ProductImageAttach> existingImages = product.getImageAttaches();
            if (imagesToRemove != null && !imagesToRemove.isEmpty()) {
            // Remove images from the product's collection and delete from database
       // existingImages.removeIf(image -> imagesToRemove.contains(image.getId()));
                imagesToRemove.forEach(imageId -> productImageService.deleteImageAttachById(imageId));
            }

            List<ProductImageAttach> newProductImages = new ArrayList<>();
            if (newImages != null && !newImages.isEmpty()) {
                for (int i = 0; i < newImages.size(); i++) {
                    ProductImageAttach image = productImageService.uploadImageAttach(productId, newImages.get(i), i == 0 && primaryImageId == null);
                    newProductImages.add(image);
                }
            }
            newProductImages.forEach(img -> img.setProduct(product));
            existingImages.addAll(newProductImages);
            updatePrimaryImageAttach(product, primaryImageId, !newProductImages.isEmpty());
        } else {
            List<ProductImage> existingImages = product.getImages();
            if (imagesToRemove != null && !imagesToRemove.isEmpty()) {
                imagesToRemove.forEach(imageId -> productImageService.deleteImageById(imageId));
            }

            List<ProductImage> newProductImages = new ArrayList<>();
            if (newImages != null && !newImages.isEmpty()) {
                for (int i = 0; i < newImages.size(); i++) {
                    ProductImage image = productImageService.uploadImage(productId, newImages.get(i), i == 0 && primaryImageId == null);
                    newProductImages.add(image);
                }
            }
            newProductImages.forEach(img -> img.setProduct(product));
            existingImages.addAll(newProductImages);
            updatePrimaryImage(product, primaryImageId, !newProductImages.isEmpty());
        }

        product.setUpdatedAt(LocalDateTime.now());
        product.setDeleted(false);
        productRepository.save(product);
        log.info("Product updated: {} by user {}, Images: {}", product.getName(), email, groupConfig.imageStorageMode.equals("database") ? product.getImageAttaches().size() : product.getImages().size());

        auditLogService.logAction(email, "UPDATE_PRODUCT", "Product ID: " + productId + ", Images updated: " + (newImages != null ? newImages.size() : 0) + ", Images removed: " + (imagesToRemove != null ? imagesToRemove.size() : 0));

        return productMapper.toResponse(product);
    }*/

    private ProductResponse fallbackUpdateProduct(Long productId, ProductRequest request, List<MultipartFile> newImages, List<Long> imagesToRemove, Long primaryImageId, String email, Throwable t) {
        log.error("Fallback triggered for updateProduct: {}", t.getMessage(), t);
        throw new RuntimeException("Failed to update product: " + t.getMessage(), t);
    }

    @Override
    @Transactional
    @RateLimiter(name = "updateProductImages", fallbackMethod = "fallbackUpdateProductImages")
    @CacheEvict(value = {"products", "productsByCategory", "allProducts", "productImages"}, allEntries = true)
    public ProductResponse updateProductImages(Long productId, List<MultipartFile> newImages, List<Long> imagesToRemove, Long primaryImageId, String email) {
        log.info("updateProductImages: Updating images for product {} by user {}", productId, email);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User not found with email: " + email).build());
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> ProductNotFoundException.builder()
                        .message("Product not found with id: " + productId).build());

        boolean isCompanyAdmin = hasRole(user, RoleTypes.COMPANY_ADMIN_SELLER) && user.getCompany() != null && product.getCompany() != null && user.getCompany().getId().equals(product.getCompany().getId());
        boolean isIndividualSeller = product.getAuthor() != null && product.getAuthor().getId().equals(user.getId()) && product.getCompany() == null;

        if (!isCompanyAdmin && !isIndividualSeller) {
            log.error("User {} is not authorized to update product images {}", email, productId);
            throw UnauthorizedException.builder()
                    .message("User is not authorized to update this product's images").build();
        }

        int currentImageCount = groupConfig.imageStorageMode.equals("database") ? product.getImageAttaches().size() : product.getImages().size();
        if (newImages != null && (currentImageCount - (imagesToRemove != null ? imagesToRemove.size() : 0) + newImages.size()) > groupConfig.maxFileCountPerProduct) {
            log.error("Too many images for product {}: current {}, to remove {}, to add {}, max allowed: {}",
                    productId, currentImageCount, imagesToRemove != null ? imagesToRemove.size() : 0, newImages != null ? newImages.size() : 0, groupConfig.maxFileCountPerProduct);
            throw BadRequestException.builder()
                    .message("Maximum " + groupConfig.maxFileCountPerProduct + " images allowed per product").build();
        }

        if (groupConfig.imageStorageMode.equals("database")) {
            List<ProductImageAttach> existingImages = product.getImageAttaches();
            if (imagesToRemove != null && !imagesToRemove.isEmpty()) {
                imagesToRemove.forEach(imageId -> productImageService.deleteImageAttachById(imageId));
            }

            List<ProductImageAttach> newProductImages = new ArrayList<>();
            if (newImages != null && !newImages.isEmpty()) {
                for (int i = 0; i < newImages.size(); i++) {
                    ProductImageAttach image = productImageService.uploadImageAttach(productId, newImages.get(i), i == 0 && primaryImageId == null);
                    newProductImages.add(image);
                }
            }
            newProductImages.forEach(img -> img.setProduct(product));
            existingImages.addAll(newProductImages);
            updatePrimaryImageAttach(product, primaryImageId, !newProductImages.isEmpty());
        } else {
            List<ProductImage> existingImages = product.getImages();
            if (imagesToRemove != null && !imagesToRemove.isEmpty()) {
                imagesToRemove.forEach(imageId -> productImageService.deleteImageById(imageId));
            }

            List<ProductImage> newProductImages = new ArrayList<>();
            if (newImages != null && !newImages.isEmpty()) {
                for (int i = 0; i < newImages.size(); i++) {
                    ProductImage image = productImageService.uploadImage(productId, newImages.get(i), i == 0 && primaryImageId == null);
                    newProductImages.add(image);
                }
            }
            newProductImages.forEach(img -> img.setProduct(product));
            existingImages.addAll(newProductImages);
            updatePrimaryImage(product, primaryImageId, !newProductImages.isEmpty());
        }

        productRepository.save(product);
        log.info("Product images updated: {} by user {}, Images: {}", productId, email, groupConfig.imageStorageMode.equals("database") ? product.getImageAttaches().size() : product.getImages().size());

        auditLogService.logAction(email, "UPDATE_PRODUCT_IMAGES", "Product ID: " + productId + ", Images updated: " + (newImages != null ? newImages.size() : 0) + ", Images removed: " + (imagesToRemove != null ? imagesToRemove.size() : 0));

        return productMapper.toResponse(product);
    }

    private ProductResponse fallbackUpdateProductImages(Long productId, List<MultipartFile> newImages, List<Long> imagesToRemove, Long primaryImageId, String email, Throwable t) {
        log.error("Fallback triggered for updateProductImages: {}", t.getMessage(), t);
        throw new RuntimeException("Failed to update product images: " + t.getMessage(), t);
    }

    @Override
    @Transactional
    @RateLimiter(name = "manageProduct", fallbackMethod = "fallbackDeleteProduct")
    @CacheEvict(value = {"products", "productsByCategory", "allProducts", "productImages"}, allEntries = true)
    public void deleteProduct(Long productId, String email) {
        log.info("deleteProduct: Deleting product {} by user {}", productId, email);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User not found with email: " + email).build());
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> ProductNotFoundException.builder()
                        .message("Product not found with id: " + productId).build());

        boolean isCompanyAdmin = hasRole(user, RoleTypes.COMPANY_ADMIN_SELLER) && user.getCompany() != null && product.getCompany() != null && user.getCompany().getId().equals(product.getCompany().getId());
        boolean isIndividualSeller = product.getAuthor() != null && product.getAuthor().getId().equals(user.getId()) && product.getCompany() == null;

        if (!isCompanyAdmin && !isIndividualSeller) {
            log.error("User {} is not authorized to delete product {}", email, productId);
            throw UnauthorizedException.builder().message("User is not authorized to delete this product").build();
        }

        if (groupConfig.imageStorageMode.equals("database")) {
            productImageService.deleteImageAttachesByProductId(productId);
        } else {
            productImageService.deleteImagesByProductId(productId);
        }
        product.setDeleted(true);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
        log.info("Product deleted: {} by user {}", productId, email);

        auditLogService.logAction(email, "DELETE_PRODUCT", "Product ID: " + productId);
    }

    private void fallbackDeleteProduct(Long productId, String email, Throwable t) {
        log.error("Fallback triggered for deleteProduct: {}", t.getMessage(), t);
        throw new RuntimeException("Failed to delete product: " + t.getMessage(), t);
    }

    @Override
    public List<ProductResponse> findProductsByNameAuthorIdDeletedFalse(String productName) {
        log.info("findProductsByNameAuthorIdDeletedFalse: Searching for products with name: {}", productName);
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User author = userRepository.findByEmailAddress(email)
                .orElseThrow(() -> UnauthorizedException.builder()
                        .message("Authenticated user not found: " + email).build());

        List<Product> products = productRepository.findByNameContainingIgnoreCaseAuthorIdDeletedFalse(productName, author.getId());
        return products.stream().map(productMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public Page<ProductResponse> findProductsByNameAuthorIdDeletedFalse(String name, Pageable pageable) {
        log.info("findProductsByNameAuthorIdDeletedFalse: Searching for products with name: {}, page: {}", name, pageable);
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User author = userRepository.findByEmailAddress(email)
                .orElseThrow(() -> UnauthorizedException.builder()
                        .message("Authenticated user not found: " + email).build());

        Page<Product> productPage = productRepository.findByNameContainingIgnoreCaseAuthorIdDeletedFalse(name, author.getId(), pageable);
        return productPage.map(productMapper::toResponse);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "productsByCategory", "allProducts", "productImages"}, allEntries = true)
    public ProductResponse saveProduct(ProductRequest productRequest, List<MultipartFile> imageFiles) {
        log.info("saveProduct: Saving product: {}", productRequest.getName());
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User author = userRepository.findByEmailAddress(email)
                .orElseThrow(() -> UnauthorizedException.builder()
                        .message("Authenticated user not found: " + email).build());

        if (imageFiles != null && imageFiles.size() > groupConfig.maxFileCountPerProduct) {
            log.error("Too many images provided: {}, max allowed: {}", imageFiles.size(), groupConfig.maxFileCountPerProduct);
            throw BadRequestException.builder()
                    .message("Maximum " + groupConfig.maxFileCountPerProduct + " images allowed per product").build();
        }

        Product product = productMapper.toEntity(productRequest, author);
        product.setProductStatus(product.getQuantity() > 0 ? ProductStatus.AVAILABLE : ProductStatus.OUT_OF_STOCK);
        product.setCreatedAt(LocalDateTime.now());
        product.setDeleted(false);

        Product savedProduct = productRepository.save(product);

        if (imageFiles != null && !imageFiles.isEmpty()) {
            if (groupConfig.imageStorageMode.equals("database")) {
                List<ProductImageAttach> productImages = new ArrayList<>();
                for (int i = 0; i < imageFiles.size(); i++) {
                    ProductImageAttach image = productImageService.uploadImageAttach(savedProduct.getId(), imageFiles.get(i), i == 0);
                    productImages.add(image);
                }
                product.setImageAttaches(productImages);
                productImages.forEach(image -> image.setProduct(savedProduct));
            } else {
                List<ProductImage> productImages = new ArrayList<>();
                for (int i = 0; i < imageFiles.size(); i++) {
                    ProductImage image = productImageService.uploadImage(savedProduct.getId(), imageFiles.get(i), i == 0);
                    productImages.add(image);
                }
                product.setImages(productImages);
                productImages.forEach(image -> image.setProduct(savedProduct));
            }
        }

        productRepository.save(savedProduct);
        log.info("Product saved: {} by user {}, Images: {}", savedProduct.getName(), email, imageFiles != null ? imageFiles.size() : 0);

        auditLogService.logAction(email, "SAVE_PRODUCT", "Product: " + savedProduct.getName() + ", Images: " + (imageFiles != null ? imageFiles.size() : 0));

        return productMapper.toResponse(savedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "productsByCategory", "allProducts"}, allEntries = true)
    public ProductResponse updateProductQuantity(Long id, int quantity) {
        log.info("ProductService: updateQuantity {},{}", id, quantity);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found with id: {}", id);
                    return ProductNotFoundException.builder().message("Product not Found.").build();
                });
        product.setQuantity(quantity);
        product.setProductStatus(quantity > 0 ? ProductStatus.AVAILABLE : ProductStatus.OUT_OF_STOCK);

        Product updatedProduct = productRepository.save(product);
        return productMapper.toResponse(updatedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "productsByCategory", "allProducts"}, allEntries = true)
    public ProductResponse updateProductPrice(Long id, BigDecimal price) {
        log.info("ProductService: updatePrice {},{}", id, price);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found with id: {}", id);
                    return ProductNotFoundException.builder().message("Product not Found.").build();
                });
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            log.error("Invalid Price : {}", price);
            throw new IllegalArgumentException("Invalid Price");
        }
        product.setPrice(price);
        Product updatedProduct = productRepository.save(product);
        return productMapper.toResponse(updatedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllAuthorOrCompanyProducts(int page, int size) {
        log.debug("Fetching products for seller, page: {}, size: {}", page, size);

        // Validate pagination parameters
        if (page < 0) {
            log.error("Invalid page number: {}", page);
            throw IllegalParameterException.builder().message("Page number cannot be negative").build();
        }
        if (size < 1) {
            log.error("Invalid page size: {}. Must be at least 1", size);
            throw IllegalParameterException.builder().message(
                    String.format("Page size must be at least 1. %s", size)).build();
        }

        // Get authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            log.error("No authenticated user found");
            throw UnauthorizedException.builder().message("No authenticated user").build();
        }
        String email = authentication.getName();
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message(ExceptionMessages.USER_NOT_FOUND + "Email: " + email)
                        .build());

        // Check for SELLER role
        boolean isSeller = user.getUserRoles().stream()
                .anyMatch(role -> role.getRole() == RoleTypes.SELLER);
        if (!isSeller) {
            log.error("User {} lacks SELLER role", email);
            throw UnauthorizedException.builder().message("User is not a seller").build();
        }

        // Create pageable
        Pageable pageable = PageRequest.of(page, size);

        // Fetch products by company or user
        Page<Product> products;
        Company company = user.getCompany();
        if (company != null && !company.isDeleted()) {
            log.debug("Fetching products for company ID: {}", company.getId());
            products = productRepository.findByCompanyIdAndDeletedFalse(company.getId(), pageable);
            if (products.isEmpty()) {
                log.debug("No products found for company ID: {}", company.getId());
            }
        } else {
            log.debug("Fetching products for user ID: {}", user.getId());
            products = productRepository.findByAuthorIdAndDeletedFalse(user.getId(), pageable);
            if (products.isEmpty()) {
                log.debug("No products found for user ID: {}", user.getId());
            }
        }

        // Map to response DTO
        return products.map(productMapper::toResponse);
    }

    @Override
    public Page<ProductResponse> getAuthorOrCompanyProducts(Long companyId, int page, int size) {
        log.info("getAuthorOrCompanyProducts: Fetching products for companyId: {}, page: {}, size: {}", companyId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products;

        if (companyId == null) {
            User user = userRepository.findByEmailAddressAndDeletedFalse(SecurityContextHolder.getContext().getAuthentication().getName())
                    .orElseThrow(() -> UserNotFoundException.builder().message("User not found").build());
            if (!hasRole(user, RoleTypes.SELLER)) {
                log.error("User {} is not a seller", user.getEmailAddress());
                throw UnauthorizedException.builder().message("User is not a seller").build();
            }
            products = productRepository.findByAuthorIdAndDeletedFalse(user.getId(), pageable);
        } else {
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> ResourceNotFoundException.builder()
                            .message("Company not found with id: " + companyId).build());
            products = productRepository.findByCompanyIdAndDeletedFalse(companyId, pageable);
        }

        return products.map(productMapper::toResponse);
    }

    @Override
    @Transactional
    @RateLimiter(name = "mergeProductsToCompany", fallbackMethod = "fallbackMergeProductsToCompany")
    @CacheEvict(value = {"products", "productsByCategory", "allProducts", "productImages"}, allEntries = true)
    public void mergeProductsToCompany(Long companyId, String authorEmail, String companyAdminEmail) {
        log.info("mergeProductsToCompany: {} merging products for seller {} to company {}", companyAdminEmail, authorEmail, companyId);
        User companyAdmin = userRepository.findByEmailAddressAndDeletedFalse(companyAdminEmail)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("Admin not found with email: " + companyAdminEmail).build());
        User author = userRepository.findByEmailAddressAndDeletedFalse(authorEmail)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("Seller not found with email: " + authorEmail).build());
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> ResourceNotFoundException.builder()
                        .message("Company not found with id: " + companyId).build());

        if (!hasRole(companyAdmin, RoleTypes.COMPANY_ADMIN_SELLER)) {
            log.error("User {} lacks COMPANY_ADMIN_SELLER role", companyAdminEmail);
            throw UnauthorizedException.builder().message("User lacks COMPANY_ADMIN_SELLER role").build();
        }

        CompanyAdminRights companyAdminRights = companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, companyAdmin.getId())
                .orElseThrow(() -> UnauthorizedException.builder()
                        .message("Admin has no rights for company: " + companyId).build());
        if (!companyAdminRights.getCanAddRemoveSellers()) {
            log.error("Admin {} lacks permission to merge products", companyAdminEmail);
            throw UnauthorizedException.builder().message("Admin lacks permission to merge products").build();
        }

        if (!hasRole(author, RoleTypes.SELLER) || author.getCompany() == null || !author.getCompany().getId().equals(companyId)) {
            log.error("User {} is not a seller associated with company {}", authorEmail, companyId);
            throw BadRequestException.builder().message("User is not a seller associated with the company").build();
        }

        productRepository.updateCompanyForAuthorProducts(author.getId(), company);
        log.info("Merged products to company {}", companyId);

        auditLogService.logAction(authorEmail, "MERGE_PRODUCTS_TO_COMPANY", companyAdminEmail, "Company ID: " + companyId);
    }

    private void fallbackMergeProductsToCompany(Long companyId, String authorEmail, String companyAdminEmail, Throwable t) {
        log.error("Fallback triggered for mergeProductsToCompany: {}", t.getMessage(), t);
        throw new RuntimeException("Failed to merge products to company: " + t.getMessage(), t);
    }

    private void updatePrimaryImage(Product product, Long primaryImageId, boolean hasNewImages) {
        List<ProductImage> images = product.getImages();
        if (images == null || images.isEmpty()) {
            return;
        }

        images.forEach(img -> img.setPrimary(false));

        if (primaryImageId != null) {
            ProductImage primaryImage = images.stream()
                    .filter(img -> img.getId() != null && img.getId().equals(primaryImageId))
                    .findFirst()
                    .orElseThrow(() -> BadRequestException.builder().message("Primary image ID " + primaryImageId + " not found").build());
            primaryImage.setPrimary(true);
        } else if (hasNewImages) {
            images.stream()
                    .filter(img -> img.getId() == null)
                    .findFirst()
                    .ifPresent(img -> img.setPrimary(true));
        } else {
            images.stream()
                    .min((img1, img2) -> Integer.compare(img1.getDisplayOrder(), img2.getDisplayOrder()))
                    .ifPresent(img -> img.setPrimary(true));
        }
    }

    private void updatePrimaryImageAttach(Product product, Long primaryImageId, boolean hasNewImages) {
        List<ProductImageAttach> images = product.getImageAttaches();
        if (images == null || images.isEmpty()) {
            return;
        }

        images.forEach(img -> img.setPrimary(false));

        if (primaryImageId != null) {
            ProductImageAttach primaryImage = images.stream()
                    .filter(img -> img.getId() != null && img.getId().equals(primaryImageId))
                    .findFirst()
                    .orElseThrow(() -> BadRequestException.builder().message("Primary image ID " + primaryImageId + " not found").build());
            primaryImage.setPrimary(true);
        } else if (hasNewImages) {
            images.stream()
                    .filter(img -> img.getId() == null)
                    .findFirst()
                    .ifPresent(img -> img.setPrimary(true));
        } else {
            images.stream()
                    .min((img1, img2) -> Integer.compare(img1.getDisplayOrder(), img2.getDisplayOrder()))
                    .ifPresent(img -> img.setPrimary(true));
        }
    }

    private boolean hasRole(User user, RoleTypes roleType) {
        return user.getUserRoles().stream().anyMatch(role -> role.getRole() == roleType);
    }
}

/*
import com.datasaz.ecommerce.exceptions.*;
import com.datasaz.ecommerce.mappers.ProductMapper;
import com.datasaz.ecommerce.models.request.ProductRequest;
import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.repositories.*;
import com.datasaz.ecommerce.repositories.entities.*;
import com.datasaz.ecommerce.services.interfaces.ISellerProductService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
@Slf4j
public class SellerProductService implements ISellerProductService {

    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;
    private final ProductImageService productImageService;
    private final AuditLogService auditLogService;
    private final CompanyAdminRightsRepository companyAdminRightsRepository;

    @Override
    @Transactional
    @RateLimiter(name = "manageProduct", fallbackMethod = "fallbackCreateProduct")
    @CacheEvict(value = {"products", "productsByCategory", "allProducts", "productImages"}, allEntries = true)
    public ProductResponse createProduct(ProductRequest productRequest, List<MultipartFile> images, String email) {
        log.info("createProduct: Creating product for user: {}", email);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User not found with email: " + email).build());

        if (!hasRole(user, RoleTypes.SELLER)) {
            log.error("User {} is not authorized to create product", email);
            throw UnauthorizedException.builder()
                    .message("User is not authorized to create product").build();
        }

        Product product = productMapper.toEntity(productRequest, user);
        product.setCompany(user.getCompany());
        product.setAuthor(user);
        product.setCreatedAt(LocalDateTime.now());
        product.setDeleted(false);

        // Save product first to get ID
        Product savedProduct = productRepository.save(product);

        List<ProductImage> productImages = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            for (int i = 0; i < images.size(); i++) {
                ProductImage image = productImageService.uploadImage(savedProduct.getId(), images.get(i), i == 0);
                productImages.add(image);
            }
        }
        product.setImages(productImages);
        productImages.forEach(image -> image.setProduct(savedProduct));

        productRepository.save(savedProduct);
        log.info("Product created: {} by user {}, Images: {}", savedProduct.getName(), email, productImages.size());

        auditLogService.logAction(email, "CREATE_PRODUCT", "Product: " + savedProduct.getName() + ", Images: " + productImages.size());

        return productMapper.toResponse(savedProduct);
    }

    private ProductResponse fallbackCreateProduct(ProductRequest request, List<MultipartFile> images, String email, Throwable t) {
        log.error("Fallback triggered for createProduct: {}", t.getMessage(), t);
        throw new RuntimeException("Failed to create product: " + t.getMessage(), t);
    }

    @Override
    @Transactional
    @RateLimiter(name = "manageProduct", fallbackMethod = "fallbackUpdateProduct")
    @CacheEvict(value = {"products", "productsByCategory", "allProducts", "productImages"}, allEntries = true)
    public ProductResponse updateProduct(Long productId, ProductRequest request, List<MultipartFile> newImages, List<Long> imagesToRemove, Long primaryImageId, String email) {
        log.info("updateProduct: Updating product {} by user {}", productId, email);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User not found with email: " + email).build());
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> ProductNotFoundException.builder()
                        .message("Product not found with id: " + productId).build());

        boolean isCompanyAdmin = hasRole(user, RoleTypes.COMPANY_ADMIN_SELLER) && user.getCompany() != null && product.getCompany() != null && user.getCompany().getId().equals(product.getCompany().getId());
        boolean isIndividualSeller = product.getAuthor() != null && product.getAuthor().getId().equals(user.getId()) && product.getCompany() == null;

        if (!isCompanyAdmin && !isIndividualSeller) {
            log.error("User {} is not authorized to update product {}", email, productId);
            throw UnauthorizedException.builder().message("User is not authorized to update this product").build();
        }

        // Update product fields
        if (request.getName() != null) product.setName(request.getName());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getOfferPrice() != null) product.setOfferPrice(request.getOfferPrice());
        if (request.getQuantity() > 0) {
            product.setQuantity(request.getQuantity());
            product.setProductStatus(request.getQuantity() > 0 ? ProductStatus.AVAILABLE : ProductStatus.OUT_OF_STOCK);
        }
        if (request.getInventoryLocation() != null) product.setInventoryLocation(request.getInventoryLocation());
        if (request.getWarranty() != null) product.setWarranty(request.getWarranty());
        if (request.getBrand() != null) product.setBrand(request.getBrand());
        if (request.getProductCode() != null) product.setProductCode(request.getProductCode());
        if (request.getManufacturingPieceNumber() != null)
            product.setManufacturingPieceNumber(request.getManufacturingPieceNumber());
        if (request.getManufacturingDate() != null) product.setManufacturingDate(request.getManufacturingDate());
        if (request.getEAN() != null) product.setEAN(request.getEAN());
        if (request.getManufacturingPlace() != null) product.setManufacturingPlace(request.getManufacturingPlace());
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> BadRequestException.builder().message("Category not found with ID: " + request.getCategoryId()).build());
            product.setCategory(category);
        }
        if (request.getVariants() != null) {
            product.getVariants().clear();
            List<ProductVariant> variants = request.getVariants().stream()
                    .map(var -> ProductVariant.builder()
                            .name(var.getName())
                            .priceAdjustment(var.getPriceAdjustment())
                            .quantity(var.getQuantity())
                            .product(product)
                            .build())
                    .collect(Collectors.toList());
            product.setVariants(variants);
        }

        // Handle image updates
        List<ProductImage> existingImages = product.getImages();
        if (imagesToRemove != null && !imagesToRemove.isEmpty()) {
            imagesToRemove.forEach(imageId -> productImageService.deleteImageById(imageId));
        }

        List<ProductImage> newProductImages = new ArrayList<>();
        if (newImages != null && !newImages.isEmpty()) {
            for (int i = 0; i < newImages.size(); i++) {
                ProductImage image = productImageService.uploadImage(productId, newImages.get(i), i == 0 && primaryImageId == null);
                newProductImages.add(image);
            }
        }
        newProductImages.forEach(img -> img.setProduct(product));
        existingImages.addAll(newProductImages);

        // Update primary image
        updatePrimaryImage(product, primaryImageId, !newProductImages.isEmpty());

        product.setUpdatedAt(LocalDateTime.now());
        product.setDeleted(false);
        productRepository.save(product);
        log.info("Product updated: {} by user {}, Images: {}", product.getName(), email, existingImages.size());

        auditLogService.logAction(email, "UPDATE_PRODUCT", "Product ID: " + productId + ", Images updated: " + newProductImages.size() + ", Images removed: " + (imagesToRemove != null ? imagesToRemove.size() : 0));

        return productMapper.toResponse(product);
    }

    private ProductResponse fallbackUpdateProduct(Long productId, ProductRequest request, List<MultipartFile> newImages, List<Long> imagesToRemove, Long primaryImageId, String email, Throwable t) {
        log.error("Fallback triggered for updateProduct: {}", t.getMessage(), t);
        throw new RuntimeException("Failed to update product: " + t.getMessage(), t);
    }

    @Override
    @Transactional
    @RateLimiter(name = "updateProductImages", fallbackMethod = "fallbackUpdateProductImages")
    @CacheEvict(value = {"products", "productsByCategory", "allProducts", "productImages"}, allEntries = true)
    public ProductResponse updateProductImages(Long productId, List<MultipartFile> newImages, List<Long> imagesToRemove, Long primaryImageId, String email) {
        log.info("updateProductImages: Updating images for product {} by user {}", productId, email);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User not found with email: " + email).build());
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> ProductNotFoundException.builder()
                        .message("Product not found with id: " + productId).build());

        boolean isCompanyAdmin = hasRole(user, RoleTypes.COMPANY_ADMIN_SELLER) && user.getCompany() != null && product.getCompany() != null && user.getCompany().getId().equals(product.getCompany().getId());
        boolean isIndividualSeller = product.getAuthor() != null && product.getAuthor().getId().equals(user.getId()) && product.getCompany() == null;

        if (!isCompanyAdmin && !isIndividualSeller) {
            log.error("User {} is not authorized to update product images {}", email, productId);
            throw UnauthorizedException.builder()
                    .message("User is not authorized to update this product's images").build();
        }

        List<ProductImage> existingImages = product.getImages();
        if (imagesToRemove != null && !imagesToRemove.isEmpty()) {
            imagesToRemove.forEach(imageId -> productImageService.deleteImageById(imageId));
        }

        List<ProductImage> newProductImages = new ArrayList<>();
        if (newImages != null && !newImages.isEmpty()) {
            for (int i = 0; i < newImages.size(); i++) {
                ProductImage image = productImageService.uploadImage(productId, newImages.get(i), i == 0 && primaryImageId == null);
                newProductImages.add(image);
            }
        }
        newProductImages.forEach(img -> img.setProduct(product));
        existingImages.addAll(newProductImages);

        updatePrimaryImage(product, primaryImageId, !newProductImages.isEmpty());

        productRepository.save(product);
        log.info("Product images updated: {} by user {}, Images: {}", productId, email, existingImages.size());

        auditLogService.logAction(email, "UPDATE_PRODUCT_IMAGES", "Product ID: " + productId + ", Images updated: " + newProductImages.size() + ", Images removed: " + (imagesToRemove != null ? imagesToRemove.size() : 0));

        return productMapper.toResponse(product);
    }

    private ProductResponse fallbackUpdateProductImages(Long productId, List<MultipartFile> newImages, List<Long> imagesToRemove, Long primaryImageId, String email, Throwable t) {
        log.error("Fallback triggered for updateProductImages: {}", t.getMessage(), t);
        throw new RuntimeException("Failed to update product images: " + t.getMessage(), t);
    }

    @Override
    @Transactional
    @RateLimiter(name = "manageProduct", fallbackMethod = "fallbackDeleteProduct")
    @CacheEvict(value = {"products", "productsByCategory", "allProducts", "productImages"}, allEntries = true)
    public void deleteProduct(Long productId, String email) {
        log.info("deleteProduct: Deleting product {} by user {}", productId, email);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User not found with email: " + email).build());
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> ProductNotFoundException.builder()
                        .message("Product not found with id: " + productId).build());

        boolean isCompanyAdmin = hasRole(user, RoleTypes.COMPANY_ADMIN_SELLER) && user.getCompany() != null && product.getCompany() != null && user.getCompany().getId().equals(product.getCompany().getId());
        boolean isIndividualSeller = product.getAuthor() != null && product.getAuthor().getId().equals(user.getId()) && product.getCompany() == null;

        if (!isCompanyAdmin && !isIndividualSeller) {
            log.error("User {} is not authorized to delete product {}", email, productId);
            throw UnauthorizedException.builder().message("User is not authorized to delete this product").build();
        }

        productImageService.deleteImagesByProductId(productId);
        product.setDeleted(true);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
        log.info("Product deleted: {} by user {}", productId, email);

        auditLogService.logAction(email, "DELETE_PRODUCT", "Product ID: " + productId);
    }

    private void fallbackDeleteProduct(Long productId, String email, Throwable t) {
        log.error("Fallback triggered for deleteProduct: {}", t.getMessage(), t);
        throw new RuntimeException("Failed to delete product: " + t.getMessage(), t);
    }

    @Override
    public List<ProductResponse> findProductsByNameAuthorIdDeletedFalse(String productName) {
        log.info("findProductsByNameAuthorIdDeletedFalse: Searching for products with name: {}", productName);
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User author = userRepository.findByEmailAddress(email)
                .orElseThrow(() -> UnauthorizedException.builder()
                        .message("Authenticated user not found: " + email).build());

        List<Product> products = productRepository.findByNameContainingIgnoreCaseAuthorIdDeletedFalse(productName, author.getId());
        return products.stream().map(productMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public Page<ProductResponse> findProductsByNameAuthorIdDeletedFalse(String name, Pageable pageable) {
        log.info("findProductsByNameAuthorIdDeletedFalse: Searching for products with name: {}, page: {}", name, pageable);
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User author = userRepository.findByEmailAddress(email)
                .orElseThrow(() -> UnauthorizedException.builder()
                        .message("Authenticated user not found: " + email).build());

        Page<Product> productPage = productRepository.findByNameContainingIgnoreCaseAuthorIdDeletedFalse(name, author.getId(), pageable);
        return productPage.map(productMapper::toResponse);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "productsByCategory", "allProducts", "productImages"}, allEntries = true)
    public ProductResponse saveProduct(ProductRequest productRequest, List<MultipartFile> imageFiles) {
        log.info("saveProduct: Saving product: {}", productRequest.getName());
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User author = userRepository.findByEmailAddress(email)
                .orElseThrow(() -> UnauthorizedException.builder()
                        .message("Authenticated user not found: " + email).build());

        Product product = productMapper.toEntity(productRequest, author);
        product.setProductStatus(product.getQuantity() > 0 ? ProductStatus.AVAILABLE : ProductStatus.OUT_OF_STOCK);
        product.setCreatedAt(LocalDateTime.now());
        product.setDeleted(false);

        // Save product first to get ID
        Product savedProduct = productRepository.save(product);

        List<ProductImage> productImages = new ArrayList<>();
        if (imageFiles != null && !imageFiles.isEmpty()) {
            for (int i = 0; i < imageFiles.size(); i++) {
                ProductImage image = productImageService.uploadImage(savedProduct.getId(), imageFiles.get(i), i == 0);
                productImages.add(image);
            }
        }
        product.setImages(productImages);
        productImages.forEach(image -> image.setProduct(savedProduct));

        productRepository.save(savedProduct);
        log.info("Product saved: {} by user {}, Images: {}", savedProduct.getName(), email, productImages.size());

        //auditLogService.logAction(email, "SAVE_PRODUCT", "Product: " + savedProduct.getName() + ", Images: " + productImages.size());

        return productMapper.toResponse(savedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "productsByCategory", "allProducts"}, allEntries = true)
    public ProductResponse updateProductQuantity(Long id, int quantity) {
        log.info("ProductService: updateQuantity {},{}", id, quantity);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found with id: {}", id);
                    return ProductNotFoundException.builder().message("Product not Found.").build();
                });
        product.setQuantity(quantity);
        product.setProductStatus(quantity > 0 ? ProductStatus.AVAILABLE : ProductStatus.OUT_OF_STOCK);

        Product updatedProduct = productRepository.save(product);
        return productMapper.toResponse(updatedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "productsByCategory", "allProducts"}, allEntries = true)
    public ProductResponse updateProductPrice(Long id, BigDecimal price) {
        log.info("ProductService: updateQuantity {},{}", id, price);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found with id: {}", id);
                    return ProductNotFoundException.builder().message("Product not Found.").build();
                });
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            log.error("Invalid Price : {}", price);
            throw new IllegalArgumentException("Invalid Price");
        }
        product.setPrice(price);
        Product updatedProduct = productRepository.save(product);
        return productMapper.toResponse(updatedProduct);
    }

    @Override
    public Page<ProductResponse> getAuthorOrCompanyProducts(Long companyId, int page, int size) {
        log.info("getAuthorOrCompanyProducts: Fetching products for companyId: {}, page: {}, size: {}", companyId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products;

        if (companyId == null) {
            User user = userRepository.findByEmailAddressAndDeletedFalse(SecurityContextHolder.getContext().getAuthentication().getName())
                    .orElseThrow(() -> UserNotFoundException.builder().message("User not found").build());
            if (!hasRole(user, RoleTypes.SELLER)) {
                log.error("User {} is not a seller", user.getEmailAddress());
                throw UnauthorizedException.builder().message("User is not a seller").build();
            }
            products = productRepository.findByAuthorIdAndDeletedFalse(user.getId(), pageable);
        } else {
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> ResourceNotFoundException.builder()
                            .message("Company not found with id: " + companyId).build());
            products = productRepository.findByCompanyIdAndDeletedFalse(companyId, pageable);
        }

        return products.map(productMapper::toResponse);
    }

    @Override
    @Transactional
    @RateLimiter(name = "mergeProductsToCompany", fallbackMethod = "fallbackMergeProductsToCompany")
    @CacheEvict(value = {"products", "productsByCategory", "allProducts", "productImages"}, allEntries = true)
    public void mergeProductsToCompany(Long companyId, String authorEmail, String companyAdminEmail) {
        log.info("mergeProductsToCompany: {} merging products for seller {} to company {}", companyAdminEmail, authorEmail, companyId);
        User companyAdmin = userRepository.findByEmailAddressAndDeletedFalse(companyAdminEmail)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("Admin not found with email: " + companyAdminEmail).build());
        User author = userRepository.findByEmailAddressAndDeletedFalse(authorEmail)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("Seller not found with email: " + authorEmail).build());
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> ResourceNotFoundException.builder()
                        .message("Company not found with id: " + companyId).build());

        if (!hasRole(companyAdmin, RoleTypes.COMPANY_ADMIN_SELLER)) {
            log.error("User {} lacks COMPANY_ADMIN_SELLER role", companyAdminEmail);
            throw UnauthorizedException.builder().message("User lacks COMPANY_ADMIN_SELLER role").build();
        }

        CompanyAdminRights companyAdminRights = companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, companyAdmin.getId())
                .orElseThrow(() -> UnauthorizedException.builder()
                        .message("Admin has no rights for company: " + companyId).build());
        if (!companyAdminRights.getCanAddRemoveSellers()) {
            log.error("Admin {} lacks permission to merge products", companyAdminEmail);
            throw UnauthorizedException.builder().message("Admin lacks permission to merge products").build();
        }

        if (!hasRole(author, RoleTypes.SELLER) || author.getCompany() == null || !author.getCompany().getId().equals(companyId)) {
            log.error("User {} is not a seller associated with company {}", authorEmail, companyId);
            throw BadRequestException.builder().message("User is not a seller associated with the company").build();
        }

        productRepository.updateCompanyForAuthorProducts(author.getId(), company);
        log.info("Merged products to company {}", companyId);

        auditLogService.logAction(authorEmail, "MERGE_PRODUCTS_TO_COMPANY", companyAdminEmail, "Company ID: " + companyId);
    }

    private void fallbackMergeProductsToCompany(Long companyId, String authorEmail, String companyAdminEmail, Throwable t) {
        log.error("Fallback triggered for mergeProductsToCompany: {}", t.getMessage(), t);
        throw new RuntimeException("Failed to merge products to company: " + t.getMessage(), t);
    }

    private void updatePrimaryImage(Product product, Long primaryImageId, boolean hasNewImages) {
        List<ProductImage> images = product.getImages();
        if (images == null || images.isEmpty()) {
            return;
        }

        images.forEach(img -> img.setPrimary(false));

        if (primaryImageId != null) {
            ProductImage primaryImage = images.stream()
                    .filter(img -> img.getId() != null && img.getId().equals(primaryImageId))
                    .findFirst()
                    .orElseThrow(() -> BadRequestException.builder().message("Primary image ID " + primaryImageId + " not found").build());
            primaryImage.setPrimary(true);
        } else if (hasNewImages) {
            images.stream()
                    .filter(img -> img.getId() == null)
                    .findFirst()
                    .ifPresent(img -> img.setPrimary(true));
        } else {
            images.stream()
                    .min((img1, img2) -> Integer.compare(img1.getDisplayOrder(), img2.getDisplayOrder()))
                    .ifPresent(img -> img.setPrimary(true));
        }
    }

    private boolean hasRole(User user, RoleTypes roleType) {
        return user.getUserRoles().stream().anyMatch(role -> role.getRole() == roleType);
    }
}
*/


/*
package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.configs.GroupConfig;
import com.datasaz.ecommerce.exceptions.*;
import com.datasaz.ecommerce.mappers.ProductMapper;
import com.datasaz.ecommerce.models.request.ProductImageRequest;
import com.datasaz.ecommerce.models.request.ProductRequest;
import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.repositories.*;
import com.datasaz.ecommerce.repositories.entities.*;
import com.datasaz.ecommerce.services.interfaces.ISellerProductService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
@Slf4j
public class SellerProductService implements ISellerProductService {

    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;
    private final ProductImageService productImageService;
    private final AuditLogService auditLogService;
    private final CompanyAdminRightsRepository companyAdminRightsRepository;
    private final GroupConfig groupConfig;
    private final Tika tika = new Tika();
    //private final NegativeOrZeroValidatorForInteger negativeOrZeroValidatorForInteger;


    @Override
    @Transactional
    @RateLimiter(name = "manageProduct", fallbackMethod = "fallbackCreateProduct")
    @CacheEvict(value = {"products", "productsByCategory", "allProducts"}, allEntries = true)
    public ProductResponse createProduct(ProductRequest productRequest, List<ProductImageRequest> images, String email) {
        log.info("createProduct: Creating product for user: {}", email);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User not found with email: " + email).build());

        if (!hasRole(user, RoleTypes.SELLER)) {
            log.error("User {} is not authorized to create product", email);
            throw UnauthorizedException.builder()
                    .message("User is not authorized to create product").build();
        }

        List<ProductImage> productImages = uploadImages(images, email);
        if (!productImages.isEmpty()) {
            productImages.get(0).setPrimary(true); // Set first image as primary
        }

        Product product = productMapper.toEntity(productRequest, user);
        product.setCompany(user.getCompany());
        product.setAuthor(user);
        product.setImages(productImages);

        product.setCreatedAt(LocalDateTime.now());
        product.setDeleted(false);

        productImages.forEach(image -> image.setProduct(product));
        productRepository.save(product);
        log.info("Product created: {} by user {}", product.getName(), email);

        auditLogService.logAction(email, "CREATE_PRODUCT", "Product: " + product.getName() + ", Images: " + productImages.size());

        return productMapper.toResponse(product);
    }

    private ProductResponse fallbackCreateProduct(ProductRequest request, List<ProductImageRequest> images, String email, Throwable t) {
        log.error("Fallback triggered for createProduct: {}", t.getMessage(), t);
        throw new RuntimeException("Either too many requests or exceeded cache. Failed to create product: " + t.getMessage(), t);

    }

    @Override
    @Transactional
    @RateLimiter(name = "manageProduct", fallbackMethod = "fallbackUpdateProduct")
    @CacheEvict(value = {"products", "productsByCategory", "allProducts"}, allEntries = true)
    public ProductResponse updateProduct(Long productId, ProductRequest request, List<ProductImageRequest> newImages, List<Long> imagesToRemove, Long primaryImageId, String email) {
        log.info("updateProduct: Updating product {} by user {}", productId, email);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User not found with email: " + email).build());
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> ProductNotFoundException.builder()
                        .message("Product not found with id: " + productId).build());

        boolean isCompanyAdmin = hasRole(user, RoleTypes.COMPANY_ADMIN_SELLER) && user.getCompany() != null && product.getCompany() != null && user.getCompany().getId().equals(product.getCompany().getId());
        boolean isIndividualSeller = product.getAuthor() != null && product.getAuthor().getId().equals(user.getId()) && product.getCompany() == null;

        if (!isCompanyAdmin && !isIndividualSeller) {
            log.error("User {} is not authorized to update product {}", email, productId);
            throw UnauthorizedException.builder().message("User is not authorized to update this product").build();
        }

        // Update product fields
        if (request.getName() != null) product.setName(request.getName());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getOfferPrice() != null) product.setOfferPrice(request.getOfferPrice());
        if (request.getQuantity() > 0) {
            product.setQuantity(request.getQuantity());
            product.setProductStatus(request.getQuantity() > 0 ? ProductStatus.AVAILABLE : ProductStatus.OUT_OF_STOCK);
        }
        if (request.getInventoryLocation() != null) product.setInventoryLocation(request.getInventoryLocation());
        if (request.getWarranty() != null) product.setWarranty(request.getWarranty());
        if (request.getBrand() != null) product.setBrand(request.getBrand());
        if (request.getProductCode() != null) product.setProductCode(request.getProductCode());
        if (request.getManufacturingPieceNumber() != null)
            product.setManufacturingPieceNumber(request.getManufacturingPieceNumber());
        if (request.getManufacturingDate() != null) product.setManufacturingDate(request.getManufacturingDate());
        if (request.getEAN() != null) product.setEAN(request.getEAN());
        if (request.getManufacturingPlace() != null) product.setManufacturingPlace(request.getManufacturingPlace());
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            product.setCategory(category);
        }
        if (request.getVariants() != null) {
            product.getVariants().clear();
            List<ProductVariant> variants = request.getVariants().stream()
                    .map(var -> ProductVariant.builder()
                            .name(var.getName())
                            .priceAdjustment(var.getPriceAdjustment())
                            .quantity(var.getQuantity())
                            .product(product)
                            .build())
                    .collect(Collectors.toList());
            product.setVariants(variants);
        }

        // Handle image updates
        List<ProductImage> existingImages = product.getImages();
        if (imagesToRemove != null && !imagesToRemove.isEmpty()) {
            List<ProductImage> imagesToDelete = existingImages.stream()
                    .filter(img -> imagesToRemove.contains(img.getId()))
                    .collect(Collectors.toList());
            imagesToDelete.forEach(img -> {
                deleteImageFile(img.getFileUrl());
                existingImages.remove(img);
            });
        }

        List<ProductImage> newProductImages = uploadImages(newImages, email);
        newProductImages.forEach(img -> img.setProduct(product));
        existingImages.addAll(newProductImages);

        // Update primary image
        updatePrimaryImage(product, primaryImageId, !newProductImages.isEmpty());

        product.setUpdatedAt(LocalDateTime.now());
        product.setDeleted(false);

        productRepository.save(product);
        log.info("Product updated: {} by user {}, Images: {}", product.getName(), email, existingImages.size());

        auditLogService.logAction(email, "UPDATE_PRODUCT", "Product ID: " + productId + ", Images updated: " + newProductImages.size() + ", Images removed: " + (imagesToRemove != null ? imagesToRemove.size() : 0));

        return productMapper.toResponse(product);
    }

    private ProductResponse fallbackUpdateProduct(Long productId, ProductRequest request, List<ProductImageRequest> newImages, List<Long> imagesToRemove, Long primaryImageId, String email, Throwable t) {
        log.error("Rate limit exceeded for updateProduct: {}", t.getMessage());
        throw new RuntimeException("Too many requests, please try again later");
    }

    @Override
    public List<ProductResponse> findProductsByNameAuthorIdDeletedFalse(String productName) {
        log.info("Find products with name containing: {}", productName);

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User author = userRepository.findByEmailAddress(email)
                .orElseThrow(() -> UnauthorizedException.builder()
                        .message("Authenticated user not found : " + email).build());


        List<Product> products = productRepository.findByNameContainingIgnoreCaseAuthorIdDeletedFalse(productName, author.getId());

        return products.isEmpty() ? Collections.emptyList() : products.stream().map(productMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public Page<ProductResponse> findProductsByNameAuthorIdDeletedFalse(String name, Pageable pageable) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User author = userRepository.findByEmailAddress(email)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        Page<Product> productPage = productRepository.findByNameContainingIgnoreCaseAuthorIdDeletedFalse(name, author.getId(), pageable);
        return productPage.map(productMapper::toResponse);
    }


    // remove if duplicate with creatProduct method
    @Override
    @Transactional
    @CacheEvict(value = {"products", "productsByCategory", "allProducts"}, allEntries = true)
    public ProductResponse saveProduct(ProductRequest productRequest, List<MultipartFile> imageFiles) {
        log.info("Saving product: {}", productRequest);
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User author = userRepository.findByEmailAddress(email)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        Product product = productMapper.toEntity(productRequest, author);
        product.setProductStatus(product.getQuantity() > 0 ? ProductStatus.AVAILABLE : ProductStatus.OUT_OF_STOCK);
        Product savedProduct = productRepository.save(product);
        uploadImages(savedProduct.getId(), imageFiles);

        // Save images
//        try {
//        if (productRequest.getImages() != null) {
//            productRequest.getImages().forEach(imageReq -> {
//                    productImageService.uploadImage(savedProduct.getId(),
//                            convertBase64ToMultipartFile(imageReq.getBase64Content(), imageReq.getFileName()),
//                            imageReq.isPrimary());
//            });
//        }
//        } catch (Exception e) {
//            log.error("Failed to save image for product {}", savedProduct.getId(), e);
//            throw new RuntimeException("Image upload failed", e);
//        }
        return productMapper.toResponse(savedProduct);
    }

//    @Override
//    public ProductResponse saveProduct(ProductRequest productRequest) {
//        log.info("ProductService: save {}", productRequest);
//        Product product = productMapper.toEntity(productRequest, categoryRepository, userRepository);
//        Product savedProduct = productRepository.save(product);
//        return productMapper.toResponse(savedProduct);
//    }

//    @Override
//    @Transactional
//    public void deleteProduct(Long id) {
//        log.info("Soft deleting product: {}", id);
//        Product product = productRepository.findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
//        product.setDeleted(true);
//        product.setProductStatus(Product.ProductStatus.INACTIVE);
//        productRepository.save(product);
//    }

    //    @Override
//    public void deleteProduct(Long id) {
//        log.info("ProductService: deleteById {}", id);
//        productRepository.deleteById(id);
//    }

    @Override
    @Transactional
    @RateLimiter(name = "manageProduct", fallbackMethod = "fallbackDeleteProduct")
    @CacheEvict(value = {"products", "productsByCategory", "allProducts"}, allEntries = true)
    public void deleteProduct(Long productId, String email) {
        log.info("deleteProduct: Deleting product {} by user {}", productId, email);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User not found with email: " + email + " Product not Deleted").build());
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> ProductNotFoundException.builder()
                        .message("Product not found with id: " + productId).build());

        boolean isCompanyAdmin = hasRole(user, RoleTypes.COMPANY_ADMIN_SELLER) && user.getCompany() != null && product.getCompany() != null && user.getCompany().getId().equals(product.getCompany().getId());
        boolean isIndividualSeller = product.getAuthor() != null && product.getAuthor().getId().equals(user.getId()) && product.getCompany() == null;

        if (!isCompanyAdmin && !isIndividualSeller) {
            log.error("User {} is not authorized to delete product {}", email, productId);
            throw UnauthorizedException.builder().message("User is not authorized to delete this product").build();
        }

        product.getImages().forEach(img -> deleteImageFile(img.getFileUrl()));
        product.setDeleted(true);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
        log.info("Product deleted: {} by user {}", productId, email);

        auditLogService.logAction(email, "DELETE_PRODUCT", "Product ID: " + productId);
    }

    private void fallbackDeleteProduct(Long productId, String email, Throwable t) {
        log.error("Rate limit exceeded for deleteProduct: {}", t.getMessage());
        throw new RuntimeException("Too many requests, please try again later");
    }

    // if company is null it returns current authors products else company products
    @Override
    public Page<ProductResponse> getAuthorOrCompanyProducts(Long companyId, int page, int size) {
        log.info("getCompanyProducts: Fetching products for company {} or individual seller, page: {}, size: {}", companyId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products;

        if (companyId == null) {
            User user = userRepository.findByEmailAddressAndDeletedFalse(SecurityContextHolder.getContext().getAuthentication().getName())
                    .orElseThrow(() -> UserNotFoundException.builder().message("User not found").build());
            if (!hasRole(user, RoleTypes.SELLER)) {
                log.error("User {} is not a seller", user.getEmailAddress());
                throw UnauthorizedException.builder().message("User is not a seller").build();
            }
            products = productRepository.findByAuthorIdAndDeletedFalse(user.getId(), pageable);
        } else {
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> ResourceNotFoundException.builder()
                            .message("Company not found with id: " + companyId).build());
            products = productRepository.findByCompanyIdAndDeletedFalse(companyId, pageable);
        }

        return products.map(productMapper::toResponse);
    }

    @Override
    @Transactional
    @RateLimiter(name = "mergeProductsToCompany", fallbackMethod = "fallbackMergeProductsToCompany")
    @CacheEvict(value = {"products", "productsByCategory", "allProducts"}, allEntries = true)
    public void mergeProductsToCompany(Long companyId, String authorEmail, String companyAdminEmail) {
        log.info("mergeProductsToCompany: {} merging products for seller {} to company {}", companyAdminEmail, authorEmail, companyId);
        User companyAdmin = userRepository.findByEmailAddressAndDeletedFalse(companyAdminEmail)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("Admin not found with email: " + companyAdminEmail).build());
        User author = userRepository.findByEmailAddressAndDeletedFalse(authorEmail)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("Seller not found with email: " + authorEmail).build());
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> ResourceNotFoundException.builder()
                        .message("Company not found with id: " + companyId).build());

        if (!hasRole(companyAdmin, RoleTypes.COMPANY_ADMIN_SELLER)) {
            log.error("User {} lacks COMPANY_ADMIN_SELLER role", companyAdminEmail);
            throw UnauthorizedException.builder().message("User lacks COMPANY_ADMIN_SELLER role").build();
        }

        CompanyAdminRights companyAdminRights = companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, companyAdmin.getId())
                .orElseThrow(() -> UnauthorizedException.builder()
                        .message("Admin has no rights for company: " + companyId).build());
        if (!companyAdminRights.getCanAddRemoveSellers()) {
            log.error("Admin {} lacks permission to merge products", companyAdminEmail);
            throw UnauthorizedException.builder().message("Admin lacks permission to merge products").build();
        }

        if (!hasRole(author, RoleTypes.SELLER) || author.getCompany() == null || !author.getCompany().getId().equals(companyId)) {
            log.error("User {} is not a seller associated with company {}", authorEmail, companyId);
            throw BadRequestException.builder().message("User is not a seller associated with the company").build();
        }

        productRepository.updateCompanyForAuthorProducts(author.getId(), company);
        log.info("Merged products to company {}", companyId);

        auditLogService.logAction(authorEmail, "MERGE_PRODUCTS_TO_COMPANY", companyAdminEmail, "Company ID: " + companyId);

    }

    private void fallbackMergeProductsToCompany(Long companyId, String authorEmail, String companyAdminEmail, Throwable t) {
        log.error("Rate limit exceeded for mergeProductsToCompany: {}", t.getMessage());
        throw new RuntimeException("Too many requests, please try again later");
    }

    @Override
    @Transactional
    @RateLimiter(name = "updateProductImages", fallbackMethod = "fallbackUpdateProductImages")
    @CacheEvict(value = {"products", "productsByCategory", "allProducts"}, allEntries = true)
    public ProductResponse updateProductImages(Long productId, List<ProductImageRequest> newImages, List<Long> imagesToRemove, Long primaryImageId, String email) {
        log.info("updateProductImages: Updating images for product {} by user {}", productId, email);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User not found with email: " + email).build());
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> ProductNotFoundException.builder()
                        .message("Product not found with id: " + productId).build());

        boolean isCompanyAdmin = hasRole(user, RoleTypes.COMPANY_ADMIN_SELLER) && user.getCompany() != null && product.getCompany() != null && user.getCompany().getId().equals(product.getCompany().getId());
        boolean isIndividualSeller = product.getAuthor() != null && product.getAuthor().getId().equals(user.getId()) && product.getCompany() == null;

        if (!isCompanyAdmin && !isIndividualSeller) {
            log.error("User {} is not authorized to update product images {}", email, productId);
            throw UnauthorizedException.builder()
                    .message("User is not authorized to update this product's images").build();
        }

        List<ProductImage> existingImages = product.getImages();
        if (imagesToRemove != null && !imagesToRemove.isEmpty()) {
            List<ProductImage> imagesToDelete = existingImages.stream()
                    .filter(img -> imagesToRemove.contains(img.getId()))
                    .collect(Collectors.toList());
            imagesToDelete.forEach(img -> {
                deleteImageFile(img.getFileUrl());
                existingImages.remove(img);
            });
        }

        List<ProductImage> newProductImages = uploadImages(newImages, email);
        newProductImages.forEach(img -> img.setProduct(product));
        existingImages.addAll(newProductImages);

        updatePrimaryImage(product, primaryImageId, !newProductImages.isEmpty());

        productRepository.save(product);
        log.info("Product images updated: {} by user {}, Images: {}", productId, email, existingImages.size());

        auditLogService.logAction(email, "UPDATE_PRODUCT_IMAGES", "Product ID: " + productId + ", Images updated: " + newProductImages.size() + ", Images removed: " + (imagesToRemove != null ? imagesToRemove.size() : 0));

        return productMapper.toResponse(product);
    }

    private ProductResponse fallbackUpdateProductImages(Long productId, List<ProductImageRequest> newImages, List<Long> imagesToRemove, Long primaryImageId, String email, Throwable t) {
        log.error("Rate limit exceeded for updateProductImages: {}", t.getMessage());
        throw new RuntimeException("Too many requests, please try again later");
    }

//    @Override
//    public ProductResponse updateProduct(Long id, ProductRequest productRequest, List<MultipartFile> imageFiles) {
//        log.info("ProductService: update {},{}", productRequest, id);
//        Product product = productRepository.findById(id)
//                .orElseThrow(() -> {
//                    log.error("Product not found with id: {}", id);
//                    return ProductNotFoundException.builder().message("Product not Found.").build();
//                });
//
//        String email = SecurityContextHolder.getContext().getAuthentication().getName();
//        if (!product.getAuthor().getEmailAddress().equals(email)) {
//            throw new SecurityException("Unauthorized to modify this product");
//        }
//        User lastUpdateAuthor = userRepository.findByEmailAddress(email)
//                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
//
//
//        productMapper.toEntity(productRequest, lastUpdateAuthor);
//        Product updatedProduct = productRepository.save(product);
//
//        productImageService.deleteImagesByProductId(id);
//        uploadImages(id, imageFiles);
//
//        return productMapper.toResponse(updatedProduct);
//    }

    public void uploadImages(Long productId, List<MultipartFile> imageFiles) {
        if (imageFiles != null && !imageFiles.isEmpty()) {
            for (int i = 0; i < imageFiles.size(); i++) {
                try {
                    productImageService.uploadImage(productId, imageFiles.get(i), i == 0);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to upload image", e);
                }
            }
        }
    }


    // Convert Base64 to MultipartFile (temporary, prefer direct MultipartFile uploads)
//    private MultipartFile convertBase64ToMultipartFile(String base64Content, String fileName) {
//        byte[] content = Base64.getDecoder().decode(base64Content);
//        return new MockMultipartFile(fileName, fileName, "image/jpeg", content);
//    }


    @Override
    @Transactional
    @CacheEvict(value = {"products", "productsByCategory", "allProducts"}, allEntries = true)
    public ProductResponse updateProductQuantity(Long id, int quantity) {
        log.info("ProductService: updateQuantity {},{}", id, quantity);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found with id: {}", id);
                    return ProductNotFoundException.builder().message("Product not Found.").build();
                });
        product.setQuantity(quantity);
        product.setProductStatus(quantity > 0 ? ProductStatus.AVAILABLE : ProductStatus.OUT_OF_STOCK);

        Product updatedProduct = productRepository.save(product);
        return productMapper.toResponse(updatedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "productsByCategory", "allProducts"}, allEntries = true)
    public ProductResponse updateProductPrice(Long id, BigDecimal price) {
        log.info("ProductService: updateQuantity {},{}", id, price);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found with id: {}", id);
                    return ProductNotFoundException.builder().message("Product not Found.").build();
                });
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            log.error("Invalid Price : {}", price);
            throw new IllegalArgumentException("Invalid Price");
        }
        product.setPrice(price);
        Product updatedProduct = productRepository.save(product);
        return productMapper.toResponse(updatedProduct);
    }


    private List<ProductImage> uploadImages(List<ProductImageRequest> images, String email) {
        List<ProductImage> productImages = new ArrayList<>();
        if (images == null || images.isEmpty()) {
            return productImages;
        }

        for (int i = 0; i < images.size(); i++) {
            ProductImageRequest image = images.get(i);
            if (image.getFileContent() == null || image.getFileContent().isEmpty()) continue;

            byte[] imageBytes;
            try {
                imageBytes = Base64.getDecoder().decode(image.getFileContent());
            } catch (IllegalArgumentException e) {
                log.error("Invalid base64 content for image: {}", image.getFileName());
                throw BadRequestException.builder()
                        .message("Invalid base64 content for image: " + image.getFileName()).build();
            }

            if (imageBytes.length > GroupConfig.DEFAULT_MAX_IMAGE_SIZE) {
                log.error("Image size {} exceeds limit {}", imageBytes.length, GroupConfig.DEFAULT_MAX_IMAGE_SIZE);
                throw BadRequestException.builder()
                        .message("Image size exceeds limit").build();
            }

            String mimeType;
            try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
                mimeType = tika.detect(bais);
                if (!GroupConfig.ALLOWED_IMAGE_TYPES.contains(mimeType)) {
                    log.error("Unsupported image type: {}", mimeType);
                    throw BadRequestException.builder()
                            .message("Unsupported image type: " + mimeType).build();
                }
            } catch (IOException e) {
                log.error("Error detecting image type: {}", e.getMessage());
                throw BadRequestException.builder()
                        .message("Error detecting image type: " + e.getMessage()).build();
            }

            try {
                Path uploadDir = Path.of(groupConfig.UPLOAD_DIR, "products");
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                    log.info("Created upload directory: {}", uploadDir);
                }

                String fileExtension = getFileExtension(image.getFileName());
                String fileName = UUID.randomUUID() + "-" + LocalDateTime.now().toString().replace(":", "-") + "." + fileExtension;
                Path filePath = uploadDir.resolve(fileName);
                Files.write(filePath, imageBytes);

                ProductImage productImage = ProductImage.builder()
                        .fileName(fileName)
                        .fileUrl("/Uploads/products/" + fileName)
                        .contentType(mimeType)
                        .fileSize(imageBytes.length)
                        .fileExtension(fileExtension)
                        .createdAt(LocalDateTime.now())
                        .isPrimary(false)
                        .displayOrder(i)
                        .build();
                productImages.add(productImage);
                log.info("Uploaded image for user {}: {}", email, filePath);
            } catch (IOException e) {
                log.error("Failed to upload image for user {}: {}", email, e.getMessage());
                throw BadRequestException.builder()
                        .message("Failed to upload image: " + e.getMessage()).build();
            }
        }
        return productImages;
    }

    //@PrePersist
    //@PreUpdate
    //public void validatePrimary() {
    //    if (isPrimary && product != null) {
    //        // Service-level check or database constraint needed
    //    }
    //}

    private void updatePrimaryImage(Product product, Long primaryImageId, boolean hasNewImages) {
        List<ProductImage> images = product.getImages();
        if (images.isEmpty()) {
            return;
        }

        // Reset all images to non-primary
        images.forEach(img -> img.setPrimary(false));

        if (primaryImageId != null) {
            ProductImage primaryImage = images.stream()
                    .filter(img -> img.getId() != null && img.getId().equals(primaryImageId))
                    .findFirst()
                    .orElseThrow(() -> BadRequestException.builder()
                            .message("Primary image ID " + primaryImageId + " not found").build());
            primaryImage.setPrimary(true);
        } else if (hasNewImages) {
            // Set the first new image as primary if none specified
            images.stream()
                    .filter(img -> img.getId() == null) // New images have no ID yet
                    .findFirst()
                    .ifPresent(img -> img.setPrimary(true));
        } else {
            // Set the first existing image as primary
            images.stream()
                    .min((img1, img2) -> Integer.compare(img1.getDisplayOrder(), img2.getDisplayOrder()))
                    .ifPresent(img -> img.setPrimary(true));
        }
    }


    private boolean hasRole(User user, RoleTypes roleType) {
        return user.getUserRoles().stream().anyMatch(role -> role.getRole() == roleType);
    }

    private void deleteImageFile(String fileUrl) {
        if (fileUrl != null && !fileUrl.isEmpty()) {
            Path filePath = Path.of(fileUrl);
            if (Files.exists(filePath)) {
                try {
                    Files.deleteIfExists(filePath);
                    log.info("Deleted image file: {}", fileUrl);
                } catch (IOException e) {
                    log.warn("Failed to delete image file {}: {}", fileUrl, e.getMessage());
                }
            }
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "jpg";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

}
*/