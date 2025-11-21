/*package com.datasaz.ecommerce.controllers;


import com.datasaz.ecommerce.configs.GroupConfig;
import com.datasaz.ecommerce.exceptions.ResourceNotFoundException;
import com.datasaz.ecommerce.models.response.ProductImageResponse;
import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.repositories.ProductImageAttachRepository;
import com.datasaz.ecommerce.repositories.ProductImageRepository;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.entities.ProductImage;
import com.datasaz.ecommerce.repositories.entities.ProductImageAttach;
import com.datasaz.ecommerce.services.interfaces.IProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
class ProductControllerTest {

    @Autowired
    private ProductController productController;

    @MockBean
    private IProductService productService;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private ProductImageRepository productImageRepository;

    @MockBean
    private ProductImageAttachRepository productImageAttachRepository;

    @MockBean
    private GroupConfig groupConfig;

    @Test
    void getProductById_Success_ReturnsProductResponse() {
        // Arrange
        Long productId = 1L;
        ProductResponse productResponse = ProductResponse.builder()
                .id(productId)
                .name("Test Product")
                .price(new BigDecimal("100.00"))
                .build();

        when(productService.getProductById(productId)).thenReturn(productResponse);

        // Act
        ResponseEntity<ProductResponse> response = productController.getProductById(productId);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Test Product", response.getBody().getName());
    }

    @Test
    void getAllProducts_Success_ReturnsPagedProducts() {
        // Arrange
        int page = 0;
        int size = 10;
        ProductResponse productResponse = ProductResponse.builder()
                .id(1L)
                .name("Test Product")
                .build();
        Page<ProductResponse> productPage = new PageImpl<>(Collections.singletonList(productResponse));

        when(productService.getAllProducts(page, size)).thenReturn(productPage);

        // Act
        ResponseEntity<Page<ProductResponse>> response = productController.getAllProducts(page, size);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals("Test Product", response.getBody().getContent().get(0).getName());
    }

    @Test
    void searchProductsByName_Success_ReturnsProductResponses() {
        // Arrange
        String name = "Test";
        ProductResponse productResponse = ProductResponse.builder()
                .id(1L)
                .name("Test Product")
                .build();

        when(productService.searchProductsByName(name)).thenReturn(Collections.singletonList(productResponse));

        // Act
        ResponseEntity<List<ProductResponse>> response = productController.searchProductsByName(name);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals("Test Product", response.getBody().get(0).getName());
    }

    @Test
    void getProductsByCategory_Success_ReturnsPagedProducts() {
        // Arrange
        Long categoryId = 1L;
        int page = 0;
        int size = 10;
        ProductResponse productResponse = ProductResponse.builder()
                .id(1L)
                .name("Test Product")
                .categoryId(categoryId)
                .build();
        Page<ProductResponse> productPage = new PageImpl<>(Collections.singletonList(productResponse));

        when(productService.getProductsByCategory(categoryId, page, size)).thenReturn(productPage);

        // Act
        ResponseEntity<Page<ProductResponse>> response = productController.getProductsByCategory(categoryId, page, size);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals("Test Product", response.getBody().getContent().get(0).getName());
    }

//    @Test
//    void getProductsByCompany_Success_ReturnsPagedProducts() {
//        // Arrange
//        Long companyId = 1L;
//        int page = 0;
//        int size = 10;
//        ProductResponse productResponse = ProductResponse.builder()
//                .id(1L)
//                .name("Test Product")
//                .companyId(companyId)
//                .build();
//        Page<ProductResponse> productPage = new PageImpl<>(Collections.singletonList(productResponse));
//
//        when(productService.getProductsByCompany(companyId, page, size)).thenReturn(productPage);
//
//        // Act
//        ResponseEntity<Page<ProductResponse>> response = productController.getProductsByCompany(companyId, page, size);
//
//        // Assert
//        assertEquals(200, response.getStatusCodeValue());
//        assertEquals(1, response.getBody().getTotalElements());
//        assertEquals("Test Product", response.getBody().getContent().get(0).getName());
//    }

    @Test
    void getProductPrimaryImage_DatabaseMode_Success_ReturnsImageResource() {
        // Arrange
        Long productId = 1L;
        ProductImageAttach image = ProductImageAttach.builder()
                .id(1L)
                .fileName("test.jpg")
                .contentType("image/jpeg")
                .fileContent(new byte[]{1, 2, 3})
                .isPrimary(true)
                .build();

        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "database");
        when(productImageAttachRepository.findByProductIdAndIsPrimaryTrue(productId)).thenReturn(Optional.of(image));

        // Act
        ResponseEntity<?> response = productController.getProductPrimaryImage(productId);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof ByteArrayResource);
        assertEquals("image/jpeg", response.getHeaders().getContentType().toString());
        assertEquals("inline; filename=\"test.jpg\"", response.getHeaders().getFirst("Content-Disposition"));
    }

    @Test
    void getProductPrimaryImage_FileMode_Success_ReturnsProductImageResponse() {
        // Arrange
        Long productId = 1L;
        ProductImage image = ProductImage.builder()
                .id(1L)
                .fileName("test.jpg")
                .fileUrl("http://example.com/test.jpg")
                .contentType("image/jpeg")
                .isPrimary(true)
                .build();
        ProductImageResponse imageResponse = ProductImageResponse.builder()
                .id(1L)
                .fileName("test.jpg")
                .fileUrl("http://example.com/test.jpg")
                .contentType("image/jpeg")
                .isPrimary(true)
                .build();

        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "file");
        when(productImageRepository.findByProductIdAndIsPrimaryTrue(productId)).thenReturn(Optional.of(image));

        // Act
        ResponseEntity<?> response = productController.getProductPrimaryImage(productId);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof ProductImageResponse);
        assertEquals("test.jpg", ((ProductImageResponse) response.getBody()).getFileName());
    }

    @Test
    void getProductPrimaryImage_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        Long productId = 1L;
        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "database");
        when(productImageAttachRepository.findByProductIdAndIsPrimaryTrue(productId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> productController.getProductPrimaryImage(productId));
        assertEquals("Primary image not found for product ID: " + productId, exception.getMessage());
    }
}

 */