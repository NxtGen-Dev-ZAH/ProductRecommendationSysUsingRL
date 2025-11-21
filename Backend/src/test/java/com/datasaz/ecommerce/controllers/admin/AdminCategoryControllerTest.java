package com.datasaz.ecommerce.controllers.admin;

import com.datasaz.ecommerce.exceptions.CategoryNotFoundException;
import com.datasaz.ecommerce.exceptions.response.ExceptionMessages;
import com.datasaz.ecommerce.models.request.CategoryRequest;
import com.datasaz.ecommerce.models.response.CategoryResponse;
import com.datasaz.ecommerce.services.interfaces.IAdminCategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AdminCategoryControllerTest {

    @Mock
    private IAdminCategoryService adminCategoryService;

    @InjectMocks
    private AdminCategoryController adminCategoryController;

    private CategoryRequest categoryRequest;
    private CategoryResponse categoryResponse;
    private byte[] testImage;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock image data
        testImage = new byte[]{1, 2, 3}; // Replace with actual image data in real tests
        String base64Image = Base64.getEncoder().encodeToString(testImage);

        categoryRequest = CategoryRequest.builder()
                .name("Electronics")
                .description("Electronic devices")
                .parentId(null)
                .imageContent(base64Image)
                .imageContentType("image/jpeg")
                .build();

        categoryResponse = CategoryResponse.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic devices")
                .imageUrl("/category_images/electronics.jpg")
                .imageContent(base64Image)
                .imageContentType("image/jpeg")
                .createdDate(LocalDateTime.of(2025, 7, 22, 8, 0))
                .parentId(null)
                .subcategories(null)
                .build();
    }

    @Test
    void testAddCategory_Success() {
        when(adminCategoryService.saveCategory(any(CategoryRequest.class))).thenReturn(categoryResponse);

        ResponseEntity<CategoryResponse> response = adminCategoryController.addCategory(categoryRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(categoryResponse, response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals("Electronics", response.getBody().getName());
        verify(adminCategoryService).saveCategory(categoryRequest);
    }

    @Test
    void testAddCategory_InvalidRequest() {
        CategoryRequest invalidRequest = CategoryRequest.builder().name(null).build();
        when(adminCategoryService.saveCategory(invalidRequest)).thenThrow(new jakarta.validation.ConstraintViolationException("Invalid request", null));

        assertThrows(jakarta.validation.ConstraintViolationException.class, () -> {
            adminCategoryController.addCategory(invalidRequest);
        });
        verify(adminCategoryService).saveCategory(invalidRequest);
    }

    @Test
    void testUploadCategoryImage_MultipartFile_Success() {
        MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", testImage);
        String imageUrl = "/category_images/test.jpg";
        when(adminCategoryService.uploadCategoryImage(eq(image), eq(1L))).thenReturn(imageUrl);

        ResponseEntity<String> response = adminCategoryController.uploadCategoryImage(1L, image, null);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(imageUrl, response.getBody());
        verify(adminCategoryService).uploadCategoryImage(image, 1L);
        verify(adminCategoryService, never()).updateCategory(anyLong(), any());
    }

    @Test
    void testUploadCategoryImage_CategoryRequest_FileSystemMode_Success() {
        String imageUrl = "/category_images/electronics.jpg";
        CategoryResponse updatedResponse = CategoryResponse.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic devices")
                .imageUrl(imageUrl)
                .imageContent(null)
                .imageContentType(null)
                .createdDate(LocalDateTime.of(2025, 7, 22, 8, 0))
                .parentId(null)
                .subcategories(null)
                .build();
        when(adminCategoryService.updateCategory(eq(1L), eq(categoryRequest))).thenReturn(updatedResponse);

        ResponseEntity<String> response = adminCategoryController.uploadCategoryImage(1L, null, categoryRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(imageUrl, response.getBody());
        verify(adminCategoryService).updateCategory(1L, categoryRequest);
        verify(adminCategoryService, never()).uploadCategoryImage(any(MultipartFile.class), anyLong());
    }

    @Test
    void testUploadCategoryImage_CategoryRequest_DatabaseMode_Success() {
        String base64Image = Base64.getEncoder().encodeToString(testImage);
        CategoryResponse updatedResponse = CategoryResponse.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic devices")
                .imageUrl(null)
                .imageContent(base64Image)
                .imageContentType("image/jpeg")
                .createdDate(LocalDateTime.of(2025, 7, 22, 8, 0))
                .parentId(null)
                .subcategories(null)
                .build();
        when(adminCategoryService.updateCategory(eq(1L), eq(categoryRequest))).thenReturn(updatedResponse);

        ResponseEntity<String> response = adminCategoryController.uploadCategoryImage(1L, null, categoryRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("", response.getBody());
        verify(adminCategoryService).updateCategory(1L, categoryRequest);
        verify(adminCategoryService, never()).uploadCategoryImage(any(MultipartFile.class), anyLong());
    }

    @Test
    void testUploadCategoryImage_NullImageAndRequest_ThrowsBadRequest() {
        ResponseEntity<String> response = adminCategoryController.uploadCategoryImage(1L, null, null);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Image file or image content is required", response.getBody());
        verifyNoInteractions(adminCategoryService);
    }

    @Test
    void testUploadCategoryImage_EmptyImage_ThrowsBadRequest() {
        MockMultipartFile emptyImage = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[0]);

        ResponseEntity<String> response = adminCategoryController.uploadCategoryImage(1L, emptyImage, null);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Image file or image content is required", response.getBody());
        verifyNoInteractions(adminCategoryService);
    }

    @Test
    void testUploadCategoryImage_EmptyCategoryRequestImageContent_ThrowsBadRequest() {
        CategoryRequest emptyRequest = CategoryRequest.builder()
                .name("Electronics")
                .description("Electronic devices")
                .imageContent("")
                .imageContentType("image/jpeg")
                .build();

        ResponseEntity<String> response = adminCategoryController.uploadCategoryImage(1L, null, emptyRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Image file or image content is required", response.getBody());
        verifyNoInteractions(adminCategoryService);
    }

    @Test
    void testUploadCategoryImage_CategoryNotFound_ThrowsCategoryNotFoundException() {
        MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", testImage);
        when(adminCategoryService.uploadCategoryImage(eq(image), eq(999L)))
                .thenThrow(CategoryNotFoundException.builder().message(ExceptionMessages.CATEGORY_NOT_FOUND + "Category not found.").build());

        assertThrows(CategoryNotFoundException.class, () -> {
            adminCategoryController.uploadCategoryImage(999L, image, null);
        });
        verify(adminCategoryService).uploadCategoryImage(image, 999L);
        verify(adminCategoryService, never()).updateCategory(anyLong(), any());
    }

    @Test
    void testUploadCategoryImage_CategoryRequest_CategoryNotFound_ThrowsCategoryNotFoundException() {
        when(adminCategoryService.updateCategory(eq(999L), eq(categoryRequest)))
                .thenThrow(CategoryNotFoundException.builder().message(ExceptionMessages.CATEGORY_NOT_FOUND + "Category not found.").build());

        assertThrows(CategoryNotFoundException.class, () -> {
            adminCategoryController.uploadCategoryImage(999L, null, categoryRequest);
        });
        verify(adminCategoryService).updateCategory(999L, categoryRequest);
        verify(adminCategoryService, never()).uploadCategoryImage(any(MultipartFile.class), anyLong());
    }

    @Test
    void testUpdateCategory_Success() {
        when(adminCategoryService.updateCategory(eq(1L), any(CategoryRequest.class))).thenReturn(categoryResponse);

        ResponseEntity<CategoryResponse> response = adminCategoryController.updateCategory(1L, categoryRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(categoryResponse, response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals("Electronics", response.getBody().getName());
        verify(adminCategoryService).updateCategory(1L, categoryRequest);
    }

    @Test
    void testUpdateCategory_CategoryNotFound() {
        when(adminCategoryService.updateCategory(eq(999L), any(CategoryRequest.class)))
                .thenThrow(CategoryNotFoundException.builder().message(ExceptionMessages.CATEGORY_NOT_FOUND + "Category not found.").build());

        assertThrows(CategoryNotFoundException.class, () -> {
            adminCategoryController.updateCategory(999L, categoryRequest);
        });
        verify(adminCategoryService).updateCategory(999L, categoryRequest);
    }

    @Test
    void testUpdateCategory_InvalidRequest() {
        CategoryRequest invalidRequest = CategoryRequest.builder().name(null).build();
        when(adminCategoryService.updateCategory(eq(1L), eq(invalidRequest)))
                .thenThrow(new jakarta.validation.ConstraintViolationException("Invalid request", null));

        assertThrows(jakarta.validation.ConstraintViolationException.class, () -> {
            adminCategoryController.updateCategory(1L, invalidRequest);
        });
        verify(adminCategoryService).updateCategory(1L, invalidRequest);
    }

    @Test
    void testDeleteCategory_Success() {
        doNothing().when(adminCategoryService).deleteCategory(1L);

        ResponseEntity<Void> response = adminCategoryController.deleteCategory(1L);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(adminCategoryService).deleteCategory(1L);
    }

    @Test
    void testDeleteCategory_CategoryNotFound() {
        doThrow(CategoryNotFoundException.builder().message(ExceptionMessages.CATEGORY_NOT_FOUND + "Category not found.").build())
                .when(adminCategoryService).deleteCategory(999L);

        assertThrows(CategoryNotFoundException.class, () -> {
            adminCategoryController.deleteCategory(999L);
        });
        verify(adminCategoryService).deleteCategory(999L);
    }
}

/*
import com.datasaz.ecommerce.exceptions.CategoryNotFoundException;
import com.datasaz.ecommerce.exceptions.response.ExceptionMessages;
import com.datasaz.ecommerce.models.request.CategoryRequest;
import com.datasaz.ecommerce.models.response.CategoryResponse;
import com.datasaz.ecommerce.services.interfaces.IAdminCategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AdminCategoryControllerTest {

    @Mock
    private IAdminCategoryService adminCategoryService;

    @InjectMocks
    private AdminCategoryController adminCategoryController;

    private CategoryRequest categoryRequest;
    private CategoryResponse categoryResponse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        categoryRequest = CategoryRequest.builder()
                .name("Electronics")
                .description("Electronic devices")
                .parentId(null)
                .build();

        categoryResponse = CategoryResponse.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic devices")
                .imageUrl("/category_images/electronics.jpg")
                .createdDate(LocalDateTime.of(2025, 7, 22, 8, 0))
                .parentId(null)
                .subcategories(null)
                .build();
    }

    @Test
    void testAddCategory_Success() {
        when(adminCategoryService.saveCategory(any(CategoryRequest.class))).thenReturn(categoryResponse);

        ResponseEntity<CategoryResponse> response = adminCategoryController.addCategory(categoryRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(categoryResponse, response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals("Electronics", response.getBody().getName());
        verify(adminCategoryService).saveCategory(categoryRequest);
    }

    @Test
    void testAddCategory_InvalidRequest() {
        CategoryRequest invalidRequest = CategoryRequest.builder().name(null).build();
        when(adminCategoryService.saveCategory(invalidRequest)).thenThrow(new jakarta.validation.ConstraintViolationException("Invalid request", null));

        assertThrows(jakarta.validation.ConstraintViolationException.class, () -> {
            adminCategoryController.addCategory(invalidRequest);
        });
        verify(adminCategoryService).saveCategory(invalidRequest);
    }

    @Test
    void testUploadCategoryImage_Success() {
        MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image".getBytes());
        String imageUrl = "/category_images/test.jpg";
        when(adminCategoryService.uploadCategoryImage(eq(image), eq(1L))).thenReturn(imageUrl);

        ResponseEntity<String> response = adminCategoryController.uploadCategoryImage(1L, image);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(imageUrl, response.getBody());
        verify(adminCategoryService).uploadCategoryImage(image, 1L);
    }

    @Test
    void testUploadCategoryImage_NullImage() {
        ResponseEntity<String> response = adminCategoryController.uploadCategoryImage(1L, null);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Image file is required", response.getBody());
        verifyNoInteractions(adminCategoryService);
    }

    @Test
    void testUploadCategoryImage_EmptyImage() {
        MockMultipartFile emptyImage = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[0]);

        ResponseEntity<String> response = adminCategoryController.uploadCategoryImage(1L, emptyImage);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Image file is required", response.getBody());
        verifyNoInteractions(adminCategoryService);
    }

    @Test
    void testUploadCategoryImage_CategoryNotFound() {
        MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image".getBytes());
        when(adminCategoryService.uploadCategoryImage(eq(image), eq(999L)))
                .thenThrow(CategoryNotFoundException.builder().message(ExceptionMessages.CATEGORY_NOT_FOUND + "Category not found.").build());

        assertThrows(CategoryNotFoundException.class, () -> {
            adminCategoryController.uploadCategoryImage(999L, image);
        });
        verify(adminCategoryService).uploadCategoryImage(image, 999L);
    }

    @Test
    void testUpdateCategory_Success() {
        when(adminCategoryService.updateCategory(eq(1L), any(CategoryRequest.class))).thenReturn(categoryResponse);

        ResponseEntity<CategoryResponse> response = adminCategoryController.updateCategory(1L, categoryRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(categoryResponse, response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals("Electronics", response.getBody().getName());
        verify(adminCategoryService).updateCategory(1L, categoryRequest);
    }

    @Test
    void testUpdateCategory_CategoryNotFound() {
        when(adminCategoryService.updateCategory(eq(999L), any(CategoryRequest.class)))
                .thenThrow(CategoryNotFoundException.builder().message(ExceptionMessages.CATEGORY_NOT_FOUND + "Category not found.").build());

        assertThrows(CategoryNotFoundException.class, () -> {
            adminCategoryController.updateCategory(999L, categoryRequest);
        });
        verify(adminCategoryService).updateCategory(999L, categoryRequest);
    }

    @Test
    void testUpdateCategory_InvalidRequest() {
        CategoryRequest invalidRequest = CategoryRequest.builder().name(null).build();
        when(adminCategoryService.updateCategory(eq(1L), eq(invalidRequest)))
                .thenThrow(new jakarta.validation.ConstraintViolationException("Invalid request", null));

        assertThrows(jakarta.validation.ConstraintViolationException.class, () -> {
            adminCategoryController.updateCategory(1L, invalidRequest);
        });
        verify(adminCategoryService).updateCategory(1L, invalidRequest);
    }

    @Test
    void testDeleteCategory_Success() {
        doNothing().when(adminCategoryService).deleteCategory(1L);

        ResponseEntity<Void> response = adminCategoryController.deleteCategory(1L);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(adminCategoryService).deleteCategory(1L);
    }

    @Test
    void testDeleteCategory_CategoryNotFound() {
        doThrow(CategoryNotFoundException.builder().message(ExceptionMessages.CATEGORY_NOT_FOUND + "Category not found.").build())
                .when(adminCategoryService).deleteCategory(999L);

        assertThrows(CategoryNotFoundException.class, () -> {
            adminCategoryController.deleteCategory(999L);
        });
        verify(adminCategoryService).deleteCategory(999L);
    }
}
*/


/*
package com.datasaz.ecommerce.controllers.admin;

import com.datasaz.ecommerce.controllers.CategoryController;
import com.datasaz.ecommerce.models.Request.CategoryRequest;
import com.datasaz.ecommerce.models.Response.CategoryResponse;
import com.datasaz.ecommerce.models.Response.ProductResponse;
import com.datasaz.ecommerce.services.interfaces.ICategoryService;
import com.datasaz.ecommerce.services.interfaces.IProductService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminCategoryControllerTest {
    @Mock
    private ICategoryService categoryService;

    @Mock
    private IProductService productService;

    @InjectMocks
    private CategoryController categoryController;

    @BeforeEach
    void setUp() {
        categoryController = CategoryController(categoryService, productService);
    }

    //    @Test
//    void testGetAllCategories() {
//        CategoryResponse category1 = CategoryResponse.builder().id(1L).name("Category1").build();
//        CategoryResponse category2 = CategoryResponse.builder().id(2L).name("Category2").build();
//        List<CategoryResponse> mockedCategoryList = List.of(category1, category2);
//
//        when(categoryService.findAllCategory()).thenReturn(mockedCategoryList);
//        List<CategoryResponse> response = categoryController.getAllCategories();
//        Assertions.assertEquals(mockedCategoryList, response);
//        // Verify that the categoryService's findAll method was called
//        verify(categoryService).findAllCategory();
//    }


//    @Test
//    void testGetCategoryById() {
//        Long categoryId = 1L;
//        CategoryResponse mockedCategory = CategoryResponse.builder().id(categoryId).name("Category1").build();
//
//        when(categoryService.findCategoryById(categoryId)).thenReturn(mockedCategory);
//        ResponseEntity<CategoryResponse> response = adminCategoryController.getById(categoryId);
//        Assertions.assertEquals(mockedCategory, response.getBody());
//        // Verify that the categoryService's findById method was called
//        verify(categoryService).findCategoryById(categoryId);
//    }

    @Test
    void testAddCategory() {
        Long parentCategoryId = 10L;

        CategoryResponse mockedResponse = CategoryResponse.builder()
                .id(1L)
                .name("Category1")
                .parentId(parentCategoryId)
                .build();
        CategoryRequest mockedRequest = CategoryRequest.builder()
                .name("Category1")
                .parentId(parentCategoryId)
                .build();
        when(categoryService.saveCategory(mockedRequest)).thenReturn(mockedResponse);
        ResponseEntity<CategoryResponse> response = adminCategoryController.addCategory(mockedRequest);
        Assertions.assertEquals(mockedResponse, response.getBody());
        // Verify that the categoryService's save method was called
        verify(categoryService).saveCategory(mockedRequest);
    }

    @Test
    void testUpdateCategory() {
        Long categoryId = 1L;
        Long newParentId = 2L;
        CategoryRequest mockedRequest = CategoryRequest.builder()
                .name("UpdatedCategory")
                .parentId(newParentId)
                .build();
        CategoryResponse mockedResponse = CategoryResponse.builder()
                .id(categoryId)
                .name("UpdatedCategory")
                .parentId(newParentId)
                .build();

        when(categoryService.updateCategory(categoryId, mockedRequest)).thenReturn(mockedResponse);
        ResponseEntity<CategoryResponse> response = adminCategoryController.updateCategory(categoryId, mockedRequest);
        Assertions.assertEquals(mockedResponse, response.getBody());
        // Verify that the categoryService's update method was called
        verify(categoryService).updateCategory(categoryId, mockedRequest);
    }

    @Test
    void testDeleteCategory() {
        Long categoryId = 1L;
        adminCategoryController.deleteCategory(categoryId);
        // Verify that the categoryService's deleteById method was called
        verify(categoryService).deleteCategory(categoryId);
    }

//    @Test
//    void testGetProductsByCategoryId() {
//        Long categoryId = 1L;
//        List<ProductResponse> mockedProductList = List.of(ProductResponse.builder().id(1L).name("Product1").build());
//
//        when(categoryService.checkCategoryExisitsById(categoryId)).thenReturn(true);
//        when(productService.findProductsByCategoryId(categoryId)).thenReturn(mockedProductList);
//
//        ResponseEntity<List<ProductResponse>> response = adminCategoryController.getProductsByCategoryId(categoryId);
//        Assertions.assertEquals(mockedProductList, response.getBody());
//        // Verify that the categoryService's existsById method was called
//        verify(categoryService).checkCategoryExisitsById(categoryId);
//        // Verify that the productService's findByCategoryId method was called
//        verify(productService).findProductsByCategoryId(categoryId);
//    }


//    @Test
//    void testGetProductsByCategoryId_CategoryNotFound() {
//        Long categoryId = 1L;
//
//        when(categoryService.checkCategoryExisitsById(categoryId)).thenReturn(false);
//
//        try {
//            adminCategoryController.getProductsByCategoryId(categoryId);
//        } catch (IllegalArgumentException e) {
//            Assertions.assertEquals("Category not found", e.getMessage());
//        }
//        // Verify that the categoryService's existsById method was called
//        verify(categoryService).checkCategoryExisitsById(categoryId);
//    }

}
*/