/*
package com.datasaz.ecommerce.controllers.admin;

import com.datasaz.ecommerce.configs.TestSecurityConfig;
import com.datasaz.ecommerce.controllers.CategoryController;
import com.datasaz.ecommerce.models.Request.CategoryRequest;
import com.datasaz.ecommerce.models.Response.CategoryResponse;
import com.datasaz.ecommerce.models.Response.ProductResponse;
import com.datasaz.ecommerce.services.interfaces.ICategoryService;
import com.datasaz.ecommerce.services.interfaces.IProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import(TestSecurityConfig.class)
class AdminCategoryControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ICategoryService categoryService;

    @MockBean
    private IProductService productService;

    @MockBean
    private UserDetailsService userDetailsService;

    private CategoryResponse categoryResponse;
    private CategoryRequest categoryRequest;
    private ProductResponse productResponse;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        categoryResponse = CategoryResponse.builder()
                .id(1L)
                .name("Category")
                .description("Description")
                .subcategories(Collections.emptyList())
                .build();

        categoryRequest = CategoryRequest.builder()
                .name("Category")
                .description("Description")
                .parentId(null)
                .build();

        productResponse = ProductResponse.builder()
                .id(1L)
                .name("Product")
                .build();

        pageable = PageRequest.of(0, 10);
    }

    @Test
    void getAllCategories_returnsList() throws Exception {
        when(categoryService.findAllCategory()).thenReturn(Collections.singletonList(categoryResponse));

        mockMvc.perform(get("/api/category/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Category"))
                .andExpect(jsonPath("$[0].description").value("Description"))
                .andExpect(jsonPath("$[0].subcategories").isEmpty());

        verify(categoryService).findAllCategory();
    }

    @Test
    void getById_found_returnsCategory() throws Exception {
        when(categoryService.findCategoryById(1L)).thenReturn(categoryResponse);

        mockMvc.perform(get("/api/category/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Category"))
                .andExpect(jsonPath("$.description").value("Description"))
                .andExpect(jsonPath("$.subcategories").isEmpty());

        verify(categoryService).findCategoryById(1L);
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(categoryService.findCategoryById(1L)).thenThrow(new jakarta.persistence.EntityNotFoundException("Category not found"));

        mockMvc.perform(get("/api/category/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(categoryService).findCategoryById(1L);
    }

    @Test
    void searchByName_returnsList() throws Exception {
        when(categoryService.findCategoriesByName("cat")).thenReturn(Collections.singletonList(categoryResponse));

        mockMvc.perform(get("/api/category/search")
                        .param("name", "cat")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Category"))
                .andExpect(jsonPath("$[0].description").value("Description"))
                .andExpect(jsonPath("$[0].subcategories").isEmpty());

        verify(categoryService).findCategoriesByName("cat");
    }

    @Test
    void getCategories_paginated_returnsPage() throws Exception {
        Page<CategoryResponse> page = new PageImpl<>(Collections.singletonList(categoryResponse), pageable, 1);
        when(categoryService.getCategories(pageable)).thenReturn(page);

        mockMvc.perform(get("/api/category")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].name").value("Category"))
                .andExpect(jsonPath("$.content[0].description").value("Description"))
                .andExpect(jsonPath("$.content[0].subcategories").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(categoryService).getCategories(any(Pageable.class));
    }

    @Test
    void getSortedCategories_favorites_returnsPage() throws Exception {
        Page<CategoryResponse> page = new PageImpl<>(Collections.singletonList(categoryResponse), pageable, 1);
        when(categoryService.getCategoriesSortedByProductFavorites(pageable)).thenReturn(page);

        mockMvc.perform(get("/api/category/sort")
                        .param("sortBy", "favorites")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].name").value("Category"))
                .andExpect(jsonPath("$.content[0].subcategories").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(categoryService).getCategoriesSortedByProductFavorites(any(Pageable.class));
    }

    @Test
    void getSortedCategories_invalidSortBy_returns400() throws Exception {
        mockMvc.perform(get("/api/category/sort")
                        .param("sortBy", "invalid")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoryService);
    }

    @Test
    void getProductsByCategoryId_found_returnsProducts() throws Exception {
        when(categoryService.checkCategoryExisitsById(1L)).thenReturn(true);
        when(productService.findProductsByCategoryId(1L)).thenReturn(Collections.singletonList(productResponse));

        mockMvc.perform(get("/api/category/1/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Product"));

        verify(categoryService).checkCategoryExisitsById(1L);
        verify(productService).findProductsByCategoryId(1L);
    }

    @Test
    void getProductsByCategoryId_notFound_throwsException() throws Exception {
        when(categoryService.checkCategoryExisitsById(1L)).thenReturn(false);

        mockMvc.perform(get("/api/category/1/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(categoryService).checkCategoryExisitsById(1L);
        verifyNoInteractions(productService);
    }

    @Test
    void addCategory_validRequest_returnsCreated() throws Exception {
        when(categoryService.saveCategory(any(CategoryRequest.class))).thenReturn(categoryResponse);

        mockMvc.perform(post("/api/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Category\",\"description\":\"Description\",\"parentId\":null}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Category"))
                .andExpect(jsonPath("$.description").value("Description"))
                .andExpect(jsonPath("$.subcategories").isEmpty());

        verify(categoryService).saveCategory(any(CategoryRequest.class));
    }

    @Test
    void addCategory_invalidRequest_nullName_returns400() throws Exception {
        mockMvc.perform(post("/api/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":null,\"description\":\"Description\",\"parentId\":null}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoryService);
    }

    @Test
    void updateCategory_found_returnsUpdated() throws Exception {
        when(categoryService.updateCategory(eq(1L), any(CategoryRequest.class))).thenReturn(categoryResponse);

        mockMvc.perform(put("/api/category/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\",\"description\":\"Updated Description\",\"parentId\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Category"))
                .andExpect(jsonPath("$.description").value("Description"))
                .andExpect(jsonPath("$.subcategories").isEmpty());

        verify(categoryService).updateCategory(eq(1L), any(CategoryRequest.class));
    }

    @Test
    void updateCategory_invalidRequest_nullName_returns400() throws Exception {
        mockMvc.perform(put("/api/category/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":null,\"description\":\"Updated Description\",\"parentId\":null}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoryService);
    }

    @Test
    void deleteCategory_exists_returnsNoContent() throws Exception {
        doNothing().when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/category/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(categoryService).deleteCategory(1L);
    }

    @Test
    void deleteCategory_notFound_returns404() throws Exception {
        doThrow(new jakarta.persistence.EntityNotFoundException("Category not found"))
                .when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/category/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(categoryService).deleteCategory(1L);
    }
}

//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.ResponseEntity;
//
//import java.util.List;
//
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//public class CategoryControllerTest {
//    @Mock
//    private ICategoryService categoryService;
//
//    @Mock
//    private IProductService productService;
//
//    @InjectMocks
//    private CategoryController categoryController;
//
//    @BeforeEach
//    void setUp() {
//        categoryController = new CategoryController(categoryService, productService);
//    }
//
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
//        ResponseEntity<CategoryResponse> response = categoryController.getById(categoryId);
//        Assertions.assertEquals(mockedCategory, response.getBody());
//        // Verify that the categoryService's findById method was called
//        verify(categoryService).findCategoryById(categoryId);
//    }
//    @Test
//    void testAddCategory() {
//        Long parentCategoryId = 10L;
//
//        CategoryResponse mockedResponse = CategoryResponse.builder()
//                .id(1L)
//                .name("Category1")
//                .parentId(parentCategoryId)
//                .build();
//        CategoryRequest mockedRequest = CategoryRequest.builder()
//                .name("Category1")
//                .parentId(parentCategoryId)
//                .build();
//        when(categoryService.saveCategory(mockedRequest)).thenReturn(mockedResponse);
//        ResponseEntity<CategoryResponse> response = categoryController.addCategory(mockedRequest);
//        Assertions.assertEquals(mockedResponse, response.getBody());
//        // Verify that the categoryService's save method was called
//        verify(categoryService).saveCategory(mockedRequest);
//    }
//    @Test
//    void testUpdateCategory() {
//        Long categoryId = 1L;
//        Long newParentId = 2L;
//        CategoryRequest mockedRequest = CategoryRequest.builder()
//                .name("UpdatedCategory")
//                .parentId(newParentId)
//                .build();
//        CategoryResponse mockedResponse = CategoryResponse.builder()
//                .id(categoryId)
//                .name("UpdatedCategory")
//                .parentId(newParentId)
//                .build();
//
//        when(categoryService.updateCategory(categoryId, mockedRequest)).thenReturn(mockedResponse);
//        ResponseEntity<CategoryResponse> response = categoryController.updateCategory(categoryId, mockedRequest);
//        Assertions.assertEquals(mockedResponse, response.getBody());
//        // Verify that the categoryService's update method was called
//        verify(categoryService).updateCategory(categoryId, mockedRequest);
//    }
//    @Test
//    void testDeleteCategory() {
//        Long categoryId = 1L;
//        categoryController.deleteCategory(categoryId);
//        // Verify that the categoryService's deleteById method was called
//        verify(categoryService).deleteCategory(categoryId);
//    }
//    @Test
//    void testGetProductsByCategoryId() {
//        Long categoryId = 1L;
//        List<ProductResponse> mockedProductList = List.of(ProductResponse.builder().id(1L).name("Product1").build());
//
//        when(categoryService.checkCategoryExisitsById(categoryId)).thenReturn(true);
//        when(productService.findProductsByCategoryId(categoryId)).thenReturn(mockedProductList);
//
//        ResponseEntity<List<ProductResponse>> response = categoryController.getProductsByCategoryId(categoryId);
//        Assertions.assertEquals(mockedProductList, response.getBody());
//        // Verify that the categoryService's existsById method was called
//        verify(categoryService).checkCategoryExisitsById(categoryId);
//        // Verify that the productService's findByCategoryId method was called
//        verify(productService).findProductsByCategoryId(categoryId);
//    }
//
//    @Test
//    void testGetProductsByCategoryId_CategoryNotFound() {
//        Long categoryId = 1L;
//
//        when(categoryService.checkCategoryExisitsById(categoryId)).thenReturn(false);
//
//        try {
//            categoryController.getProductsByCategoryId(categoryId);
//        } catch (IllegalArgumentException e) {
//            Assertions.assertEquals("Category not found", e.getMessage());
//        }
//        // Verify that the categoryService's existsById method was called
//        verify(categoryService).checkCategoryExisitsById(categoryId);
//    }
//
//}
*/