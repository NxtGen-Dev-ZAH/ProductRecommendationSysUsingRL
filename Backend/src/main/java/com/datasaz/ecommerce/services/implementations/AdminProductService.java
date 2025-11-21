package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.ProductNotFoundException;
import com.datasaz.ecommerce.exceptions.UnauthorizedException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.mappers.ProductMapper;
import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.Product;
import com.datasaz.ecommerce.repositories.entities.RoleTypes;
import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.services.interfaces.IAdminProductService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
@Slf4j
@Transactional(readOnly = true)
public class AdminProductService implements IAdminProductService {

    private final ProductRepository productRepository;
    //private final CompanyRepository companyRepository;
    //private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;
    private final AuditLogService auditLogService;
    //private final ProductImageService productImageService;
    //private final AuditLogRepository auditLogRepository;
    //private final CompanyAdminRightsRepository companyAdminRightsRepository;
    // private final GroupConfig groupConfig;
    private final Tika tika = new Tika();
    //private final NegativeOrZeroValidatorForInteger negativeOrZeroValidatorForInteger;

    @Override
    @Cacheable(value = "allProducts")
    public List<ProductResponse> findAllProducts() {
        log.info("ProductService: findAll()");
        return productRepository.findAll().stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ProductResponse> findAllProducts(Pageable pageable) {
        log.info("Getting paginated categories");
        return productRepository.findAll(pageable).map(productMapper::toResponse);
    }

    @Override
    @Cacheable(value = "products", key = "#id")
    @RateLimiter(name = "productService", fallbackMethod = "fallbackFindProductById")
    public ProductResponse findProductById(Long id) {
        log.info("ProductService: findById {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found with id: {}", id);
                    return ProductNotFoundException.builder().message("Product not Found.").build();
                });
        return productMapper.toResponse(product);
    }

    private ProductResponse fallbackFindProductById(Long id, Throwable t) {
        log.error("Rate limit exceeded for findProductById: {}", t.getMessage());
        throw new RuntimeException("Too many requests, please try again later");
    }

    @Override
    public Page<ProductResponse> findProductsByName(String productName, Pageable pageable) {
        Page<Product> productPage = productRepository.findByNameContainingIgnoreCase(productName, pageable);
        return productPage.map(productMapper::toResponse);
    }

//    @Override
//    public List<ProductResponse> findProductsByName(String name) {
//        log.info("Find products with name containing: {}", name);
//        List<Product> products = productRepository.findByNameContainingIgnoreCase(name);
//
//        if (products.isEmpty())
//            return Collections.emptyList();
//        else
//            return products.stream().map(productMapper::toResponse).collect(Collectors.toList());
//    }


//    @Override
//    public List<ProductResponse> findProductsByNameAuthorIdDeletedFalse(String name) {
//        log.info("Find products with name containing: {}", name);
//
//        String email = SecurityContextHolder.getContext().getAuthentication().getName();
//        User author = userRepository.findByEmailAddress(email)
//                .orElseThrow(() -> UnauthorizedException.builder()
//                        .message("Authenticated user not found : " + email).build());
//
//
//        List<Product> products = productRepository.findByNameContainingIgnoreCaseAuthorIdDeletedFalse(name, author.getId());
//
//        if (products.isEmpty())
//            return Collections.emptyList();
//        else
//            return products.stream().map(productMapper::toResponse).collect(Collectors.toList());
//    }


    @Override
    @Transactional
    @RateLimiter(name = "manageProduct", fallbackMethod = "fallbackDeleteProductByAdmin")
    @CacheEvict(value = {"products", "productsByCategory", "allProducts"}, allEntries = true)
    public void deleteProductByAdmin(Long productId, String email) {
        log.info("deleteProduct: Deleting product {} by user {}", productId, email);

        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User not found with email: " + email + " Product not Deleted").build());

        if (!hasRole(user, RoleTypes.APP_ADMIN)) {
            throw UnauthorizedException.builder().message("User is not an admin").build();
        }

        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> ProductNotFoundException.builder()
                        .message("Product not found with id: " + productId).build());

//        boolean isCompanyAdmin = hasRole(user, RoleTypes.COMPANY_ADMIN_SELLER) && user.getCompany() != null && product.getCompany() != null && user.getCompany().getId().equals(product.getCompany().getId());
//        boolean isIndividualSeller = product.getAuthor() != null && product.getAuthor().getId().equals(user.getId()) && product.getCompany() == null;
//
//        if (!isCompanyAdmin && !isIndividualSeller) {
//            log.error("User {} is not authorized to delete product {}", email, productId);
//            throw UnauthorizedException.builder().message("User is not authorized to delete this product").build();
//        }

        product.getImages().forEach(img -> deleteImageFile(img.getFileUrl()));
        product.setDeleted(true);
        productRepository.save(product);
        log.info("Product deleted: {} by user {}", productId, email);

        auditLogService.logAction(email, "DELETE_PRODUCT", "Product ID: " + productId);
    }

    private void fallbackDeleteProductByAdmin(Long productId, String email, Throwable t) {
        log.error("Rate limit exceeded for deleteProductByAdmin: {}", t.getMessage());
        throw new RuntimeException("Too many requests, please try again later");
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


}
