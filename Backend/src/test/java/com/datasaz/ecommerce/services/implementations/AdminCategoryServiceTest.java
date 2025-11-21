package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.CategoryNotFoundException;
import com.datasaz.ecommerce.exceptions.response.ExceptionMessages;
import com.datasaz.ecommerce.mappers.CategoryMapper;
import com.datasaz.ecommerce.models.request.CategoryRequest;
import com.datasaz.ecommerce.models.response.CategoryResponse;
import com.datasaz.ecommerce.repositories.CategoryRepository;
import com.datasaz.ecommerce.repositories.entities.Category;
import com.datasaz.ecommerce.utilities.FileStorageService;
import org.apache.tika.Tika;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminCategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private Tika tika;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AdminCategoryService adminCategoryService;

    private CategoryRequest categoryRequest;
    private Category category;
    private Category parentCategory;
    private CategoryResponse categoryResponse;
    private MockMultipartFile image;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);

        // Mock FileStorageService
        when(fileStorageService.exists(any(Path.class))).thenReturn(false);
        when(fileStorageService.createDirectories(any(Path.class))).thenReturn(mock(Path.class));
        when(fileStorageService.copy(any(InputStream.class), any(Path.class), eq(StandardCopyOption.REPLACE_EXISTING))).thenReturn(1024L);
        when(fileStorageService.deleteIfExists(any(Path.class))).thenReturn(true);

        // Mock AuditLogService
        doNothing().when(auditLogService).logAction(anyString(), anyString(), anyString());

        categoryRequest = CategoryRequest.builder()
                .name("Electronics")
                .description("Electronic devices")
                .parentId(null)
                .build();

        category = new Category();
        category.setId(1L);
        category.setName("Electronics");
        category.setDescription("Electronic devices");
        category.setImageUrl("/category_images/electronics.jpg");
        category.setCreatedAt(LocalDateTime.of(2025, 7, 22, 8, 0));
        category.setParent(null);
        category.setSubcategories(null);

        parentCategory = new Category();
        parentCategory.setId(2L);
        parentCategory.setName("Parent Category");
        parentCategory.setDescription("Parent category");
        parentCategory.setCreatedAt(LocalDateTime.of(2025, 7, 22, 7, 0));

        categoryResponse = CategoryResponse.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic devices")
                .imageUrl("/category_images/electronics.jpg")
                .createdDate(LocalDateTime.of(2025, 7, 22, 8, 0))
                .parentId(null)
                .subcategories(null)
                .build();

        // Use a realistic JPEG header
        byte[] imageContent = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46};
        image = new MockMultipartFile("image", "test.jpg", "image/jpeg", imageContent);

        // Mock CategoryMapper behavior
        when(categoryMapper.toEntity(eq(categoryRequest), any())).thenReturn(category);
        when(categoryMapper.toResponse(eq(category))).thenReturn(categoryResponse);

        // Mock Tika behavior for default image
        when(tika.detect(any(InputStream.class))).thenReturn("image/jpeg");
    }

    @Test
    void testSaveCategory_Success() {
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryResponse result = adminCategoryService.saveCategory(categoryRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Electronics", result.getName());
        verify(categoryMapper).toEntity(eq(categoryRequest), eq(null));
        verify(categoryRepository).save(category);
        verify(categoryMapper).toResponse(category);
    }

    @Test
    void testSaveCategory_WithParent_Success() {
        categoryRequest.setParentId(2L);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(parentCategory));
        when(categoryMapper.toEntity(eq(categoryRequest), eq(parentCategory))).thenReturn(category);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryResponse result = adminCategoryService.saveCategory(categoryRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Electronics", result.getName());
        verify(categoryRepository).findById(2L);
        verify(categoryMapper).toEntity(eq(categoryRequest), eq(parentCategory));
        verify(categoryRepository).save(category);
        verify(categoryMapper).toResponse(category);
    }

    /*@Test
    void testSaveCategory_NullRequest() {
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            adminCategoryService.saveCategory(null);
        });

        assertEquals(ExceptionMessages.BAD_REQUEST + "Category request cannot be null", exception.getMessage());
        verifyNoInteractions(categoryRepository, categoryMapper);
    }*/

    @Test
    void testSaveCategory_ParentNotFound() {
        categoryRequest.setParentId(999L);
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        CategoryNotFoundException exception = assertThrows(CategoryNotFoundException.class, () -> {
            adminCategoryService.saveCategory(categoryRequest);
        });

        assertEquals(ExceptionMessages.CATEGORY_NOT_FOUND + "Parent category not found", exception.getMessage());
        verify(categoryRepository).findById(999L);
        verifyNoInteractions(categoryMapper);
    }

    @Test
    void testUpdateCategory_Success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryResponse result = adminCategoryService.updateCategory(1L, categoryRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Electronics", result.getName());
        verify(categoryRepository).findById(1L);
        verify(categoryRepository).save(category);
        verify(categoryMapper).toResponse(category);
    }

    @Test
    void testUpdateCategory_WithParent_Success() {
        categoryRequest.setParentId(2L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(parentCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryResponse result = adminCategoryService.updateCategory(1L, categoryRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Electronics", result.getName());
        verify(categoryRepository).findById(1L);
        verify(categoryRepository).findById(2L);
        verify(categoryRepository).save(category);
        verify(categoryMapper).toResponse(category);
    }

    @Test
    void testUpdateCategory_NullId() {
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            adminCategoryService.updateCategory(null, categoryRequest);
        });

        assertEquals(ExceptionMessages.BAD_REQUEST + "Category ID and request cannot be null", exception.getMessage());
        verifyNoInteractions(categoryRepository, categoryMapper);
    }

    @Test
    void testUpdateCategory_NullRequest() {
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            adminCategoryService.updateCategory(1L, null);
        });

        assertEquals(ExceptionMessages.BAD_REQUEST + "Category ID and request cannot be null", exception.getMessage());
        verifyNoInteractions(categoryRepository, categoryMapper);
    }

    @Test
    void testUpdateCategory_CategoryNotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        CategoryNotFoundException exception = assertThrows(CategoryNotFoundException.class, () -> {
            adminCategoryService.updateCategory(999L, categoryRequest);
        });

        assertEquals(ExceptionMessages.CATEGORY_NOT_FOUND + "Category not found.", exception.getMessage());
        verify(categoryRepository).findById(999L);
        verifyNoMoreInteractions(categoryRepository);
        verifyNoInteractions(categoryMapper);
    }

    @Test
    void testUpdateCategory_ParentNotFound() {
        categoryRequest.setParentId(999L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        CategoryNotFoundException exception = assertThrows(CategoryNotFoundException.class, () -> {
            adminCategoryService.updateCategory(1L, categoryRequest);
        });

        assertEquals(ExceptionMessages.CATEGORY_NOT_FOUND + "Parent category not found", exception.getMessage());
        verify(categoryRepository).findById(1L);
        verify(categoryRepository).findById(999L);
        verifyNoMoreInteractions(categoryRepository);
        verifyNoInteractions(categoryMapper);
    }

    @Test
    void testDeleteCategory_Success() {
        when(categoryRepository.existsById(1L)).thenReturn(true);
        doNothing().when(categoryRepository).deleteById(1L);

        adminCategoryService.deleteCategory(1L);

        verify(categoryRepository).existsById(1L);
        verify(categoryRepository).deleteById(1L);
    }

    @Test
    void testDeleteCategory_CategoryNotFound() {
        when(categoryRepository.existsById(999L)).thenReturn(false);

        CategoryNotFoundException exception = assertThrows(CategoryNotFoundException.class, () -> {
            adminCategoryService.deleteCategory(999L);
        });

        assertEquals(ExceptionMessages.CATEGORY_NOT_FOUND + "Category not found.", exception.getMessage());
        verify(categoryRepository).existsById(999L);
        verifyNoMoreInteractions(categoryRepository);
    }

//    @Test
//    void testUploadCategoryImage_Success() throws IOException {
//        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
//        when(categoryRepository.save(any(Category.class))).thenReturn(category);
//        when(tika.detect(any(InputStream.class))).thenReturn("image/jpeg");
//
//        String result = adminCategoryService.uploadCategoryImage(image, 1L);
//
//        assertNotNull(result);
//        assertTrue(result.contains("category_images"));
//        assertTrue(result.endsWith(".jpg"));
//        verify(categoryRepository).findById(1L);
//        verify(tika).detect(any(InputStream.class));
//        verify(fileStorageService).createDirectories(any(Path.class));
//        verify(fileStorageService).copy(any(InputStream.class), any(Path.class), eq(StandardCopyOption.REPLACE_EXISTING));
//        verify(categoryRepository).save(category);
//        verify(auditLogService).logAction(eq("SYSTEM"), eq("UPLOAD_CATEGORY_IMAGE"), anyString());
//    }
//
//    @Test
//    void testUploadCategoryImage_ReplaceExistingImage() throws IOException {
//        category.setImageUrl("/category_images/old.jpg");
//        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
//        when(categoryRepository.save(any(Category.class))).thenReturn(category);
//        when(tika.detect(any(InputStream.class))).thenReturn("image/jpeg");
//
//        String result = adminCategoryService.uploadCategoryImage(image, 1L);
//
//        assertNotNull(result);
//        assertTrue(result.contains("category_images"));
//        assertTrue(result.endsWith(".jpg"));
//        verify(categoryRepository).findById(1L);
//        verify(tika).detect(any(InputStream.class));
//        verify(fileStorageService).deleteIfExists(any(Path.class));
//        verify(fileStorageService).createDirectories(any(Path.class));
//        verify(fileStorageService).copy(any(InputStream.class), any(Path.class), eq(StandardCopyOption.REPLACE_EXISTING));
//        verify(categoryRepository).save(category);
//        verify(auditLogService).logAction(eq("SYSTEM"), eq("UPLOAD_CATEGORY_IMAGE"), anyString());
//    }

    @Test
    void testUploadCategoryImage_CategoryNotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        CategoryNotFoundException exception = assertThrows(CategoryNotFoundException.class, () -> {
            adminCategoryService.uploadCategoryImage(image, 999L);
        });

        assertEquals(ExceptionMessages.CATEGORY_NOT_FOUND + "Category not found.", exception.getMessage());
        verify(categoryRepository).findById(999L);
        verifyNoInteractions(tika, categoryMapper, fileStorageService, auditLogService);
    }

    /*@Test
    void testUploadCategoryImage_OversizedImage() {
        MockMultipartFile largeImage = new MockMultipartFile("image", "large.jpg", "image/jpeg", new byte[1024 * 1024 + 1]);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            adminCategoryService.uploadCategoryImage(largeImage, 1L);
        });

        assertEquals("Image size exceeds limit", exception.getMessage());
        verify(categoryRepository).findById(1L);
        verifyNoInteractions(tika, categoryMapper, fileStorageService, auditLogService);
    }*/

//    @Test
//    void testUploadCategoryImage_UnsupportedImageType() throws IOException {
//        byte[] bmpContent = new byte[]{
//                (byte) 0x42, (byte) 0x4D, // BM header
//                (byte) 0x1E, (byte) 0x00, (byte) 0x00, (byte) 0x00, // File size
//                (byte) 0x00, (byte) 0x00, // Reserved
//                (byte) 0x00, (byte) 0x00, // Reserved
//                (byte) 0x1A, (byte) 0x00, (byte) 0x00, (byte) 0x00  // Offset to pixel data
//        };
//        MockMultipartFile bmpImage = new MockMultipartFile("image", "test.bmp", "image/bmp", bmpContent);
//        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
//        when(tika.detect(any(InputStream.class))).thenReturn("image/bmp");
//
//        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
//            adminCategoryService.uploadCategoryImage(bmpImage, 1L);
//        });
//
//        assertEquals("Unsupported image type: image/bmp", exception.getMessage());
//        verify(categoryRepository).findById(1L);
//        verify(tika).detect(any(InputStream.class));
//        verifyNoInteractions(categoryMapper, fileStorageService, auditLogService);
//    }

   /* @Test
    void testUploadCategoryImage_IOExceptionOnDetect() throws IOException {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(tika.detect(any(InputStream.class))).thenThrow(new IOException("Detection error"));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            adminCategoryService.uploadCategoryImage(image, 1L);
        });

        assertEquals("Error detecting image type: Detection error", exception.getMessage());
        verify(categoryRepository).findById(1L);
        verify(tika).detect(any(InputStream.class));
        verifyNoInteractions(categoryMapper, fileStorageService, auditLogService);
    }*/

//    @Test
//    void testUploadCategoryImage_IOExceptionOnCopy() throws IOException {
//        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
//        when(tika.detect(any(InputStream.class))).thenReturn("image/jpeg");
//        when(fileStorageService.copy(any(InputStream.class), any(Path.class), eq(StandardCopyOption.REPLACE_EXISTING)))
//                .thenThrow(new IOException("Copy error"));
//
//        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
//            adminCategoryService.uploadCategoryImage(image, 1L);
//        });
//
//        assertEquals("Failed to upload image: Copy error", exception.getMessage());
//        verify(categoryRepository).findById(1L);
//        verify(tika).detect(any(InputStream.class));
//        verify(fileStorageService).createDirectories(any(Path.class));
//        verify(fileStorageService).copy(any(InputStream.class), any(Path.class), eq(StandardCopyOption.REPLACE_EXISTING));
//        verifyNoInteractions(categoryMapper, auditLogService);
//        verify(categoryRepository, never()).save(any());
//    }

   /* @Test
    void testUploadCategoryImage_NullImage() {
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            adminCategoryService.uploadCategoryImage(null, 1L);
        });
        assertEquals("Image file is required", exception.getMessage());
        verifyNoInteractions(categoryRepository, tika, categoryMapper, fileStorageService, auditLogService);
    }*/
}