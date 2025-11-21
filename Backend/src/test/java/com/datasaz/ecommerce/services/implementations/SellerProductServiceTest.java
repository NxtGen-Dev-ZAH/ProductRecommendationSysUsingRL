package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.configs.GroupConfig;
import com.datasaz.ecommerce.exceptions.*;
import com.datasaz.ecommerce.exceptions.response.ExceptionMessages;
import com.datasaz.ecommerce.mappers.ProductMapper;
import com.datasaz.ecommerce.models.request.ProductRequest;
import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.repositories.*;
import com.datasaz.ecommerce.repositories.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SellerProductServiceTest {

    @InjectMocks
    private SellerProductService sellerProductService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductImageService productImageService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private CompanyAdminRightsRepository companyAdminRightsRepository;

    @Spy
    private GroupConfig groupConfig = new GroupConfig();

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private User seller;
    private User admin;
    private Product product;
    private ProductResponse productResponse;
    private ProductRequest productRequest;
    private Category category;
    private Company company;

    @BeforeEach
    void setUp() {
        groupConfig.maxFileCountPerProduct = 5;
        groupConfig.imageStorageMode = "database";

        seller = User.builder()
                .id(1L)
                .emailAddress("seller@test.com")
                .userRoles(Set.of(Roles.builder().role(RoleTypes.SELLER).build()))
                .deleted(false)
                .build();

        admin = User.builder()
                .id(2L)
                .emailAddress("admin@test.com")
                .userRoles(Set.of(Roles.builder().role(RoleTypes.SELLER).build()))
                .deleted(false)
                .build();

        company = Company.builder().id(1L).deleted(false).build();
        category = Category.builder().id(1L).build();

        product = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(new BigDecimal("100.00"))
                .quantity(10)
                .author(seller)
                .category(category)
                .createdAt(LocalDateTime.now())
                .deleted(false)
                .productStatus(ProductStatus.AVAILABLE)
                .imageAttaches(new ArrayList<>())
                .images(new ArrayList<>())
                .build();

        productResponse = ProductResponse.builder()
                .id(1L)
                .name("Test Product")
                .price(new BigDecimal("100.00"))
                .quantity(10)
                .authorId(1L)
                .categoryId(1L)
                .build();

        productRequest = ProductRequest.builder()
                .name("Test Product")
                .price(new BigDecimal("100.00"))
                .quantity(10)
                .categoryId(1L)
                .build();
    }

    @Test
    void createProduct_SuccessWithNoImages_ReturnsProductResponse() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            when(userRepository.findByEmailAddressAndDeletedFalse("seller@test.com")).thenReturn(Optional.of(seller));
            when(productMapper.toEntity(productRequest, seller)).thenReturn(product);
            lenient().when(productRepository.save(any(Product.class))).thenReturn(product);
            lenient().when(productMapper.toResponse(any(Product.class))).thenReturn(productResponse);

            ProductResponse result = sellerProductService.createProduct(productRequest, Collections.emptyList(), "seller@test.com");

            assertNotNull(result);
            assertEquals("Test Product", result.getName());
            assertEquals(new BigDecimal("100.00"), result.getPrice());
            assertEquals(10, result.getQuantity());
            verify(auditLogService).logAction("seller@test.com", "CREATE_PRODUCT", "Product: Test Product, Images: 0");
            verify(productRepository, times(2)).save(any(Product.class));
        }
    }

    @Test
    void createProduct_WithImagesDatabaseMode_Success() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            MultipartFile image = mock(MultipartFile.class);
            ProductImageAttach imageAttach = ProductImageAttach.builder()
                    .id(1L)
                    .product(product)
                    .fileName("test.jpg")
                    .fileSize(1000L)
                    .fileExtension("jpg")
                    .contentType("image/jpeg")
                    .createdAt(LocalDateTime.now())
                    .isPrimary(true)
                    .displayOrder(0)
                    .build();

            when(userRepository.findByEmailAddressAndDeletedFalse("seller@test.com")).thenReturn(Optional.of(seller));
            when(productMapper.toEntity(productRequest, seller)).thenReturn(product);
            lenient().when(productRepository.save(any(Product.class))).thenReturn(product);
            lenient().when(productMapper.toResponse(any(Product.class))).thenReturn(productResponse);
            when(productImageService.uploadImageAttach(eq(1L), any(MultipartFile.class), eq(true))).thenReturn(imageAttach);

            ProductResponse result = sellerProductService.createProduct(productRequest, List.of(image), "seller@test.com");

            assertNotNull(result);
            assertEquals("Test Product", result.getName());
            verify(productImageService).uploadImageAttach(eq(1L), any(MultipartFile.class), eq(true));
            verify(auditLogService).logAction("seller@test.com", "CREATE_PRODUCT", "Product: Test Product, Images: 1");
            verify(productRepository, times(2)).save(any(Product.class));
        }
    }

    @Test
    void createProduct_WithImagesNonDatabaseMode_Success() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            groupConfig.imageStorageMode = "filesystem";
            MultipartFile image = mock(MultipartFile.class);
            ProductImage imageEntity = ProductImage.builder()
                    .id(1L)
                    .product(product)
                    .fileName("test.jpg")
                    .fileUrl("http://localhost/test.jpg")
                    .fileSize(1000L)
                    .fileExtension("jpg")
                    .contentType("image/jpeg")
                    .createdAt(LocalDateTime.now())
                    .isPrimary(true)
                    .displayOrder(0)
                    .build();

            when(userRepository.findByEmailAddressAndDeletedFalse("seller@test.com")).thenReturn(Optional.of(seller));
            when(productMapper.toEntity(productRequest, seller)).thenReturn(product);
            lenient().when(productRepository.save(any(Product.class))).thenReturn(product);
            lenient().when(productMapper.toResponse(any(Product.class))).thenReturn(productResponse);
            when(productImageService.uploadImage(eq(1L), any(MultipartFile.class), eq(true))).thenReturn(imageEntity);

            ProductResponse result = sellerProductService.createProduct(productRequest, List.of(image), "seller@test.com");

            assertNotNull(result);
            assertEquals("Test Product", result.getName());
            verify(productImageService).uploadImage(eq(1L), any(MultipartFile.class), eq(true));
            verify(auditLogService).logAction("seller@test.com", "CREATE_PRODUCT", "Product: Test Product, Images: 1");
            verify(productRepository, times(2)).save(any(Product.class));
        }
    }

    @Test
    void createProduct_UserNotFound_ThrowsUserNotFoundException() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            when(userRepository.findByEmailAddressAndDeletedFalse("seller@test.com")).thenReturn(Optional.empty());

            UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                    () -> sellerProductService.createProduct(productRequest, Collections.emptyList(), "seller@test.com"));
            assertEquals("User not found with email: seller@test.com", exception.getMessage());
        }
    }

    @Test
    void createProduct_NotSeller_ThrowsUnauthorizedException() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            User nonSeller = User.builder()
                    .id(1L)
                    .emailAddress("seller@test.com")
                    .userRoles(Collections.emptySet())
                    .deleted(false)
                    .build();
            when(userRepository.findByEmailAddressAndDeletedFalse("seller@test.com")).thenReturn(Optional.of(nonSeller));

            UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                    () -> sellerProductService.createProduct(productRequest, Collections.emptyList(), "seller@test.com"));
            assertEquals("User is not authorized to create product", exception.getMessage());
        }
    }

    @Test
    void createProduct_TooManyImages_ThrowsBadRequestException() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            when(userRepository.findByEmailAddressAndDeletedFalse("seller@test.com")).thenReturn(Optional.of(seller));
            MultipartFile image = mock(MultipartFile.class);

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> sellerProductService.createProduct(productRequest, List.of(image, image, image, image, image, image), "seller@test.com"));
            assertEquals("Maximum 5 images allowed per product", exception.getMessage());
        }
    }

    @Test
    void saveProduct_Success_ReturnsProductResponse() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            when(userRepository.findByEmailAddress("seller@test.com")).thenReturn(Optional.of(seller));
            when(productMapper.toEntity(productRequest, seller)).thenReturn(product);
            when(productRepository.save(any(Product.class))).thenReturn(product);
            when(productMapper.toResponse(any(Product.class))).thenReturn(productResponse);

            ProductResponse result = sellerProductService.saveProduct(productRequest, Collections.emptyList());

            assertNotNull(result);
            assertEquals("Test Product", result.getName());
            assertEquals(new BigDecimal("100.00"), result.getPrice());
            assertEquals(10, result.getQuantity());
            verify(auditLogService).logAction("seller@test.com", "SAVE_PRODUCT", "Product: Test Product, Images: 0");
            verify(productRepository, times(2)).save(any(Product.class));
        }
    }

    @Test
    void saveProduct_ZeroQuantity_SetsOutOfStock() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            ProductRequest zeroQtyRequest = ProductRequest.builder()
                    .name("Test Product")
                    .price(new BigDecimal("100.00"))
                    .quantity(0)
                    .categoryId(1L)
                    .build();
            Product zeroQtyProduct = Product.builder()
                    .id(1L)
                    .name("Test Product")
                    .price(new BigDecimal("100.00"))
                    .quantity(0)
                    .author(seller)
                    .category(category)
                    .createdAt(LocalDateTime.now())
                    .deleted(false)
                    .productStatus(ProductStatus.OUT_OF_STOCK)
                    .imageAttaches(new ArrayList<>())
                    .images(new ArrayList<>())
                    .build();
            ProductResponse zeroQtyResponse = ProductResponse.builder()
                    .id(1L)
                    .name("Test Product")
                    .price(new BigDecimal("100.00"))
                    .quantity(0)
                    .authorId(1L)
                    .categoryId(1L)
                    .build();

            when(userRepository.findByEmailAddress("seller@test.com")).thenReturn(Optional.of(seller));
            when(productMapper.toEntity(zeroQtyRequest, seller)).thenReturn(zeroQtyProduct);
            when(productRepository.save(any(Product.class))).thenReturn(zeroQtyProduct);
            when(productMapper.toResponse(any(Product.class))).thenReturn(zeroQtyResponse);

            ProductResponse result = sellerProductService.saveProduct(zeroQtyRequest, Collections.emptyList());

            assertNotNull(result);
            assertEquals(0, result.getQuantity());
            verify(productRepository, times(2)).save(any(Product.class));
        }
    }

    @Test
    void updateProduct_Success_ReturnsUpdatedProductResponse() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            ProductRequest updateRequest = ProductRequest.builder()
                    .name("Updated Product")
                    .price(new BigDecimal("150.00"))
                    .quantity(20)
                    .categoryId(2L)
                    .build();
            Product updatedProduct = Product.builder()
                    .id(1L)
                    .name("Updated Product")
                    .price(new BigDecimal("150.00"))
                    .quantity(20)
                    .author(seller)
                    .category(Category.builder().id(2L).build())
                    .productStatus(ProductStatus.AVAILABLE)
                    .imageAttaches(new ArrayList<>())
                    .images(new ArrayList<>())
                    .build();
            ProductResponse updatedResponse = ProductResponse.builder()
                    .id(1L)
                    .name("Updated Product")
                    .price(new BigDecimal("150.00"))
                    .quantity(20)
                    .authorId(1L)
                    .categoryId(2L)
                    .build();

            when(userRepository.findByEmailAddressAndDeletedFalse("seller@test.com")).thenReturn(Optional.of(seller));
            when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
            when(categoryRepository.findById(2L)).thenReturn(Optional.of(Category.builder().id(2L).build()));
            when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);
            when(productMapper.toResponse(any(Product.class))).thenReturn(updatedResponse);

            ProductResponse result = sellerProductService.updateProduct(1L, updateRequest, Collections.emptyList(), Collections.emptyList(), null, "seller@test.com");

            assertNotNull(result);
            assertEquals("Updated Product", result.getName());
            assertEquals(new BigDecimal("150.00"), result.getPrice());
            assertEquals(20, result.getQuantity());
            verify(auditLogService).logAction("seller@test.com", "UPDATE_PRODUCT", "Product ID: 1, Images updated: 0, Images removed: 0");
            verify(productRepository).save(argThat(p -> p.getProductStatus() == ProductStatus.AVAILABLE));
        }
    }

    @Test
    void updateProduct_ZeroQuantity_SetsOutOfStock() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            ProductRequest updateRequest = ProductRequest.builder()
                    .name("Updated Product")
                    .price(new BigDecimal("150.00"))
                    .quantity(0)
                    .categoryId(2L)
                    .build();
            Product updatedProduct = Product.builder()
                    .id(1L)
                    .name("Updated Product")
                    .price(new BigDecimal("150.00"))
                    .quantity(0)
                    .author(seller)
                    .category(Category.builder().id(2L).build())
                    .productStatus(ProductStatus.OUT_OF_STOCK)
                    .imageAttaches(new ArrayList<>())
                    .images(new ArrayList<>())
                    .build();
            ProductResponse updatedResponse = ProductResponse.builder()
                    .id(1L)
                    .name("Updated Product")
                    .price(new BigDecimal("150.00"))
                    .quantity(0)
                    .authorId(1L)
                    .categoryId(2L)
                    .build();

            when(userRepository.findByEmailAddressAndDeletedFalse("seller@test.com")).thenReturn(Optional.of(seller));
            when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
            when(categoryRepository.findById(2L)).thenReturn(Optional.of(Category.builder().id(2L).build()));
            when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);
            when(productMapper.toResponse(any(Product.class))).thenReturn(updatedResponse);

            ProductResponse result = sellerProductService.updateProduct(1L, updateRequest, Collections.emptyList(), Collections.emptyList(), null, "seller@test.com");

            assertNotNull(result);
            assertEquals(0, result.getQuantity());
            verify(productRepository).save(argThat(p -> p.getProductStatus() == ProductStatus.OUT_OF_STOCK));
        }
    }

    @Test
    void updateProduct_InvalidCategory_ThrowsBadRequestException() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            ProductRequest updateRequest = ProductRequest.builder()
                    .name("Updated Product")
                    .price(new BigDecimal("150.00"))
                    .quantity(20)
                    .categoryId(2L)
                    .build();

            when(userRepository.findByEmailAddressAndDeletedFalse("seller@test.com")).thenReturn(Optional.of(seller));
            when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
            when(categoryRepository.findById(2L)).thenReturn(Optional.empty());

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> sellerProductService.updateProduct(1L, updateRequest, Collections.emptyList(), Collections.emptyList(), null, "seller@test.com"));
            assertEquals("Category not found with ID: 2", exception.getMessage());
        }
    }

    @Test
    void updateProduct_NotFound_ThrowsProductNotFoundException() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            when(userRepository.findByEmailAddressAndDeletedFalse("seller@test.com")).thenReturn(Optional.of(seller));
            when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

            ProductNotFoundException exception = assertThrows(ProductNotFoundException.class,
                    () -> sellerProductService.updateProduct(1L, productRequest, Collections.emptyList(), Collections.emptyList(), null, "seller@test.com"));
            assertEquals("Product not found with id: 1", exception.getMessage());
        }
    }

    @Test
    void updateProduct_Unauthorized_ThrowsUnauthorizedException() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("other@test.com");

            User unauthorizedUser = User.builder()
                    .id(2L)
                    .emailAddress("other@test.com")
                    .userRoles(Set.of(Roles.builder().role(RoleTypes.SELLER).build()))
                    .deleted(false)
                    .build();
            when(userRepository.findByEmailAddressAndDeletedFalse("other@test.com")).thenReturn(Optional.of(unauthorizedUser));
            when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));

            UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                    () -> sellerProductService.updateProduct(1L, productRequest, Collections.emptyList(), Collections.emptyList(), null, "other@test.com"));
            assertEquals("User is not authorized to update this product", exception.getMessage());
        }
    }

    @Test
    void updateProductImages_SuccessWithDatabaseMode_ReturnsUpdatedProductResponse() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            MultipartFile image = mock(MultipartFile.class);
            ProductImageAttach imageAttach = ProductImageAttach.builder()
                    .id(2L)
                    .product(product)
                    .fileName("test.jpg")
                    .fileSize(1000L)
                    .fileExtension("jpg")
                    .contentType("image/jpeg")
                    .createdAt(LocalDateTime.now())
                    .isPrimary(true)
                    .displayOrder(0)
                    .build();
            product.setImageAttaches(new ArrayList<>());

            when(userRepository.findByEmailAddressAndDeletedFalse("seller@test.com")).thenReturn(Optional.of(seller));
            when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
            when(productImageService.uploadImageAttach(eq(1L), any(MultipartFile.class), eq(true))).thenReturn(imageAttach);
            when(productRepository.save(any(Product.class))).thenReturn(product);
            when(productMapper.toResponse(any(Product.class))).thenReturn(productResponse);

            ProductResponse result = sellerProductService.updateProductImages(1L, List.of(image), Collections.emptyList(), null, "seller@test.com");

            assertNotNull(result);
            verify(productImageService).uploadImageAttach(eq(1L), any(MultipartFile.class), eq(true));
            verify(auditLogService).logAction("seller@test.com", "UPDATE_PRODUCT_IMAGES", "Product ID: 1, Images updated: 1, Images removed: 0");
        }
    }

    @Test
    void updateProductImages_SuccessWithNonDatabaseMode_ReturnsUpdatedProductResponse() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            groupConfig.imageStorageMode = "filesystem";
            MultipartFile image = mock(MultipartFile.class);
            ProductImage imageEntity = ProductImage.builder()
                    .id(2L)
                    .product(product)
                    .fileName("test.jpg")
                    .fileUrl("http://localhost/test.jpg")
                    .fileSize(1000L)
                    .fileExtension("jpg")
                    .contentType("image/jpeg")
                    .createdAt(LocalDateTime.now())
                    .isPrimary(true)
                    .displayOrder(0)
                    .build();
            product.setImages(new ArrayList<>());

            when(userRepository.findByEmailAddressAndDeletedFalse("seller@test.com")).thenReturn(Optional.of(seller));
            when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
            when(productImageService.uploadImage(eq(1L), any(MultipartFile.class), eq(true))).thenReturn(imageEntity);
            when(productRepository.save(any(Product.class))).thenReturn(product);
            when(productMapper.toResponse(any(Product.class))).thenReturn(productResponse);

            ProductResponse result = sellerProductService.updateProductImages(1L, List.of(image), Collections.emptyList(), null, "seller@test.com");

            assertNotNull(result);
            verify(productImageService).uploadImage(eq(1L), any(MultipartFile.class), eq(true));
            verify(auditLogService).logAction("seller@test.com", "UPDATE_PRODUCT_IMAGES", "Product ID: 1, Images updated: 1, Images removed: 0");
        }
    }

    @Test
    void updateProductImages_TooManyImages_ThrowsBadRequestException() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            ProductImageAttach existingImage = ProductImageAttach.builder().id(1L).build();
            product.setImageAttaches(new ArrayList<>(List.of(existingImage)));
            MultipartFile image = mock(MultipartFile.class);

            when(userRepository.findByEmailAddressAndDeletedFalse("seller@test.com")).thenReturn(Optional.of(seller));
            when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> sellerProductService.updateProductImages(1L, List.of(image, image, image, image, image), Collections.emptyList(), null, "seller@test.com"));
            assertEquals("Maximum 5 images allowed per product", exception.getMessage());
        }
    }

    @Test
    void deleteProduct_Success_SoftDeletesProduct() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            when(userRepository.findByEmailAddressAndDeletedFalse("seller@test.com")).thenReturn(Optional.of(seller));
            when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
            when(productRepository.save(any(Product.class))).thenReturn(product);

            sellerProductService.deleteProduct(1L, "seller@test.com");

            verify(productRepository).save(argThat(p -> p.isDeleted()));
            verify(auditLogService).logAction("seller@test.com", "DELETE_PRODUCT", "Product ID: 1");
        }
    }

    @Test
    void deleteProduct_Unauthorized_ThrowsUnauthorizedException() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("other@test.com");

            User unauthorizedUser = User.builder()
                    .id(2L)
                    .emailAddress("other@test.com")
                    .userRoles(Set.of(Roles.builder().role(RoleTypes.SELLER).build()))
                    .deleted(false)
                    .build();
            when(userRepository.findByEmailAddressAndDeletedFalse("other@test.com")).thenReturn(Optional.of(unauthorizedUser));
            when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));

            UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                    () -> sellerProductService.deleteProduct(1L, "other@test.com"));
            assertEquals("User is not authorized to delete this product", exception.getMessage());
        }
    }

    @Test
    void findProductsByNameAuthorIdDeletedFalse_Success_ReturnsProductResponses() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            when(userRepository.findByEmailAddress("seller@test.com")).thenReturn(Optional.of(seller));
            when(productRepository.findByNameContainingIgnoreCaseAuthorIdDeletedFalse("Test", 1L)).thenReturn(List.of(product));
            when(productMapper.toResponse(any(Product.class))).thenReturn(productResponse);

            List<ProductResponse> result = sellerProductService.findProductsByNameAuthorIdDeletedFalse("Test");

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("Test Product", result.get(0).getName());
        }
    }

    @Test
    void findProductsByNameAuthorIdDeletedFalse_Paged_Success_ReturnsPagedProductResponses() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> productPage = new PageImpl<>(List.of(product), pageable, 1);
            when(userRepository.findByEmailAddress("seller@test.com")).thenReturn(Optional.of(seller));
            when(productRepository.findByNameContainingIgnoreCaseAuthorIdDeletedFalse("Test", 1L, pageable)).thenReturn(productPage);
            when(productMapper.toResponse(any(Product.class))).thenReturn(productResponse);

            Page<ProductResponse> result = sellerProductService.findProductsByNameAuthorIdDeletedFalse("Test", pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals("Test Product", result.getContent().get(0).getName());
        }
    }

    @Test
    void updateProductQuantity_Success_ReturnsUpdatedProductResponse() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            Product updatedProduct = Product.builder()
                    .id(1L)
                    .name("Test Product")
                    .price(new BigDecimal("100.00"))
                    .quantity(50)
                    .author(seller)
                    .category(category)
                    .createdAt(LocalDateTime.now())
                    .deleted(false)
                    .productStatus(ProductStatus.AVAILABLE)
                    .imageAttaches(new ArrayList<>())
                    .images(new ArrayList<>())
                    .build();
            ProductResponse updatedResponse = ProductResponse.builder()
                    .id(1L)
                    .name("Test Product")
                    .price(new BigDecimal("100.00"))
                    .quantity(50)
                    .authorId(1L)
                    .categoryId(1L)
                    .build();

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);
            when(productMapper.toResponse(any(Product.class))).thenReturn(updatedResponse);

            ProductResponse result = sellerProductService.updateProductQuantity(1L, 50);

            assertNotNull(result);
            assertEquals(50, result.getQuantity());
            verify(productRepository).save(argThat(p -> p.getQuantity() == 50 && p.getProductStatus() == ProductStatus.AVAILABLE));
        }
    }

    @Test
    void updateProductQuantity_ZeroQuantity_SetsOutOfStock() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            Product updatedProduct = Product.builder()
                    .id(1L)
                    .name("Test Product")
                    .price(new BigDecimal("100.00"))
                    .quantity(0)
                    .author(seller)
                    .category(category)
                    .createdAt(LocalDateTime.now())
                    .deleted(false)
                    .productStatus(ProductStatus.OUT_OF_STOCK)
                    .imageAttaches(new ArrayList<>())
                    .images(new ArrayList<>())
                    .build();
            ProductResponse updatedResponse = ProductResponse.builder()
                    .id(1L)
                    .name("Test Product")
                    .price(new BigDecimal("100.00"))
                    .quantity(0)
                    .authorId(1L)
                    .categoryId(1L)
                    .build();

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);
            when(productMapper.toResponse(any(Product.class))).thenReturn(updatedResponse);

            ProductResponse result = sellerProductService.updateProductQuantity(1L, 0);

            assertNotNull(result);
            assertEquals(0, result.getQuantity());
            verify(productRepository).save(argThat(p -> p.getQuantity() == 0 && p.getProductStatus() == ProductStatus.OUT_OF_STOCK));
        }
    }

    @Test
    void updateProductQuantity_NotFound_ThrowsProductNotFoundException() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            when(productRepository.findById(1L)).thenReturn(Optional.empty());

            ProductNotFoundException exception = assertThrows(ProductNotFoundException.class,
                    () -> sellerProductService.updateProductQuantity(1L, 50));
            assertEquals("Product not Found.", exception.getMessage());
        }
    }

    @Test
    void updateProductPrice_Success_ReturnsUpdatedProductResponse() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            Product updatedProduct = Product.builder()
                    .id(1L)
                    .name("Test Product")
                    .price(new BigDecimal("200.00"))
                    .quantity(10)
                    .author(seller)
                    .category(category)
                    .createdAt(LocalDateTime.now())
                    .deleted(false)
                    .productStatus(ProductStatus.AVAILABLE)
                    .imageAttaches(new ArrayList<>())
                    .images(new ArrayList<>())
                    .build();
            ProductResponse updatedResponse = ProductResponse.builder()
                    .id(1L)
                    .name("Test Product")
                    .price(new BigDecimal("200.00"))
                    .quantity(10)
                    .authorId(1L)
                    .categoryId(1L)
                    .build();

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);
            when(productMapper.toResponse(any(Product.class))).thenReturn(updatedResponse);

            ProductResponse result = sellerProductService.updateProductPrice(1L, new BigDecimal("200.00"));

            assertNotNull(result);
            assertEquals(new BigDecimal("200.00"), result.getPrice());
            verify(productRepository).save(argThat(p -> p.getPrice().equals(new BigDecimal("200.00"))));
        }
    }

    @Test
    void updateProductPrice_NegativePrice_ThrowsIllegalArgumentException() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> sellerProductService.updateProductPrice(1L, new BigDecimal("-10.00")));
            assertEquals("Invalid Price", exception.getMessage());
        }
    }

    @Test
    void getAuthorOrCompanyProducts_Author_Success_ReturnsPagedProductResponses() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> productPage = new PageImpl<>(List.of(product), pageable, 1);
            when(userRepository.findByEmailAddressAndDeletedFalse("seller@test.com")).thenReturn(Optional.of(seller));
            when(productRepository.findByAuthorIdAndDeletedFalse(1L, pageable)).thenReturn(productPage);
            when(productMapper.toResponse(any(Product.class))).thenReturn(productResponse);

            Page<ProductResponse> result = sellerProductService.getAuthorOrCompanyProducts(null, 0, 10);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals("Test Product", result.getContent().get(0).getName());
        }
    }

    @Test
    void getAuthorOrCompanyProducts_Company_Success_ReturnsPagedProductResponses() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> productPage = new PageImpl<>(List.of(product), pageable, 1);
            product.setCompany(company);
            when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
            when(productRepository.findByCompanyIdAndDeletedFalse(1L, pageable)).thenReturn(productPage);
            when(productMapper.toResponse(any(Product.class))).thenReturn(productResponse);

            Page<ProductResponse> result = sellerProductService.getAuthorOrCompanyProducts(1L, 0, 10);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals("Test Product", result.getContent().get(0).getName());
        }
    }

    @Test
    void mergeProductsToCompany_Success_UpdatesProducts() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("admin@test.com");

            seller.setCompany(company);
            User companyAdmin = User.builder()
                    .id(2L)
                    .emailAddress("admin@test.com")
                    .userRoles(Set.of(Roles.builder().role(RoleTypes.COMPANY_ADMIN_SELLER).build()))
                    .deleted(false)
                    .build();
            CompanyAdminRights adminRights = CompanyAdminRights.builder()
                    .company(company)
                    .user(companyAdmin)
                    .canAddRemoveSellers(true)
                    .build();

            when(userRepository.findByEmailAddressAndDeletedFalse("admin@test.com")).thenReturn(Optional.of(companyAdmin));
            when(userRepository.findByEmailAddressAndDeletedFalse("seller@test.com")).thenReturn(Optional.of(seller));
            when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
            when(companyAdminRightsRepository.findByCompanyIdAndUserId(eq(1L), eq(2L))).thenReturn(Optional.of(adminRights));
            doNothing().when(productRepository).updateCompanyForAuthorProducts(1L, company);

            sellerProductService.mergeProductsToCompany(1L, "seller@test.com", "admin@test.com");

            verify(productRepository).updateCompanyForAuthorProducts(1L, company);
            verify(auditLogService).logAction("seller@test.com", "MERGE_PRODUCTS_TO_COMPANY", "admin@test.com", "Company ID: 1");
        }
    }

    @Test
    void mergeProductsToCompany_UnauthorizedAdmin_ThrowsUnauthorizedException() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("admin@test.com");

            User unauthorizedAdmin = User.builder()
                    .id(2L)
                    .emailAddress("admin@test.com")
                    .userRoles(Collections.emptySet())
                    .deleted(false)
                    .build();

            when(userRepository.findByEmailAddressAndDeletedFalse("admin@test.com")).thenReturn(Optional.of(unauthorizedAdmin));
            when(userRepository.findByEmailAddressAndDeletedFalse("seller@test.com")).thenReturn(Optional.of(seller));
            when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

            UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                    () -> sellerProductService.mergeProductsToCompany(1L, "seller@test.com", "admin@test.com"));
            assertEquals("User lacks COMPANY_ADMIN_SELLER role", exception.getMessage());
        }
    }

    @Test
    void createProduct_Fallback_ThrowsRuntimeException() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            when(userRepository.findByEmailAddressAndDeletedFalse("seller@test.com")).thenThrow(new RuntimeException("Database error"));

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> sellerProductService.createProduct(productRequest, Collections.emptyList(), "seller@test.com"));
            assertTrue(exception.getMessage().contains("Database error"));
        }
    }

    @Test
    void createProduct_AdminRole_Success() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("admin@test.com");

            when(userRepository.findByEmailAddressAndDeletedFalse("admin@test.com")).thenReturn(Optional.of(admin));
            when(productMapper.toEntity(productRequest, admin)).thenReturn(product);
            when(productRepository.save(any(Product.class))).thenReturn(product);
            when(productMapper.toResponse(any(Product.class))).thenReturn(productResponse);

            ProductResponse result = sellerProductService.createProduct(productRequest, Collections.emptyList(), "admin@test.com");

            assertNotNull(result);
            assertEquals("Test Product", result.getName());
            verify(auditLogService).logAction("admin@test.com", "CREATE_PRODUCT", "Product: Test Product, Images: 0");
            verify(productRepository, times(2)).save(any(Product.class));
        }
    }


    @Test
    void getAllAuthorOrCompanyProducts_WithCompany_Success_ReturnsPagedProductResponses() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            seller.setCompany(company);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> productPage = new PageImpl<>(List.of(product), pageable, 1);

            when(userRepository.findByEmailAddressAndDeletedFalse("seller@test.com")).thenReturn(Optional.of(seller));
            when(productRepository.findByCompanyIdAndDeletedFalse(1L, pageable)).thenReturn(productPage);
            when(productMapper.toResponse(any(Product.class))).thenReturn(productResponse);

            Page<ProductResponse> result = sellerProductService.getAllAuthorOrCompanyProducts(0, 10);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals("Test Product", result.getContent().get(0).getName());
            verify(productRepository).findByCompanyIdAndDeletedFalse(1L, pageable);
            verify(productRepository, never()).findByAuthorIdAndDeletedFalse(anyLong(), any());
        }
    }

    @Test
    void getAllAuthorOrCompanyProducts_NoCompany_Success_ReturnsPagedProductResponses() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> productPage = new PageImpl<>(List.of(product), pageable, 1);

            when(userRepository.findByEmailAddressAndDeletedFalse("seller@test.com")).thenReturn(Optional.of(seller));
            when(productRepository.findByAuthorIdAndDeletedFalse(1L, pageable)).thenReturn(productPage);
            when(productMapper.toResponse(any(Product.class))).thenReturn(productResponse);

            Page<ProductResponse> result = sellerProductService.getAllAuthorOrCompanyProducts(0, 10);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals("Test Product", result.getContent().get(0).getName());
            verify(productRepository).findByAuthorIdAndDeletedFalse(1L, pageable);
            verify(productRepository, never()).findByCompanyIdAndDeletedFalse(anyLong(), any());
        }
    }

    @Test
    void getAllAuthorOrCompanyProducts_EmptyResults_ReturnsEmptyPage() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(userRepository.findByEmailAddressAndDeletedFalse("seller@test.com")).thenReturn(Optional.of(seller));
            when(productRepository.findByAuthorIdAndDeletedFalse(1L, pageable)).thenReturn(emptyPage);

            Page<ProductResponse> result = sellerProductService.getAllAuthorOrCompanyProducts(0, 10);

            assertNotNull(result);
            assertTrue(result.getContent().isEmpty());
            assertEquals(0, result.getTotalElements());
            verify(productRepository).findByAuthorIdAndDeletedFalse(1L, pageable);
            verify(productRepository, never()).findByCompanyIdAndDeletedFalse(anyLong(), any());
        }
    }

    @Test
    void getAllAuthorOrCompanyProducts_NegativePage_ThrowsIllegalParameterException() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            IllegalParameterException exception = assertThrows(IllegalParameterException.class,
                    () -> sellerProductService.getAllAuthorOrCompanyProducts(-1, 10));
            assertEquals("Page number cannot be negative", exception.getMessage());
            verify(productRepository, never()).findByCompanyIdAndDeletedFalse(anyLong(), any());
            verify(productRepository, never()).findByAuthorIdAndDeletedFalse(anyLong(), any());
        }
    }

    @Test
    void getAllAuthorOrCompanyProducts_ZeroSize_ThrowsIllegalParameterException() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            IllegalParameterException exception = assertThrows(IllegalParameterException.class,
                    () -> sellerProductService.getAllAuthorOrCompanyProducts(0, 0));
            assertEquals("Page size must be at least 1. 0", exception.getMessage());
            verify(productRepository, never()).findByCompanyIdAndDeletedFalse(anyLong(), any());
            verify(productRepository, never()).findByAuthorIdAndDeletedFalse(anyLong(), any());
        }
    }

    @Test
    void getAllAuthorOrCompanyProducts_NoAuthenticatedUser_ThrowsUnauthorizedException() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(null);

            UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                    () -> sellerProductService.getAllAuthorOrCompanyProducts(0, 10));
            assertEquals("No authenticated user", exception.getMessage());
            verify(userRepository, never()).findByEmailAddressAndDeletedFalse(anyString());
            verify(productRepository, never()).findByCompanyIdAndDeletedFalse(anyLong(), any());
            verify(productRepository, never()).findByAuthorIdAndDeletedFalse(anyLong(), any());
        }
    }

    @Test
    void getAllAuthorOrCompanyProducts_UserNotFound_ThrowsUserNotFoundException() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            when(userRepository.findByEmailAddressAndDeletedFalse("seller@test.com")).thenReturn(Optional.empty());

            UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                    () -> sellerProductService.getAllAuthorOrCompanyProducts(0, 10));
            assertEquals(ExceptionMessages.USER_NOT_FOUND + "Email: seller@test.com", exception.getMessage());
            verify(productRepository, never()).findByCompanyIdAndDeletedFalse(anyLong(), any());
            verify(productRepository, never()).findByAuthorIdAndDeletedFalse(anyLong(), any());
        }
    }

    @Test
    void getAllAuthorOrCompanyProducts_NonSeller_ThrowsUnauthorizedException() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("seller@test.com");

            User nonSeller = User.builder()
                    .id(1L)
                    .emailAddress("seller@test.com")
                    .userRoles(Collections.emptySet())
                    .deleted(false)
                    .build();
            when(userRepository.findByEmailAddressAndDeletedFalse("seller@test.com")).thenReturn(Optional.of(nonSeller));

            UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                    () -> sellerProductService.getAllAuthorOrCompanyProducts(0, 10));
            assertEquals("User is not a seller", exception.getMessage());
            verify(productRepository, never()).findByCompanyIdAndDeletedFalse(anyLong(), any());
            verify(productRepository, never()).findByAuthorIdAndDeletedFalse(anyLong(), any());
        }
    }
}
