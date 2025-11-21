/*package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.ResourceNotFoundException;
import com.datasaz.ecommerce.mappers.ProductMapper;
import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.entities.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private ProductMapper productMapper;

    @Test
    void getProductById_Success_ReturnsProductResponse() {
        // Arrange
        Long productId = 1L;
        Product product = Product.builder()
                .id(productId)
                .name("Test Product")
                .price(new BigDecimal("100.00"))
                .quantity(10)
                .productStatus(ProductStatus.AVAILABLE)
                .createdAt(LocalDateTime.now())
                .author(User.builder().id(1L).build())
                .category(Category.builder().id(1L).build())
                .build();
        ProductResponse productResponse = ProductResponse.builder()
                .id(productId)
                .name("Test Product")
                .price(new BigDecimal("100.00"))
                .quantity(10)
                .productStatus(ProductStatus.AVAILABLE)
                .authorId(1L)
                .categoryId(1L)
                .build();

        when(productRepository.findByIdAndDeletedFalse(productId)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(productResponse);

        // Act
        ProductResponse result = productService.getProductById(productId);

        // Assert
        assertNotNull(result);
        assertEquals(productId, result.getId());
        assertEquals("Test Product", result.getName());
        assertEquals(new BigDecimal("100.00"), result.getPrice());
    }

    @Test
    void getProductById_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        Long productId = 1L;
        when(productRepository.findByIdAndDeletedFalse(productId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> productService.getProductById(productId));
        assertEquals("Product not found with ID: " + productId, exception.getMessage());
    }

    @Test
    void getAllProducts_Success_ReturnsPagedProductResponses() {
        // Arrange
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Product product = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(new BigDecimal("100.00"))
                .quantity(10)
                .build();
        ProductResponse productResponse = ProductResponse.builder()
                .id(1L)
                .name("Test Product")
                .price(new BigDecimal("100.00"))
                .quantity(10)
                .build();
        Page<Product> productPage = new PageImpl<>(Collections.singletonList(product), pageable, 1);

        when(productRepository.findAllByDeletedFalse(pageable)).thenReturn(productPage);
        when(productMapper.toResponse(product)).thenReturn(productResponse);

        // Act
        Page<ProductResponse> result = productService.getAllProducts(page, size);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test Product", result.getContent().get(0).getName());
    }

    @Test
    void searchProductsByName_Success_ReturnsProductResponses() {
        // Arrange
        String name = "Test";
        Product product = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(new BigDecimal("100.00"))
                .build();
        ProductResponse productResponse = ProductResponse.builder()
                .id(1L)
                .name("Test Product")
                .price(new BigDecimal("100.00"))
                .build();

        when(productRepository.findByNameContainingIgnoreCaseAndDeletedFalse(name))
                .thenReturn(Collections.singletonList(product));
        when(productMapper.toResponse(product)).thenReturn(productResponse);

        // Act
        List<ProductResponse> result = productService.searchProductsByName(name);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Product", result.get(0).getName());
    }

    @Test
    void getProductsByCategory_Success_ReturnsPagedProductResponses() {
        // Arrange
        Long categoryId = 1L;
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Product product = Product.builder()
                .id(1L)
                .name("Test Product")
                .category(Category.builder().id(categoryId).build())
                .build();
        ProductResponse productResponse = ProductResponse.builder()
                .id(1L)
                .name("Test Product")
                .categoryId(categoryId)
                .build();
        Page<Product> productPage = new PageImpl<>(Collections.singletonList(product), pageable, 1);

        when(productRepository.findByCategoryIdAndDeletedFalse(categoryId, pageable)).thenReturn(productPage);
        when(productMapper.toResponse(product)).thenReturn(productResponse);

        // Act
        Page<ProductResponse> result = productService.getProductsByCategory(categoryId, page, size);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test Product", result.getContent().get(0).getName());
        assertEquals(categoryId, result.getContent().get(0).getCategoryId());
    }

//    @Test
//    void getProductsByCompany_Success_ReturnsPagedProductResponses() {
//        // Arrange
//        Long companyId = 1L;
//        int page = 0;
//        int size = 10;
//        Pageable pageable = PageRequest.of(page, size);
//        Product product = Product.builder()
//                .id(1L)
//                .name("Test Product")
//                .company(Company.builder().id(companyId).build())
//                .build();
//        ProductResponse productResponse = ProductResponse.builder()
//                .id(1L)
//                .name("Test Product")
//                .companyId(companyId)
//                .build();
//        Page<Product> productPage = new PageImpl<>(Collections.singletonList(product), pageable, 1);
//
//        when(productRepository.findByCompanyIdAndDeletedFalse(companyId, pageable)).thenReturn(productPage);
//        when(productMapper.toResponse(product)).thenReturn(productResponse);
//
//        // Act
//        Page<ProductResponse> result = productService.getProductsByCompany(companyId, page, size);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.getTotalElements());
//        assertEquals("Test Product", result.getContent().get(0).getName());
//        assertEquals(companyId, result.getContent().get(0).getCompanyId());
//    }
}

 */