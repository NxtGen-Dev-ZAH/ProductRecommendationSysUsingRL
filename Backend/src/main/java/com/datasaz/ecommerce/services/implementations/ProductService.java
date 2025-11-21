package com.datasaz.ecommerce.services.implementations;


import com.datasaz.ecommerce.exceptions.ResourceNotFoundException;
import com.datasaz.ecommerce.mappers.ProductMapper;
import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.entities.Product;
import com.datasaz.ecommerce.services.interfaces.IProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService implements IProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    public ProductResponse getProductById(Long id) {
        log.info("Fetching product with ID: {}", id);
        Product product = productRepository.findWithImageAttachesByIdAndDeletedFalse(id)
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Product not found with ID: " + id).build());
        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(int page, int size) {
        log.info("getAllProducts: Fetching products for page: {}, size: {}", page, size);
        Page<Product> products = productRepository.findAllWithImageAttaches(PageRequest.of(page, size));
        return products.map(productMapper::toResponse);
    }

//    @Override
//    public Page<ProductResponse> getAllProducts(int page, int size) {
//        log.info("Fetching all products, page: {}, size: {}", page, size);
//        Page<Product> products = productRepository.findAllByDeletedFalse(PageRequest.of(page, size));
//        return products.map(productMapper::toResponse);
//    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProductsByName(String name, Pageable pageable) {
        log.info("searchProductsByName: Searching products by name: {}, page: {}, size: {}", name, pageable.getPageNumber(), pageable.getPageSize());
        if (name == null || name.trim().isEmpty()) {
            log.warn("searchProductsByName: Name parameter is null or empty");
            return Page.empty(pageable);
        }
        Page<Product> products = productRepository.findByNameContainingIgnoreCaseAndDeletedFalse(name.trim(), pageable);
        return products.map(productMapper::toResponse);
    }

//    @Override
//    public List<ProductResponse> searchProductsByName(String name) {
//        log.info("Searching products by name: {}", name);
//        List<Product> products = productRepository.findByNameContainingIgnoreCaseAndDeletedFalse(name);
//        return products.stream().map(productMapper::toResponse).collect(Collectors.toList());
//    }

    @Override
    public Page<ProductResponse> getProductsByCategory(Long categoryId, int page, int size) {
        log.info("Fetching products for category ID: {}, page: {}, size: {}", categoryId, page, size);
        Page<Product> products = productRepository.findByCategoryIdAndDeletedFalse(categoryId, PageRequest.of(page, size));
        return products.map(productMapper::toResponse);
    }

//    @Override
//    @Cacheable(value = "productsByCategory", key = "#categoryId")
//    @RateLimiter(name = "productService", fallbackMethod = "fallbackFindProductsByCategoryId")
//    public List<ProductResponse> findProductsByCategoryId(Long categoryId) {
//        log.info("ProductService: findProductsByCategoryId {}", categoryId);
//        return productRepository.findByCategoryId(categoryId).stream()
//                .map(productMapper::toResponse)
//                .collect(Collectors.toList());
//    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getFeaturedProducts(int page, int size) {
        log.info("getFeaturedProducts: Fetching products for page: {}, size: {}", page, size);

        // TODO: Use ML / favorites / viewed

        Page<Product> products = productRepository.findAllWithImageAttaches(PageRequest.of(page, size));
        return products.map(productMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getNewArrivalProducts(Pageable pageable) {
        log.info("getNewArrivalProducts: Fetching products for page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        // TODO: Use ML / favorites / viewed

        Page<Product> products = productRepository.findAllByDeletedFalse(pageable);
        return products.map(productMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllSortedByProductSoldQuantity(Pageable pageable) {
        log.info("getAllSortedByProductSoldQuantity: Fetching products for page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<Product> products = productRepository.findAllSortedByProductSoldQuantity(pageable);
        return products.map(productMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getByOfferPriceLessThanPrice(int page, int size) {
        log.info("getByOfferPriceLessThanPrice: Fetching products for page: {}, size: {}", page, size);

        Page<Product> products = productRepository.findByOfferPriceLessThanPriceAndDeletedFalse(PageRequest.of(page, size));
        return products.map(productMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getRelatedProducts(Long categoryId, Long productId, Pageable pageable) {
        log.info("getRelatedProducts: Related for {}, category: {}", productId, categoryId);

        Page<Product> relatedProducts = productRepository.findByCategoryIdAndIdNotAndDeletedFalse(categoryId, productId, pageable);
        return relatedProducts.map(productMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getRecommendedProducts(int page, int size) {
        log.info("getFeaturedProducts: Fetching products for page: {}, size: {}", page, size);

        // TODO: Use ML / favorites / viewed

        Page<Product> products = productRepository.findAllWithImageAttaches(PageRequest.of(page, size));
        return products.map(productMapper::toResponse);
    }
}


/*
import com.datasaz.ecommerce.exceptions.ProductNotFoundException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.mappers.ProductMapper;
import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.Product;
import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.services.interfaces.IProductService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
@Slf4j
@Transactional(readOnly = true)
public class ProductService implements IProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;


    @Override
    @Cacheable(value = "products", key = "#pageable")
    @RateLimiter(name = "productService")
    public Page<ProductResponse> findAllProductsDeletedFalse(Pageable pageable) {
        log.info("Getting paginated products");

        return productRepository.findByDeletedFalse(pageable)
                .map(productMapper::toResponse);
    }

    @Override
    @Cacheable(value = "products", key = "#id")
    @RateLimiter(name = "productService", fallbackMethod = "fallbackFindProductByIdDeletedFalse")
    public ProductResponse findProductByIdDeletedFalse(Long id) {
        log.info("ProductService: findById {}", id);
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.error("Active product not found with id: {}", id);
                    return ProductNotFoundException.builder().message("Product not Found.").build();
                });
        return productMapper.toResponse(product);
    }

    private ProductResponse fallbackFindProductByIdDeletedFalse(Long id, Throwable t) {
        log.error("Rate limit exceeded for findProductByIdDeletedFalse: {}", t.getMessage());
        throw new RuntimeException("Too many requests, please try again later");
    }

    @Override
    public Page<ProductResponse> findProductsByNameAuthorNameDeletedFalse(String productName, String email, Pageable pageable) {
        User author = userRepository.findByEmailAddress(email)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        Page<Product> productPage = productRepository.findByNameContainingIgnoreCaseAuthorIdDeletedFalse(productName, author.getId(), pageable);
        return productPage.map(productMapper::toResponse);
    }


    @Override
    public Page<ProductResponse> findProductsByAuthorEmailAddressDeletedFalse(String email, Pageable pageable) {
        if (userRepository.existsByEmailAddressAndDeletedFalse(email)) {
            Page<Product> productPage = productRepository.findByAuthorEmailAddressAndDeletedFalse(email, pageable);
            return productPage.map(productMapper::toResponse);
        } else {
            throw UserNotFoundException.builder().message("User not found").build();
        }
    }



    private List<ProductResponse> fallbackFindProductsByCategoryId(Long categoryId, Throwable t) {
        log.error("Rate limit exceeded for findProductsByCategoryId: {}", t.getMessage());
        throw new RuntimeException("Too many requests, please try again later");
    }

    @Override
    @Cacheable(value = "productsByCategory", key = "#categoryId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    @RateLimiter(name = "productService")
    public Page<ProductResponse> findProductsByCategoryId(Long categoryId, Pageable pageable) {
        Page<Product> productPage = productRepository.findByCategoryId(categoryId, pageable);
        return productPage.map(productMapper::toResponse);
    }


    @Override
    public Page<ProductResponse> findProductBySoldQuantity(Pageable pageable) {
        log.info("ProductService: findProductBySoldQuantity {}", pageable);

        Page<Product> productPage = productRepository.findAllSortedByProductSoldQuantity(pageable);
        return productPage.map(productMapper::toResponse);
    }


}
*/