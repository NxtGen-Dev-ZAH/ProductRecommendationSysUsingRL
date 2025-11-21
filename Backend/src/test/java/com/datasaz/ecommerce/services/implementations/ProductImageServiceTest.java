package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.configs.GroupConfig;
import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.models.request.ProductImageRequest;
import com.datasaz.ecommerce.repositories.ProductImageAttachRepository;
import com.datasaz.ecommerce.repositories.ProductImageRepository;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.entities.Product;
import com.datasaz.ecommerce.repositories.entities.ProductImageAttach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductImageServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductImageAttachRepository productImageAttachRepository;

    @Mock
    private GroupConfig groupConfig;

    @InjectMocks
    private ProductImageService productImageService;

    private byte[] testJpeg;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        // Load real JPEG file from src/test/resources
        testJpeg = Files.readAllBytes(Paths.get("src/test/resources/test.jpg"));
        // Use ReflectionTestUtils to set non-final fields
        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "database");
        ReflectionTestUtils.setField(groupConfig, "resizeWidth", 1600);
        ReflectionTestUtils.setField(groupConfig, "resizeHeight", 576);
        ReflectionTestUtils.setField(groupConfig, "thumbnailResizeWidth", 150);
        ReflectionTestUtils.setField(groupConfig, "thumbnailResizeHeight", 150);
        ReflectionTestUtils.setField(groupConfig, "imageQuality", 0.7f);
        ReflectionTestUtils.setField(groupConfig, "maxFileSizeMb", 5);
        ReflectionTestUtils.setField(groupConfig, "MAX_FILE_SIZE", 5_242_880);
        // Mock ALLOWED_IMAGE_TYPES via getter
        when(groupConfig.getALLOWED_IMAGE_TYPES()).thenReturn(Set.of("image/jpeg", "image/png", "image/gif", "image/bmp", "image/tiff", "image/vnd.wap.wbmp", "image/webp"));
    }

    @Test
    void uploadImageAttach_MultipartFile_SavesJpegWithThumbnail() throws Exception {
        // Arrange
        Product product = Product.builder().id(1L).build();
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", testJpeg);
        byte[] mockResizedImage = new byte[]{10, 11, 12};
        byte[] mockThumbnail = new byte[]{13, 14, 15};
        ProductImageAttach imageAttach = ProductImageAttach.builder()
                .id(1L)
                .fileName("test.jpg") // Use a placeholder file name
                .contentType("image/jpeg")
                .fileSize(mockResizedImage.length)
                .fileExtension("jpg")
                .fileContent(mockResizedImage)
                .thumbnailContent(mockThumbnail)
                .isPrimary(true)
                .displayOrder(0)
                .createdAt(LocalDateTime.now())
                .product(product)
                .build();

        // Spy on the service to mock the package-private resizeImage method
        ProductImageService spiedService = spy(productImageService);
        doReturn(mockResizedImage).when(spiedService).resizeImage(eq(testJpeg), eq("jpg"), eq(true), eq(1600), eq(576));
        doReturn(mockThumbnail).when(spiedService).resizeImage(eq(testJpeg), eq("jpg"), eq(true), eq(150), eq(150));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productImageAttachRepository.save(any(ProductImageAttach.class))).thenReturn(imageAttach);

        // Act
        ProductImageAttach result = spiedService.uploadImageAttach(1L, file, true);

        // Assert
        assertNotNull(result);
        assertTrue(result.getFileName().endsWith(".jpg"));
        assertEquals("image/jpeg", result.getContentType());
        assertEquals("jpg", result.getFileExtension());
        assertNotNull(result.getFileContent());
        assertEquals(mockResizedImage.length, result.getFileSize());
        assertArrayEquals(mockResizedImage, result.getFileContent());
        assertNotNull(result.getThumbnailContent());
        assertArrayEquals(mockThumbnail, result.getThumbnailContent());
        assertTrue(result.isPrimary());
        assertEquals(0, result.getDisplayOrder());
        assertNotNull(result.getCreatedAt());
        assertEquals(product, result.getProduct());
        verify(productImageAttachRepository).save(any(ProductImageAttach.class));
    }

    @Test
    void uploadImageAttach_ProductImageRequest_SavesJpegWithThumbnail() throws Exception {
        // Arrange
        Product product = Product.builder().id(1L).build();
        String base64Image = Base64.getEncoder().encodeToString(testJpeg);
        ProductImageRequest request = ProductImageRequest.builder()
                .fileName("test.jpg")
                //.contentType("image/jpeg")
                .fileContent(base64Image)
                .displayOrder(0)
                .build();
        byte[] mockResizedImage = new byte[]{10, 11, 12};
        byte[] mockThumbnail = new byte[]{13, 14, 15};
        ProductImageAttach imageAttach = ProductImageAttach.builder()
                .id(1L)
                .fileName("test.jpg") // Use a placeholder file name
                .contentType("image/jpeg")
                .fileSize(mockResizedImage.length)
                .fileExtension("jpg")
                .fileContent(mockResizedImage)
                .thumbnailContent(mockThumbnail)
                .isPrimary(true)
                .displayOrder(0)
                .createdAt(LocalDateTime.now())
                .product(product)
                .build();

        // Spy on the service to mock the package-private resizeImage method
        ProductImageService spiedService = spy(productImageService);
        doReturn(mockResizedImage).when(spiedService).resizeImage(eq(testJpeg), eq("jpg"), eq(true), eq(1600), eq(576));
        doReturn(mockThumbnail).when(spiedService).resizeImage(eq(testJpeg), eq("jpg"), eq(true), eq(150), eq(150));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productImageAttachRepository.save(any(ProductImageAttach.class))).thenReturn(imageAttach);

        // Act
        ProductImageAttach result = spiedService.uploadImageAttach(1L, request, true);

        // Assert
        assertNotNull(result);
        assertTrue(result.getFileName().endsWith(".jpg"));
        assertEquals("image/jpeg", result.getContentType());
        assertEquals("jpg", result.getFileExtension());
        assertNotNull(result.getFileContent());
        assertEquals(mockResizedImage.length, result.getFileSize());
        assertArrayEquals(mockResizedImage, result.getFileContent());
        assertNotNull(result.getThumbnailContent());
        assertArrayEquals(mockThumbnail, result.getThumbnailContent());
        assertTrue(result.isPrimary());
        assertEquals(0, result.getDisplayOrder());
        assertNotNull(result.getCreatedAt());
        assertEquals(product, result.getProduct());
        verify(productImageAttachRepository).save(any(ProductImageAttach.class));
    }

    @Test
    void uploadImageAttach_TooLargeImage_ThrowsBadRequestException() {
        // Arrange
        Product product = Product.builder().id(1L).build();
        byte[] largeImage = new byte[6_000_000]; // 6 MB, exceeds 5 MB limit
        ProductImageRequest request = ProductImageRequest.builder()
                .fileName("large.jpg")
                .contentType("image/jpeg")
                .fileContent(Base64.getEncoder().encodeToString(largeImage))
                .displayOrder(0)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> productImageService.uploadImageAttach(1L, request, true));
    }

    @Test
    void uploadImageAttach_InvalidMimeType_ThrowsBadRequestException() {
        // Arrange
        Product product = Product.builder().id(1L).build();
        MockMultipartFile file = new MockMultipartFile("image", "test.txt", "text/plain", new byte[]{1, 2, 3});

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> productImageService.uploadImageAttach(1L, file, true));
    }

    @Test
    void getImageAttachesByProductId_ReturnsImages() {
        // Arrange
        Product product = Product.builder().id(1L).build();
        ProductImageAttach imageAttach = ProductImageAttach.builder()
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
                .product(product)
                .build();

        when(productImageAttachRepository.findByProductId(1L)).thenReturn(List.of(imageAttach));

        // Act
        List<ProductImageAttach> result = productImageService.getImageAttachesByProductId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test.jpg", result.get(0).getFileName());
        assertArrayEquals(new byte[]{1, 2, 3}, result.get(0).getFileContent());
        assertArrayEquals(new byte[]{4, 5, 6}, result.get(0).getThumbnailContent());
        verify(productImageAttachRepository).findByProductId(1L);
    }

    @Test
    void getImageAttachesByProductId_NoImages_EmptyList() {
        // Arrange
        when(productImageAttachRepository.findByProductId(1L)).thenReturn(List.of());

        // Act
        List<ProductImageAttach> result = productImageService.getImageAttachesByProductId(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(productImageAttachRepository).findByProductId(1L);
    }
}




/*
import com.datasaz.ecommerce.configs.GroupConfig;
import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.models.request.ProductImageRequest;
import com.datasaz.ecommerce.repositories.ProductImageAttachRepository;
import com.datasaz.ecommerce.repositories.ProductImageRepository;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.entities.Product;
import com.datasaz.ecommerce.repositories.entities.ProductImageAttach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductImageServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductImageAttachRepository productImageAttachRepository;

    @Mock
    private GroupConfig groupConfig;

    @InjectMocks
    private ProductImageService productImageService;

    private byte[] testJpeg;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        // Load real JPEG file from src/test/resources
        testJpeg = Files.readAllBytes(Paths.get("src/test/resources/test.jpg"));
        // Use ReflectionTestUtils to set non-final fields
        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "database");
        ReflectionTestUtils.setField(groupConfig, "resizeWidth", 1600);
        ReflectionTestUtils.setField(groupConfig, "resizeHeight", 576);
        ReflectionTestUtils.setField(groupConfig, "thumbnailResizeWidth", 150);
        ReflectionTestUtils.setField(groupConfig, "thumbnailResizeHeight", 150);
        ReflectionTestUtils.setField(groupConfig, "imageQuality", 0.7f);
        ReflectionTestUtils.setField(groupConfig, "maxFileSizeMb", 5);
        ReflectionTestUtils.setField(groupConfig, "MAX_FILE_SIZE", 5_242_880);
        // Mock ALLOWED_IMAGE_TYPES via getter
        when(groupConfig.getALLOWED_IMAGE_TYPES()).thenReturn(Set.of("image/jpeg", "image/png", "image/gif", "image/bmp", "image/tiff", "image/vnd.wap.wbmp", "image/webp"));
    }

    @Test
    void uploadImageAttach_MultipartFile_SavesJpegWithThumbnail() throws Exception {
        // Arrange
        Product product = Product.builder().id(1L).build();
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", testJpeg);
        byte[] mockResizedImage = new byte[]{10, 11, 12};
        byte[] mockThumbnail = new byte[]{13, 14, 15};
        ProductImageAttach imageAttach = ProductImageAttach.builder()
                .id(1L)
                .fileName("test.jpg") // Use a placeholder file name
                .contentType("image/jpeg")
                .fileSize(mockResizedImage.length)
                .fileExtension("jpg")
                .fileContent(mockResizedImage)
                .thumbnailContent(mockThumbnail)
                .isPrimary(true)
                .displayOrder(0)
                .createdAt(LocalDateTime.now())
                .product(product)
                .build();

        // Spy on the service to mock the package-private resizeImage method
        ProductImageService spiedService = spy(productImageService);
        doReturn(mockResizedImage).when(spiedService).resizeImage(eq(testJpeg), eq("jpg"), eq(true), eq(1600), eq(576));
        doReturn(mockThumbnail).when(spiedService).resizeImage(eq(testJpeg), eq("jpg"), eq(true), eq(150), eq(150));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productImageAttachRepository.save(any(ProductImageAttach.class))).thenReturn(imageAttach);

        // Act
        ProductImageAttach result = spiedService.uploadImageAttach(1L, file, true);

        // Assert
        assertNotNull(result);
        assertTrue(result.getFileName().endsWith(".jpg"));
        assertEquals("image/jpeg", result.getContentType());
        assertEquals("jpg", result.getFileExtension());
        assertNotNull(result.getFileContent());
        assertEquals(mockResizedImage.length, result.getFileSize());
        assertArrayEquals(mockResizedImage, result.getFileContent());
        assertNotNull(result.getThumbnailContent());
        assertArrayEquals(mockThumbnail, result.getThumbnailContent());
        assertTrue(result.isPrimary());
        assertEquals(0, result.getDisplayOrder());
        assertNotNull(result.getCreatedAt());
        assertEquals(product, result.getProduct());
        verify(productImageAttachRepository).save(any(ProductImageAttach.class));
    }

    @Test
    void uploadImageAttach_ProductImageRequest_SavesJpegWithThumbnail() throws Exception {
        // Arrange
        Product product = Product.builder().id(1L).build();
        String base64Image = Base64.getEncoder().encodeToString(testJpeg);
        ProductImageRequest request = ProductImageRequest.builder()
                .fileName("test.jpg")
                //.contentType("image/jpeg")
                .fileContent(base64Image)
                .displayOrder(0)
                .build();
        byte[] mockResizedImage = new byte[]{10, 11, 12};
        byte[] mockThumbnail = new byte[]{13, 14, 15};
        ProductImageAttach imageAttach = ProductImageAttach.builder()
                .id(1L)
                .fileName("test.jpg") // Use a placeholder file name
                .contentType("image/jpeg")
                .fileSize(mockResizedImage.length)
                .fileExtension("jpg")
                .fileContent(mockResizedImage)
                .thumbnailContent(mockThumbnail)
                .isPrimary(true)
                .displayOrder(0)
                .createdAt(LocalDateTime.now())
                .product(product)
                .build();

        // Spy on the service to mock the package-private resizeImage method
        ProductImageService spiedService = spy(productImageService);
        doReturn(mockResizedImage).when(spiedService).resizeImage(eq(testJpeg), eq("jpg"), eq(true), eq(1600), eq(576));
        doReturn(mockThumbnail).when(spiedService).resizeImage(eq(testJpeg), eq("jpg"), eq(true), eq(150), eq(150));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productImageAttachRepository.save(any(ProductImageAttach.class))).thenReturn(imageAttach);

        // Act
        ProductImageAttach result = spiedService.uploadImageAttach(1L, request, true);

        // Assert
        assertNotNull(result);
        assertTrue(result.getFileName().endsWith(".jpg"));
        assertEquals("image/jpeg", result.getContentType());
        assertEquals("jpg", result.getFileExtension());
        assertNotNull(result.getFileContent());
        assertEquals(mockResizedImage.length, result.getFileSize());
        assertArrayEquals(mockResizedImage, result.getFileContent());
        assertNotNull(result.getThumbnailContent());
        assertArrayEquals(mockThumbnail, result.getThumbnailContent());
        assertTrue(result.isPrimary());
        assertEquals(0, result.getDisplayOrder());
        assertNotNull(result.getCreatedAt());
        assertEquals(product, result.getProduct());
        verify(productImageAttachRepository).save(any(ProductImageAttach.class));
    }

    @Test
    void uploadImageAttach_TooLargeImage_ThrowsBadRequestException() {
        // Arrange
        Product product = Product.builder().id(1L).build();
        byte[] largeImage = new byte[6_000_000]; // 6 MB, exceeds 5 MB limit
        ProductImageRequest request = ProductImageRequest.builder()
                .fileName("large.jpg")
                .contentType("image/jpeg")
                .fileContent(Base64.getEncoder().encodeToString(largeImage))
                .displayOrder(0)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> productImageService.uploadImageAttach(1L, request, true));
    }

    @Test
    void uploadImageAttach_InvalidMimeType_ThrowsBadRequestException() {
        // Arrange
        Product product = Product.builder().id(1L).build();
        MockMultipartFile file = new MockMultipartFile("image", "test.txt", "text/plain", new byte[]{1, 2, 3});

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> productImageService.uploadImageAttach(1L, file, true));
    }

    @Test
    void getImageAttachesByProductId_ReturnsImages() {
        // Arrange
        Product product = Product.builder().id(1L).build();
        ProductImageAttach imageAttach = ProductImageAttach.builder()
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
                .product(product)
                .build();

        when(productImageAttachRepository.findByProductId(1L)).thenReturn(List.of(imageAttach));

        // Act
        List<ProductImageAttach> result = productImageService.getImageAttachesByProductId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test.jpg", result.get(0).getFileName());
        assertArrayEquals(new byte[]{1, 2, 3}, result.get(0).getFileContent());
        assertArrayEquals(new byte[]{4, 5, 6}, result.get(0).getThumbnailContent());
        verify(productImageAttachRepository).findByProductId(1L);
    }

    @Test
    void getImageAttachesByProductId_NoImages_EmptyList() {
        // Arrange
        when(productImageAttachRepository.findByProductId(1L)).thenReturn(List.of());

        // Act
        List<ProductImageAttach> result = productImageService.getImageAttachesByProductId(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(productImageAttachRepository).findByProductId(1L);
    }
}
*/

//
//import com.datasaz.ecommerce.configs.GroupConfig;
//import com.datasaz.ecommerce.exceptions.BadRequestException;
//import com.datasaz.ecommerce.exceptions.ResourceNotFoundException;
//import com.datasaz.ecommerce.models.request.ProductImageRequest;
//import com.datasaz.ecommerce.repositories.ProductImageAttachRepository;
//import com.datasaz.ecommerce.repositories.ProductImageRepository;
//import com.datasaz.ecommerce.repositories.ProductRepository;
//import com.datasaz.ecommerce.repositories.entities.Product;
//import com.datasaz.ecommerce.repositories.entities.ProductImageAttach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import java.time.LocalDateTime;
//import java.util.Base64;
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//class ProductImageServiceTest {
//
//    @Mock
//    private ProductRepository productRepository;
//
//    @Mock
//    private ProductImageRepository productImageRepository;
//
//    @Mock
//    private ProductImageAttachRepository productImageAttachRepository;
//
//    @Mock
//    private GroupConfig groupConfig;
//
//    @InjectMocks
//    private ProductImageService productImageService;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "database");
//        ReflectionTestUtils.setField(groupConfig, "resizeWidth", 1600);
//        ReflectionTestUtils.setField(groupConfig, "resizeHeight", 576);
//        ReflectionTestUtils.setField(groupConfig, "thumbnailResizeWidth", 150);
//        ReflectionTestUtils.setField(groupConfig, "thumbnailResizeHeight", 150);
//        ReflectionTestUtils.setField(groupConfig, "imageQuality", 0.7f);
//        ReflectionTestUtils.setField(groupConfig, "maxFileSizeMb", 5);
//        ReflectionTestUtils.setField(groupConfig, "MAX_FILE_SIZE", 5_242_880);
//    }
//
//    @Test
//    void uploadImageAttach_MultipartFile_SavesJpegWithThumbnail() {
//        Product product = Product.builder().id(1L).build();
//        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[]{1, 2, 3, 4, 5});
//        ProductImageAttach imageAttach = ProductImageAttach.builder()
//                .id(1L)
//                .fileName("test.jpg")
//                .contentType("image/jpeg")
//                .fileSize(1000)
//                .fileExtension("jpg")
//                .fileContent(new byte[]{1, 2, 3})
//                .thumbnailContent(new byte[]{4, 5, 6})
//                .isPrimary(true)
//                .displayOrder(0)
//                .createdAt(LocalDateTime.now())
//                .product(product)
//                .build();
//
//        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//        when(productImageAttachRepository.save(any(ProductImageAttach.class))).thenReturn(imageAttach);
//
//        ProductImageAttach result = productImageService.uploadImageAttach(1L, file, true);
//
//        assertNotNull(result);
//        assertEquals("test.jpg", result.getFileName());
//        assertEquals("image/jpeg", result.getContentType());
//        assertEquals("jpg", result.getFileExtension());
//        assertNotNull(result.getThumbnailContent());
//        assertArrayEquals(new byte[]{4, 5, 6}, result.getThumbnailContent());
//        verify(productImageAttachRepository).save(any(ProductImageAttach.class));
//    }
//
//    @Test
//    void uploadImageAttach_ProductImageRequest_SavesJpegWithThumbnail() {
//        Product product = Product.builder().id(1L).build();
//        ProductImageRequest request = ProductImageRequest.builder()
//                .fileName("test.jpg")
//                .contentType("image/jpeg")
//                .fileContent(Base64.getEncoder().encodeToString(new byte[]{1, 2, 3, 4, 5}))
//                .displayOrder(0)
//                .build();
//        ProductImageAttach imageAttach = ProductImageAttach.builder()
//                .id(1L)
//                .fileName("test.jpg")
//                .contentType("image/jpeg")
//                .fileSize(1000)
//                .fileExtension("jpg")
//                .fileContent(new byte[]{1, 2, 3})
//                .thumbnailContent(new byte[]{4, 5, 6})
//                .isPrimary(true)
//                .displayOrder(0)
//                .createdAt(LocalDateTime.now())
//                .product(product)
//                .build();
//
//        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//        when(productImageAttachRepository.save(any(ProductImageAttach.class))).thenReturn(imageAttach);
//
//        ProductImageAttach result = productImageService.uploadImageAttach(1L, request, true);
//
//        assertNotNull(result);
//        assertEquals("test.jpg", result.getFileName());
//        assertEquals("image/jpeg", result.getContentType());
//        assertEquals("jpg", result.getFileExtension());
//        assertNotNull(result.getThumbnailContent());
//        assertArrayEquals(new byte[]{4, 5, 6}, result.getThumbnailContent());
//        verify(productImageAttachRepository).save(any(ProductImageAttach.class));
//    }
//
//    @Test
//    void uploadImageAttach_TooLargeImage_ThrowsBadRequestException() {
//        Product product = Product.builder().id(1L).build();
//        byte[] largeImage = new byte[6_000_000]; // 6 MB, exceeds 5 MB limit
//        ProductImageRequest request = ProductImageRequest.builder()
//                .fileName("large.jpg")
//                .contentType("image/jpeg")
//                .fileContent(Base64.getEncoder().encodeToString(largeImage))
//                .displayOrder(0)
//                .build();
//
//        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//
//        assertThrows(BadRequestException.class, () -> productImageService.uploadImageAttach(1L, request, true));
//    }
//
//    @Test
//    void uploadImageAttach_InvalidMimeType_ThrowsBadRequestException() {
//        Product product = Product.builder().id(1L).build();
//        MockMultipartFile file = new MockMultipartFile("image", "test.txt", "text/plain", new byte[]{1, 2, 3});
//
//        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//
//        assertThrows(BadRequestException.class, () -> productImageService.uploadImageAttach(1L, file, true));
//    }
//
//    @Test
//    void getImageAttachesByProductId_ReturnsImages() {
//        Product product = Product.builder().id(1L).build();
//        ProductImageAttach imageAttach = ProductImageAttach.builder()
//                .id(1L)
//                .fileName("test.jpg")
//                .contentType("image/jpeg")
//                .fileSize(1000)
//                .fileExtension("jpg")
//                .fileContent(new byte[]{1, 2, 3})
//                .thumbnailContent(new byte[]{4, 5, 6})
//                .isPrimary(true)
//                .displayOrder(0)
//                .createdAt(LocalDateTime.now())
//                .product(product)
//                .build();
//
//        when(productImageAttachRepository.findByProductId(1L)).thenReturn(List.of(imageAttach));
//
//        List<ProductImageAttach> result = productImageService.getImageAttachesByProductId(1L);
//
//        assertNotNull(result);
//        assertEquals(1, result.size());
//        assertEquals("test.jpg", result.get(0).getFileName());
//        assertArrayEquals(new byte[]{1, 2, 3}, result.get(0).getFileContent());
//        assertArrayEquals(new byte[]{4, 5, 6}, result.get(0).getThumbnailContent());
//        verify(productImageAttachRepository).findByProductId(1L);
//    }
//
//    @Test
//    void getImageAttachesByProductId_NoImages_EmptyList() {
//        when(productImageAttachRepository.findByProductId(1L)).thenReturn(List.of());
//
//        List<ProductImageAttach> result = productImageService.getImageAttachesByProductId(1L);
//
//        assertNotNull(result);
//        assertTrue(result.isEmpty());
//        verify(productImageAttachRepository).findByProductId(1L);
//    }
//}