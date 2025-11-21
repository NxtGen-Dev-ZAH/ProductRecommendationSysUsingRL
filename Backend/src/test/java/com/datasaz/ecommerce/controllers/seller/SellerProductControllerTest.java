/*package com.datasaz.ecommerce.controllers.seller;

import com.datasaz.ecommerce.configs.GroupConfig;
import com.datasaz.ecommerce.models.request.ProductRequest;
import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.services.interfaces.ISellerProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest
class SellerProductControllerTest {

    @Autowired
    private SellerProductController sellerProductController;

    @MockBean
    private ISellerProductService sellerProductService;

    @MockBean
    private GroupConfig groupConfig;

    @MockBean
    private OAuth2AuthorizedClientService authorizedClientService;

    @MockBean
    private JavaMailSender mailSender;


    @Test
    void createProduct_Success_ReturnsCreatedProductResponse() {
        // Arrange
        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "database");

        ProductRequest productRequest = ProductRequest.builder()
                .name("Test Product")
                .price(new BigDecimal("100.00"))
                .quantity(10)
                .categoryId(1L)
                .build();
        ProductResponse productResponse = ProductResponse.builder()
                .id(1L)
                .name("Test Product")
                .price(new BigDecimal("100.00"))
                .quantity(10)
                .build();
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("test@test.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(sellerProductService.createProduct(productRequest, Collections.emptyList(), "test@test.com"))
                .thenReturn(productResponse);

        // Act
        ResponseEntity<ProductResponse> response = sellerProductController.createProduct(productRequest, Collections.emptyList(), authentication);

        // Assert
        assertEquals(201, response.getStatusCodeValue());
        assertEquals("Test Product", response.getBody().getName());
    }

    @Test
    void updateProduct_Success_ReturnsUpdatedProductResponse() {
        // Arrange
        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "database");

        Long productId = 1L;
        ProductRequest productRequest = ProductRequest.builder()
                .name("Updated Product")
                .price(new BigDecimal("150.00"))
                .quantity(20)
                .categoryId(1L)
                .build();
        ProductResponse productResponse = ProductResponse.builder()
                .id(productId)
                .name("Updated Product")
                .price(new BigDecimal("150.00"))
                .quantity(20)
                .build();
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("test@test.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(sellerProductService.updateProduct(productId, productRequest, Collections.emptyList(), Collections.emptyList(), null, "test@test.com"))
                .thenReturn(productResponse);

        // Act
        ResponseEntity<ProductResponse> response = sellerProductController.updateProduct(productId, productRequest, Collections.emptyList(), Collections.emptyList(), null, authentication);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Updated Product", response.getBody().getName());
    }

    @Test
    void updateProductQuantity_Success_ReturnsUpdatedProductResponse() {
        // Arrange
        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "database");

        Long productId = 1L;
        int quantity = 50;
        ProductResponse productResponse = ProductResponse.builder()
                .id(productId)
                .name("Test Product")
                .quantity(quantity)
                .build();
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("test@test.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(sellerProductService.updateProductQuantity(productId, quantity)).thenReturn(productResponse);

        // Act
        ResponseEntity<ProductResponse> response = sellerProductController.updateProductQuantity(productId, quantity, authentication);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(quantity, response.getBody().getQuantity());
    }

    @Test
    void updateProductPrice_Success_ReturnsUpdatedProductResponse() {
        // Arrange
        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "database");

        Long productId = 1L;
        BigDecimal price = new BigDecimal("200.00");
        ProductResponse productResponse = ProductResponse.builder()
                .id(productId)
                .name("Test Product")
                .price(price)
                .build();
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("test@test.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(sellerProductService.updateProductPrice(productId, price)).thenReturn(productResponse);

        // Act
        ResponseEntity<ProductResponse> response = sellerProductController.updateProductPrice(productId, price, authentication);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(price, response.getBody().getPrice());
    }

//    @Test
//    void getProductById_Success_ReturnsProductResponse() {
//        // Arrange
//        Long productId = 1L;
//        ProductResponse productResponse = ProductResponse.builder()
//                .id(productId)
//                .name("Test Product")
//                .build();
//        Authentication authentication = mock(Authentication.class);
//        when(authentication.getName()).thenReturn("test@test.com");
//        SecurityContext securityContext = mock(SecurityContext.class);
//        when(securityContext.getAuthentication()).thenReturn(authentication);
//        SecurityContextHolder.setContext(securityContext);
//
//        when(sellerProductService.getProductById(productId)).thenReturn(productResponse);
//
//        // Act
//        ResponseEntity<ProductResponse> response = sellerProductController.getProductById(productId, authentication);
//
//        // Assert
//        assertEquals(200, response.getStatusCodeValue());
//        assertEquals("Test Product", response.getBody().getName());
//    }

    @Test
    void deleteProduct_Success_ReturnsNoContent() {
        // Arrange
        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "database");

        Long productId = 1L;
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("test@test.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        doNothing().when(sellerProductService).deleteProduct(productId, "test@test.com");

        // Act
        ResponseEntity<Void> response = sellerProductController.deleteProduct(productId, authentication);

        // Assert
        assertEquals(204, response.getStatusCodeValue());
    }

    @Test
    void findProductsByName_Success_ReturnsProductResponses() {
        // Arrange
        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "database");

        String name = "Test";
        ProductResponse productResponse = ProductResponse.builder()
                .id(1L)
                .name("Test Product")
                .build();
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("test@test.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(sellerProductService.findProductsByNameAuthorIdDeletedFalse(name))
                .thenReturn(Collections.singletonList(productResponse));

        // Act
        ResponseEntity<List<ProductResponse>> response = sellerProductController.findProductsByName(name, authentication);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals("Test Product", response.getBody().get(0).getName());
    }

    @Test
    void getAuthorOrCompanyProducts_Success_ReturnsPagedProducts() {
        // Arrange
        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "database");

        Long companyId = 1L;
        int page = 0;
        int size = 10;
        ProductResponse productResponse = ProductResponse.builder()
                .id(1L)
                .name("Test Product")
                .companyId(companyId)
                .build();
        Page<ProductResponse> productPage = new PageImpl<>(Collections.singletonList(productResponse));

        when(sellerProductService.getAuthorOrCompanyProducts(companyId, page, size)).thenReturn(productPage);

        // Act
        ResponseEntity<Page<ProductResponse>> response = sellerProductController.getAuthorOrCompanyProducts(companyId, page, size);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals("Test Product", response.getBody().getContent().get(0).getName());
    }
}*/


//package com.datasaz.ecommerce.controllers.seller;
//
//import com.datasaz.ecommerce.models.Request.ProductRequest;
//import com.datasaz.ecommerce.models.Response.ProductResponse;
//import com.datasaz.ecommerce.services.interfaces.ICategoryService;
//import com.datasaz.ecommerce.services.interfaces.IProductService;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.ResponseEntity;
//
//import java.util.List;
//
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//public class SellerProductControllerTest {
//    @Mock
//    private IProductService productService;
//
//    @Mock
//    private ICategoryService categoryService;
//
//
//
//    @InjectMocks
//    private SellerProductController sellerProductController;
//
//    @BeforeEach
//    void setUp() {
//        sellerProductController = new SellerProductController(productService, categoryService);
//
//    }
//
//    @Test
//    @DisplayName("Test getAllProducts")
//    void testGetAllProducts() {
//        // Given
//        // Mocking the productService to return a list of ProductResponse
//        ProductResponse productResponse = ProductResponse.builder().id(1L).name("Product 1").build();
//        List<ProductResponse> productResponses = List.of(productResponse);
//        when(productService.findAllProducts()).thenReturn(productResponses);
//
//        // When
//        List<ProductResponse> result = sellerProductController.getAllProducts();
//
//        // Then
//        Assertions.assertEquals(productResponses, result);
//    }
//    @Test
//    @DisplayName("Test getProductById")
//    void testGetProductById() {
//        // Given
//        Long productId = 1L;
//        ProductResponse productResponse = ProductResponse.builder().id(productId).name("Product 1").build();
//        when(productService.findProductById(productId)).thenReturn(productResponse);
//
//        // When
//        ResponseEntity<ProductResponse> result = sellerProductController.getProductById(productId);
//
//        // Then
//        Assertions.assertEquals(productResponse, result.getBody());
//    }
//    @Test
//    @DisplayName("Test addProduct")
//    void testAddProduct() {
//        ProductResponse productResponse = ProductResponse.builder().id(1L).name("Product 1").build();
//        ProductRequest productRequest = ProductRequest.builder().name("Product 1").build();
//        when(productService.saveProduct(productRequest)).thenReturn(productResponse);
//        // When
//        ResponseEntity<ProductResponse> result = sellerProductController.addProduct(productRequest);
//        Assertions.assertEquals(productResponse, result.getBody());
//    }
//
//    @Test
//    @DisplayName("Test updateProduct")
//    void testUpdateProduct() {
//        // Given
//        Long productId = 1L;
//        ProductRequest productRequest = ProductRequest.builder().name("Updated Product").build();
//        ProductResponse productResponse = ProductResponse.builder().id(productId).name("Updated Product").build();
//        when(productService.updateProduct(productId, productRequest)).thenReturn(productResponse);
//
//        // When
//        ResponseEntity<ProductResponse> result = sellerProductController.updateProduct(productId, productRequest);
//
//        // Then
//        Assertions.assertEquals(productResponse, result.getBody());
//    }
//    @Test
//    @DisplayName("Test deleteProduct")
//    void testDeleteProduct() {
//        // Given
//        Long productId = 1L;
//
//        // When
//        sellerProductController.deleteProduct(productId);
//
//        // Then
//        // Verify that the deleteById method was called with the correct product ID
//        productService.deleteProduct(productId);
//    }
//    @Test
//    @DisplayName("Test updateProductQuantity")
//    void testUpdateProductQuantity() {
//        // Given
//        Long productId = 1L;
//        int quantity = 10;
//        ProductResponse productResponse = ProductResponse.builder().id(productId).quantity(quantity).build();
//        when(productService.updateProductQuantity(productId, quantity)).thenReturn(productResponse);
//
//        // When
//        ProductResponse result = sellerProductController.updateProductQuantity(productId, quantity);
//
//        // Then
//        Assertions.assertEquals(productResponse, result);
//    }
//
//
//}
