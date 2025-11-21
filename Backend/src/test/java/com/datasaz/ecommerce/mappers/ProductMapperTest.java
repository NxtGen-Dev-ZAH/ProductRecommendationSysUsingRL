package com.datasaz.ecommerce.mappers;

import com.datasaz.ecommerce.configs.GroupConfig;
import com.datasaz.ecommerce.models.request.ProductRequest;
import com.datasaz.ecommerce.models.request.ProductVariantRequest;
import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.repositories.CategoryRepository;
import com.datasaz.ecommerce.repositories.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;


class ProductMapperTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private GroupConfig groupConfig;

    @InjectMocks
    private ProductMapper productMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(productMapper, "groupConfig", groupConfig);
        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "database");
    }

    @Test
    void toResponse_DatabaseMode_ReturnsImageAttaches() {
        // Mock GroupConfig to return "database" mode
        //when(groupConfig.imageStorageMode).thenReturn("database");
        //when(groupConfig.getImageStorageMode()).thenReturn("database");
        //String database = doReturn("database").when(groupConfig).imageStorageMode;

        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "database");

        Product product = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(new BigDecimal("100.00"))
                .quantity(10)
                .productStatus(ProductStatus.AVAILABLE)
                .productSellType(ProductSellType.DIRECT)
                .productCondition(ProductCondition.NEW)
                .createdAt(LocalDateTime.now())
                .author(User.builder().id(1L).emailAddress("test5@test.com").build())
                .category(Category.builder().id(1L).build())
                .imageAttaches(Collections.singletonList(
                        ProductImageAttach.builder()
                                .id(1L)
                                .fileName("test.jpg")
                                .contentType("image/jpeg")
                                .fileSize(1000)
                                .fileExtension("jpg")
                                .fileContent(new byte[]{1, 2, 3})
                                .thumbnailContent(new byte[]{4, 5, 6})
                                .isPrimary(true)
                                .displayOrder(0)
                                .createdAt(LocalDateTime.now())
                                .build()
                ))
                .images(Collections.emptyList())
                .build();

        ProductResponse response = productMapper.toResponse(product);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Test Product", response.getName());
        assertEquals(0, response.getImages().size());
        assertEquals(1, response.getImageAttaches().size());
        assertEquals("test.jpg", response.getImageAttaches().get(0).getFileName());
        assertEquals("image/jpeg", response.getImageAttaches().get(0).getContentType());
        assertEquals("AQID", response.getImageAttaches().get(0).getFileContent()); // Base64 encoded {1, 2, 3}
        assertEquals("BAUG", response.getImageAttaches().get(0).getThumbnailContent()); // Base64 encoded {4, 5, 6}
        assertTrue(response.getImageAttaches().get(0).isPrimary());
        assertEquals(0, response.getImageAttaches().get(0).getDisplayOrder());
        assertNotNull(response.getImageAttaches().get(0).getCreatedAt());
    }

    @Test
    void toResponse_FileMode_ReturnsImages() {
        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "file");

        Product product = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(new BigDecimal("100.00"))
                .quantity(10)
                .productStatus(ProductStatus.AVAILABLE)
                .productSellType(ProductSellType.DIRECT)
                .productCondition(ProductCondition.NEW)
                .createdAt(LocalDateTime.now())
                .author(User.builder().id(1L).emailAddress("test5@test.com").build())
                .category(Category.builder().id(1L).build())
                .images(Collections.singletonList(
                        ProductImage.builder()
                                .id(1L)
                                .fileName("test.jpg")
                                .fileUrl("http://localhost/images/test.jpg")
                                .contentType("image/jpeg")
                                .fileSize(1000)
                                .fileExtension("jpg")
                                .isPrimary(true)
                                .displayOrder(0)
                                .createdAt(LocalDateTime.now())
                                .build()
                ))
                .imageAttaches(Collections.emptyList())
                .build();

        ProductResponse response = productMapper.toResponse(product);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Test Product", response.getName());
        assertEquals(1, response.getImages().size());
        assertEquals("test.jpg", response.getImages().get(0).getFileName());
        assertEquals("image/jpeg", response.getImages().get(0).getContentType());
        assertEquals(0, response.getImageAttaches().size());
    }

    @Test
    void toEntity_ValidRequest_ReturnsProduct() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(Category.builder().id(1L).build()));
        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "database");

        ProductRequest request = ProductRequest.builder()
                .name("Test Product")
                .price(new BigDecimal("100.00"))
                .quantity(10)
                .categoryId(1L)
                .productSellType(ProductSellType.DIRECT)
                .productCondition(ProductCondition.NEW)
                .variants(Collections.singletonList(
                        ProductVariantRequest.builder()
                                .name("Large")
                                .priceAdjustment(new BigDecimal("10.00"))
                                .quantity(5)
                                .build()
                ))
                .build();

        User author = User.builder().id(1L).emailAddress("test5@test.com").build();
        Product product = productMapper.toEntity(request, author);

        assertNotNull(product);
        assertEquals("Test Product", product.getName());
        assertEquals(1L, product.getCategory().getId());
        assertEquals(1, product.getVariants().size());
        assertEquals("Large", product.getVariants().get(0).getName());
    }
}


/*
class ProductMapperTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private GroupConfig groupConfig;

    @InjectMocks
    private ProductMapper productMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(productMapper, "groupConfig", groupConfig);
        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "database");
    }

    @Test
    void toResponse_DatabaseMode_ReturnsImageAttaches() {
        Product product = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(new BigDecimal("100.00"))
                .quantity(10)
                .productStatus(ProductStatus.AVAILABLE)
                .productSellType(ProductSellType.DIRECT)
                .productCondition(ProductCondition.NEW)
                .createdAt(LocalDateTime.now())
                .author(User.builder().id(1L).emailAddress("test5@test.com").build())
                .category(Category.builder().id(1L).build())
                .imageAttaches(Collections.singletonList(
                        ProductImageAttach.builder()
                                .id(1L)
                                .fileName("test.jpg")
                                .contentType("image/jpeg")
                                .fileSize(1000)
                                .fileExtension("jpg")
                                .fileContent(new byte[]{1, 2, 3})
                                .isPrimary(true)
                                .displayOrder(0)
                                .createdAt(LocalDateTime.now())
                                .build()
                ))
                .images(Collections.emptyList())
                .build();

        ProductResponse response = productMapper.toResponse(product);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Test Product", response.getName());
        assertEquals(0, response.getImages().size());
        assertEquals(1, response.getImageAttaches().size());
        assertEquals("test.jpg", response.getImageAttaches().get(0).getFileName());
        assertEquals("AQID", response.getImageAttaches().get(0).getFileContent()); // Base64 encoded {1, 2, 3}
    }

    @Test
    void toResponse_FileMode_ReturnsImages() {
        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "file");

        Product product = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(new BigDecimal("100.00"))
                .quantity(10)
                .productStatus(ProductStatus.AVAILABLE)
                .productSellType(ProductSellType.DIRECT)
                .productCondition(ProductCondition.NEW)
                .createdAt(LocalDateTime.now())
                .author(User.builder().id(1L).emailAddress("test5@test.com").build())
                .category(Category.builder().id(1L).build())
                .images(Collections.singletonList(
                        ProductImage.builder()
                                .id(1L)
                                .fileName("test.jpg")
                                .fileUrl("http://localhost/images/test.jpg")
                                .contentType("image/jpeg")
                                .fileSize(1000)
                                .fileExtension("jpg")
                                .isPrimary(true)
                                .displayOrder(0)
                                .createdAt(LocalDateTime.now())
                                .build()
                ))
                .imageAttaches(Collections.emptyList())
                .build();

        ProductResponse response = productMapper.toResponse(product);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Test Product", response.getName());
        assertEquals(1, response.getImages().size());
        assertEquals("test.jpg", response.getImages().get(0).getFileName());
        assertEquals(0, response.getImageAttaches().size());
    }

    @Test
    void toEntity_ValidRequest_ReturnsProduct() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(Category.builder().id(1L).build()));
        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "database");

        ProductRequest request = ProductRequest.builder()
                .name("Test Product")
                .price(new BigDecimal("100.00"))
                .quantity(10)
                .categoryId(1L)
                .productSellType(ProductSellType.DIRECT)
                .productCondition(ProductCondition.NEW)
                .variants(Collections.singletonList(
                        ProductVariantResponse.builder()
                                .name("Large")
                                .priceAdjustment(new BigDecimal("10.00"))
                                .quantity(5)
                                .build()
                ))
                .build();

        User author = User.builder().id(1L).emailAddress("test5@test.com").build();
        Product product = productMapper.toEntity(request, author);

        assertNotNull(product);
        assertEquals("Test Product", product.getName());
        assertEquals(1L, product.getCategory().getId());
        assertEquals(1, product.getVariants().size());
        assertEquals("Large", product.getVariants().get(0).getName());
    }
}
*/
