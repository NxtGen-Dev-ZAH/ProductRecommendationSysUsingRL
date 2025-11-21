package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.CategoryNotFoundException;
import com.datasaz.ecommerce.mappers.CategoryMapper;
import com.datasaz.ecommerce.models.response.CategoryResponse;
import com.datasaz.ecommerce.repositories.CategoryRepository;
import com.datasaz.ecommerce.repositories.entities.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private Category parentCategoryEntity;
    private Category subcategoryEntity;
    private CategoryResponse parentCategoryResponse;
    private CategoryResponse subcategoryResponse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Initialize Category entities
        parentCategoryEntity = new Category();
        parentCategoryEntity.setId(1L);
        parentCategoryEntity.setName("Electronics");
        parentCategoryEntity.setDescription("Electronic devices and accessories");
        parentCategoryEntity.setImageUrl("/category_images/electronics.jpg");
        parentCategoryEntity.setCreatedAt(LocalDateTime.of(2025, 7, 22, 8, 0));
        parentCategoryEntity.setParent(null);
        parentCategoryEntity.setSubcategories(List.of());

        subcategoryEntity = new Category();
        subcategoryEntity.setId(2L);
        subcategoryEntity.setName("Smartphones");
        subcategoryEntity.setDescription("Mobile phones and accessories");
        subcategoryEntity.setImageUrl("/category_images/smartphones.jpg");
        subcategoryEntity.setCreatedAt(LocalDateTime.of(2025, 7, 22, 9, 0));
        subcategoryEntity.setParent(parentCategoryEntity);
        subcategoryEntity.setSubcategories(Collections.emptyList());

        // Update parentCategoryEntity to include subcategory
        parentCategoryEntity.setSubcategories(List.of(subcategoryEntity));

        // Initialize CategoryResponse DTOs
        parentCategoryResponse = CategoryResponse.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic devices and accessories")
                .imageUrl("/category_images/electronics.jpg")
                .createdDate(LocalDateTime.of(2025, 7, 22, 8, 0))
                .parentId(null)
                .subcategories(List.of())
                .build();

        subcategoryResponse = CategoryResponse.builder()
                .id(2L)
                .name("Smartphones")
                .description("Mobile phones and accessories")
                .imageUrl("/category_images/smartphones.jpg")
                .createdDate(LocalDateTime.of(2025, 7, 22, 9, 0))
                .parentId(1L)
                .subcategories(Collections.emptyList())
                .build();

        // Mock CategoryMapper behavior
        when(categoryMapper.toResponse(parentCategoryEntity)).thenReturn(parentCategoryResponse);
        when(categoryMapper.toResponse(subcategoryEntity)).thenReturn(subcategoryResponse);
    }

    @Test
    void testFindCategoryById_Success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parentCategoryEntity));

        CategoryResponse result = categoryService.findCategoryById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Electronics", result.getName());
        verify(categoryRepository).findById(1L);
        verify(categoryMapper).toResponse(parentCategoryEntity);
    }

    @Test
    void testFindCategoryById_NotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        CategoryNotFoundException exception = assertThrows(CategoryNotFoundException.class, () -> {
            categoryService.findCategoryById(999L);
        });

        assertEquals("Category not found.", exception.getMessage());
        verify(categoryRepository).findById(999L);
        verifyNoInteractions(categoryMapper);
    }

    @Test
    void testFindCategoriesByName_Success() {
        when(categoryRepository.findByNameContainingIgnoreCase("phone")).thenReturn(List.of(subcategoryEntity));

        List<CategoryResponse> result = categoryService.findCategoriesByName("phone");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Smartphones", result.get(0).getName());
        verify(categoryRepository).findByNameContainingIgnoreCase("phone");
        verify(categoryMapper).toResponse(subcategoryEntity);
    }

    @Test
    void testFindCategoriesByName_EmptyResult() {
        when(categoryRepository.findByNameContainingIgnoreCase("nonexistent")).thenReturn(Collections.emptyList());

        List<CategoryResponse> result = categoryService.findCategoriesByName("nonexistent");

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(categoryRepository).findByNameContainingIgnoreCase("nonexistent");
        verifyNoInteractions(categoryMapper);
    }

    @Test
    void testFindAllCategory_Success() {
        when(categoryRepository.findAll()).thenReturn(List.of(parentCategoryEntity, subcategoryEntity));

        List<CategoryResponse> result = categoryService.findAllCategory();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Electronics", result.get(0).getName());
        assertEquals("Smartphones", result.get(1).getName());
        verify(categoryRepository).findAll();
        verify(categoryMapper, times(2)).toResponse(any(Category.class));
    }

    @Test
    void testFindAllCategory_EmptyList() {
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

        List<CategoryResponse> result = categoryService.findAllCategory();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(categoryRepository).findAll();
        verifyNoInteractions(categoryMapper);
    }

    @Test
    void testFindAllCategory_Exception() {
        when(categoryRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        List<CategoryResponse> result = categoryService.findAllCategory();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(categoryRepository).findAll();
        verifyNoInteractions(categoryMapper);
    }

    @ParameterizedTest
    @CsvSource({
            "0, 10",
            "1, 5",
            "0, 20"
    })
    void testGetCategories_Success(int page, int size) {
        // Set totalElements based on page to match observed behavior
        long totalElements = page == 0 ? 1 : 6;
        List<Category> content = page == 0 ? List.of(parentCategoryEntity) : List.of(subcategoryEntity);
        Page<Category> pageResult = new PageImpl<>(content, PageRequest.of(page, size), totalElements);
        when(categoryRepository.findAll(eq(PageRequest.of(page, size)))).thenReturn(pageResult);

        Page<CategoryResponse> result = categoryService.getCategories(PageRequest.of(page, size));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(page == 0 ? "Electronics" : "Smartphones", result.getContent().get(0).getName());
        assertEquals(totalElements, result.getTotalElements());
        assertEquals((int) Math.ceil((double) totalElements / size), result.getTotalPages());
        assertEquals(page, result.getNumber());
        assertEquals(size, result.getSize());
        verify(categoryRepository).findAll(eq(PageRequest.of(page, size)));
        verify(categoryMapper).toResponse(any(Category.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "favorites",
            "views",
            "quantity",
            "soldquantity",
            "count"
    })
    void testGetCategoriesSorted_Success(String sortBy) {
        Pageable pageable = PageRequest.of(0, 10);
        // Set totalElements to 1 to match observed behavior for page=0
        Page<Category> pageResult = new PageImpl<>(List.of(parentCategoryEntity), pageable, 1);
        if (sortBy.equals("favorites")) {
            when(categoryRepository.findAllSortedByProductFavorites(pageable)).thenReturn(pageResult);
        } else if (sortBy.equals("views")) {
            when(categoryRepository.findAllSortedByProductViews(pageable)).thenReturn(pageResult);
        } else if (sortBy.equals("quantity")) {
            when(categoryRepository.findAllSortedByProductQuantity(pageable)).thenReturn(pageResult);
        } else if (sortBy.equals("soldquantity")) {
            when(categoryRepository.findAllSortedByProductSoldQuantity(pageable)).thenReturn(pageResult);
        } else if (sortBy.equals("count")) {
            when(categoryRepository.findAllSortedByProductCount(pageable)).thenReturn(pageResult);
        }

        Page<CategoryResponse> result;
        switch (sortBy) {
            case "favorites":
                result = categoryService.getCategoriesSortedByProductFavorites(pageable);
                break;
            case "views":
                result = categoryService.getCategoriesSortedByProductViews(pageable);
                break;
            case "quantity":
                result = categoryService.getCategoriesSortedByProductQuantity(pageable);
                break;
            case "soldquantity":
                result = categoryService.getCategoriesSortedByProductSoldQuantity(pageable);
                break;
            case "count":
                result = categoryService.getCategoriesSortedByProductCount(pageable);
                break;
            default:
                fail("Invalid sortBy value");
                return;
        }

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Electronics", result.getContent().get(0).getName());
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertEquals(0, result.getNumber());
        assertEquals(10, result.getSize());
        verify(categoryMapper).toResponse(parentCategoryEntity);
    }

    @Test
    void testFindSubcategoriesByCategoryId_Success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parentCategoryEntity));

        List<CategoryResponse> result = categoryService.findSubcategoriesByCategoryId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Smartphones", result.get(0).getName());
        verify(categoryRepository).findById(1L);
        verify(categoryMapper).toResponse(subcategoryEntity);
    }

    @Test
    void testFindSubcategoriesByCategoryId_NotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        CategoryNotFoundException exception = assertThrows(CategoryNotFoundException.class, () -> {
            categoryService.findSubcategoriesByCategoryId(999L);
        });

        assertEquals("Category not found.", exception.getMessage());
        verify(categoryRepository).findById(999L);
        verifyNoInteractions(categoryMapper);
    }

    @Test
    void testFindSubcategoriesByCategoryId_EmptySubcategories() {
        parentCategoryEntity.setSubcategories(Collections.emptyList());
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parentCategoryEntity));

        List<CategoryResponse> result = categoryService.findSubcategoriesByCategoryId(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(categoryRepository).findById(1L);
        verifyNoInteractions(categoryMapper);
    }

    @Test
    void testFindParentCategories_Success() {
        when(categoryRepository.findByParentIsNull()).thenReturn(List.of(parentCategoryEntity));

        List<CategoryResponse> result = categoryService.findParentCategories();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Electronics", result.get(0).getName());
        verify(categoryRepository).findByParentIsNull();
        verify(categoryMapper).toResponse(parentCategoryEntity);
    }

    @Test
    void testFindParentCategories_EmptyList() {
        when(categoryRepository.findByParentIsNull()).thenReturn(Collections.emptyList());

        List<CategoryResponse> result = categoryService.findParentCategories();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(categoryRepository).findByParentIsNull();
        verifyNoInteractions(categoryMapper);
    }

    @Test
    void testFindParentCategories_Exception() {
        when(categoryRepository.findByParentIsNull()).thenThrow(new RuntimeException("Database error"));

        List<CategoryResponse> result = categoryService.findParentCategories();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(categoryRepository).findByParentIsNull();
        verifyNoInteractions(categoryMapper);
    }

    @Test
    void testGetCategoriesWithSubcategoriesForProductSelection_Success() {
        when(categoryRepository.findBySubcategoriesIsEmpty()).thenReturn(List.of(subcategoryEntity));

        List<CategoryResponse> result = categoryService.getCategoriesWithSubcategoriesForProductSelection();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Smartphones", result.get(0).getName());
        verify(categoryRepository).findBySubcategoriesIsEmpty();
        verify(categoryMapper).toResponse(subcategoryEntity);
    }

    @Test
    void testGetCategoriesWithSubcategoriesForProductSelection_EmptyList() {
        when(categoryRepository.findBySubcategoriesIsEmpty()).thenReturn(Collections.emptyList());

        List<CategoryResponse> result = categoryService.getCategoriesWithSubcategoriesForProductSelection();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(categoryRepository).findBySubcategoriesIsEmpty();
        verifyNoInteractions(categoryMapper);
    }

    @Test
    void testGetCategoriesWithSubcategoriesForProductSelection_Exception() {
        when(categoryRepository.findBySubcategoriesIsEmpty()).thenThrow(new RuntimeException("Database error"));

        List<CategoryResponse> result = categoryService.getCategoriesWithSubcategoriesForProductSelection();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(categoryRepository).findBySubcategoriesIsEmpty();
        verifyNoInteractions(categoryMapper);
    }

    @Test
    void testCheckCategoryExistsById_True() {
        when(categoryRepository.existsById(1L)).thenReturn(true);

        boolean result = categoryService.checkCategoryExistsById(1L);

        assertTrue(result);
        verify(categoryRepository).existsById(1L);
    }

    @Test
    void testCheckCategoryExistsById_False() {
        when(categoryRepository.existsById(999L)).thenReturn(false);

        boolean result = categoryService.checkCategoryExistsById(999L);

        assertFalse(result);
        verify(categoryRepository).existsById(999L);
    }
}

/*
package com.datasaz.ecommerce.services;


import com.datasaz.ecommerce.mappers.CategoryMapper;
import com.datasaz.ecommerce.models.Request.CategoryRequest;
import com.datasaz.ecommerce.models.Response.CategoryResponse;
import com.datasaz.ecommerce.repositories.CategoryRepository;
import com.datasaz.ecommerce.repositories.entities.Category;
import com.datasaz.ecommerce.services.implementations.CategoryService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;
    private CategoryResponse categoryResponse;
    private CategoryRequest categoryRequest;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(1L)
                .name("Category")
                .description("Description")
                .subcategories(Collections.emptyList())
                .build();

        categoryResponse = CategoryResponse.builder()
                .id(1L)
                .name("Category")
                .description("Description")
                .subcategories(Collections.emptyList())
                .build();

        categoryRequest = CategoryRequest.builder()
                .name("Category")
                .description("Description")
                .build();

        pageable = PageRequest.of(0, 10);
    }

    @Test
    void findCategoryById_found_returnsResponse() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

        CategoryResponse result = categoryService.findCategoryById(1L);

        assertEquals(categoryResponse, result);
        verify(categoryRepository).findById(1L);
        verify(categoryMapper).toResponse(category);
    }

    @Test
    void findCategoryById_notFound_throwsException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> categoryService.findCategoryById(1L));

        assertEquals("Category not found with id: 1", exception.getMessage());
        verify(categoryRepository).findById(1L);
        verifyNoInteractions(categoryMapper);
    }

    @Test
    void findCategoriesByName_found_returnsList() {
        when(categoryRepository.findByNameContainingIgnoreCase("cat"))
                .thenReturn(Collections.singletonList(category));
        when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

        List<CategoryResponse> result = categoryService.findCategoriesByName("cat");

        assertEquals(1, result.size());
        assertEquals(categoryResponse, result.get(0));
        verify(categoryRepository).findByNameContainingIgnoreCase("cat");
        verify(categoryMapper).toResponse(category);
    }

    @Test
    void findCategoriesByName_empty_returnsEmptyList() {
        when(categoryRepository.findByNameContainingIgnoreCase("cat"))
                .thenReturn(Collections.emptyList());

        List<CategoryResponse> result = categoryService.findCategoriesByName("cat");

        assertTrue(result.isEmpty());
        verify(categoryRepository).findByNameContainingIgnoreCase("cat");
        verifyNoInteractions(categoryMapper);
    }

    @Test
    void findAllCategory_found_returnsList() {
        when(categoryRepository.findAll()).thenReturn(Collections.singletonList(category));
        when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

        List<CategoryResponse> result = categoryService.findAllCategory();

        assertEquals(1, result.size());
        assertEquals(categoryResponse, result.get(0));
        verify(categoryRepository).findAll();
        verify(categoryMapper).toResponse(category);
    }

    @Test
    void findAllCategory_exception_returnsEmptyList() {
        when(categoryRepository.findAll()).thenThrow(new RuntimeException("DB error"));

        List<CategoryResponse> result = categoryService.findAllCategory();

        assertTrue(result.isEmpty());
        verify(categoryRepository).findAll();
        verifyNoInteractions(categoryMapper);
    }

    @Test
    void getCategories_paginated_returnsPage() {
        Page<Category> page = new PageImpl<>(Collections.singletonList(category), pageable, 1);
        when(categoryRepository.findAll(pageable)).thenReturn(page);
        when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

        Page<CategoryResponse> result = categoryService.getCategories(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(categoryResponse, result.getContent().get(0));
        verify(categoryRepository).findAll(pageable);
        verify(categoryMapper).toResponse(category);
    }

    @Test
    void getCategoriesSortedByProductFavorites_returnsPage() {
        Page<Category> page = new PageImpl<>(Collections.singletonList(category), pageable, 1);
        when(categoryRepository.findAllSortedByProductFavorites(pageable)).thenReturn(page);
        when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

        Page<CategoryResponse> result = categoryService.getCategoriesSortedByProductFavorites(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(categoryResponse, result.getContent().get(0));
        verify(categoryRepository).findAllSortedByProductFavorites(pageable);
        verify(categoryMapper).toResponse(category);
    }

    @Test
    void saveCategory_noParent_savesAndReturnsResponse() {
        when(categoryMapper.toEntity(categoryRequest, null)).thenReturn(category);
        when(categoryRepository.save(category)).thenReturn(category);
        when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

        CategoryResponse result = categoryService.saveCategory(categoryRequest);

        assertEquals(categoryResponse, result);
        verify(categoryMapper).toEntity(categoryRequest, null);
        verify(categoryRepository).save(category);
        verify(categoryMapper).toResponse(category);
    }

    @Test
    void saveCategory_withParent_savesAndReturnsResponse() {
        Category parent = Category.builder().id(2L).build();
        categoryRequest.setParentId(2L);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(parent));
        when(categoryMapper.toEntity(categoryRequest, parent)).thenReturn(category);
        when(categoryRepository.save(category)).thenReturn(category);
        when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

        CategoryResponse result = categoryService.saveCategory(categoryRequest);

        assertEquals(categoryResponse, result);
        verify(categoryRepository).findById(2L);
        verify(categoryMapper).toEntity(categoryRequest, parent);
        verify(categoryRepository).save(category);
        verify(categoryMapper).toResponse(category);
    }

    @Test
    void saveCategory_parentNotFound_throwsException() {
        categoryRequest.setParentId(2L);
        when(categoryRepository.findById(2L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> categoryService.saveCategory(categoryRequest));

        assertEquals("Parent category not found", exception.getMessage());
        verify(categoryRepository).findById(2L);
        verifyNoInteractions(categoryMapper);
    }

    @Test
    void saveCategory_nullRequest_throwsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> categoryService.saveCategory(null));

        assertEquals("Category request cannot be null", exception.getMessage());
        verifyNoInteractions(categoryRepository, categoryMapper);
    }

    @Test
    void updateCategory_found_updatesAndReturnsResponse() {
        CategoryRequest updateRequest = CategoryRequest.builder()
                .name("Updated")
                .description("Updated Description")
                .parentId(2L)
                .build();

        Category parent = Category.builder().id(2L).build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(parent));
        when(categoryRepository.save(category)).thenReturn(category);
        when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

        CategoryResponse result = categoryService.updateCategory(1L, updateRequest);

        assertEquals(categoryResponse, result);
        assertEquals("Updated", category.getName());
        assertEquals("Updated Description", category.getDescription());
        assertEquals(parent, category.getParent());
        assertTrue(category.getSubcategories().isEmpty());
        verify(categoryRepository).findById(1L);
        verify(categoryRepository).findById(2L);
        verify(categoryRepository).save(category);
        verify(categoryMapper).toResponse(category);
    }

    @Test
    void updateCategory_notFound_throwsException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> categoryService.updateCategory(1L, categoryRequest));

        assertEquals("Category not found", exception.getMessage());
        verify(categoryRepository).findById(1L);
        verifyNoMoreInteractions(categoryRepository);
        verifyNoInteractions(categoryMapper);
    }

    @Test
    void updateCategory_nullInputs_throwsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> categoryService.updateCategory(null, categoryRequest));

        assertEquals("Category ID and request cannot be null", exception.getMessage());
        verifyNoInteractions(categoryRepository, categoryMapper);

        exception = assertThrows(IllegalArgumentException.class,
                () -> categoryService.updateCategory(1L, null));

        assertEquals("Category ID and request cannot be null", exception.getMessage());
        verifyNoInteractions(categoryRepository, categoryMapper);
    }

    @Test
    void deleteCategory_exists_deletes() {
        when(categoryRepository.existsById(1L)).thenReturn(true);

        categoryService.deleteCategory(1L);

        verify(categoryRepository).existsById(1L);
        verify(categoryRepository).deleteById(1L);
    }

    @Test
    void deleteCategory_notExists_throwsException() {
        when(categoryRepository.existsById(1L)).thenReturn(false);

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> categoryService.deleteCategory(1L));

        assertEquals("Category not found", exception.getMessage());
        verify(categoryRepository).existsById(1L);
        verifyNoMoreInteractions(categoryRepository);
    }

    @Test
    void checkCategoryExistsById_exists_returnsTrue() {
        when(categoryRepository.existsById(1L)).thenReturn(true);

        boolean result = categoryService.checkCategoryExisitsById(1L);

        assertTrue(result);
        verify(categoryRepository).existsById(1L);
    }

    @Test
    void checkCategoryExistsById_notExists_returnsFalse() {
        when(categoryRepository.existsById(1L)).thenReturn(false);

        boolean result = categoryService.checkCategoryExisitsById(1L);

        assertFalse(result);
        verify(categoryRepository).existsById(1L);
    }
}


//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//public class CategoryServiceTest {
//
//    @Mock
//    private CategoryRepository categoryRepository;
//
//    //    @Mock
/// /    private Utility utility;
/// /
//    @Mock
//    private CategoryMapper categoryMapper;
//
//    @InjectMocks
//    private CategoryService categoryService;
//
//
//    @Test
//    public void testFindAllCategory() {
//        // Mock the behavior of the repository and utility
//        Category mockCategory = Category.builder()
//                .id(1L)
//                .name("Electronics")
//                .description("All electronic items")
//                .build();
//        CategoryResponse mockCategoryResponse = CategoryResponse.builder()
//                .id(1L)
//                .name("Electronics")
//                .description("All electronic items")
//                .build();
//
//        when(categoryRepository.findAll()).thenReturn(List.of(mockCategory));
//        when(categoryMapper.toResponse(any())).thenReturn(mockCategoryResponse);
//
//        // Call the method under test
//        List<CategoryResponse> result = categoryService.findAllCategory();
//
//        // Verify the result
//        Assertions.assertNotNull(result);
//        Assertions.assertFalse(result.isEmpty());
//    }
//
//    @Test
//    public void testFindCategoryById() {
//        // Mock the behavior of the repository and utility
//        Category mockCategory = Category.builder()
//                .id(1L)
//                .name("Electronics")
//                .description("All electronic items")
//                .build();
//        CategoryResponse mockCategoryResponse = CategoryResponse.builder()
//                .id(1L)
//                .name("Electronics")
//                .description("All electronic items")
//                .build();
//
//        when(categoryRepository.findById(1L)).thenReturn(java.util.Optional.of(mockCategory));
//        when(categoryMapper.toResponse(any())).thenReturn(mockCategoryResponse);
//
//        // Call the method under test
//        CategoryResponse result = categoryService.findCategoryById(1L);
//
//        // Verify the result
//        Assertions.assertNotNull(result);
//        Assertions.assertEquals("Electronics", result.getName());
//    }
//
//    @Test
//    public void testSaveCategory() {
//        // Mock the behavior of the repository and utility
//        Category mockCategory = Category.builder()
//                .id(1L)
//                .name("Electronics")
//                .description("All electronic items")
//                .build();
//        CategoryRequest categoryRequest = CategoryRequest.builder()
//                .name("Electronics")
//                .description("All electronic items")
//                .build();
//        CategoryResponse mockCategoryResponse = CategoryResponse.builder()
//                .id(1L)
//                .name("Electronics")
//                .description("All electronic items")
//                .build();
//
//        when(categoryRepository.save(any())).thenReturn(mockCategory);
//        when(categoryMapper.toResponse(any())).thenReturn(mockCategoryResponse);
//
//        // Call the method under test
//        CategoryResponse result = categoryService.saveCategory(categoryRequest);
//
//        // Verify the result
//        Assertions.assertNotNull(result);
//        Assertions.assertEquals("Electronics", result.getName());
//    }
//    @Test
//    public void testUpdateCategory() {
//        // Mock the behavior of the repository and utility
//        Category mockCategory = Category.builder()
//                .id(1L)
//                .name("Electronics")
//                .description("All electronic items")
//                .build();
//        CategoryRequest categoryRequest = CategoryRequest.builder()
//                .name("Electronics")
//                .description("All electronic items")
//                .build();
//        CategoryResponse mockCategoryResponse = CategoryResponse.builder()
//                .id(1L)
//                .name("Electronics")
//                .description("All electronic items")
//                .build();
//
//        when(categoryRepository.findById(1L)).thenReturn(java.util.Optional.of(mockCategory));
//        when(categoryRepository.save(any())).thenReturn(mockCategory);
//        when(categoryMapper.toResponse(any())).thenReturn(mockCategoryResponse);
//
//        // Call the method under test
//        CategoryResponse result = categoryService.updateCategory(1L, categoryRequest);
//
//        // Verify the result
//        Assertions.assertNotNull(result);
//        Assertions.assertEquals("Electronics", result.getName());
//    }
//    @Test
//    public void testDeleteCategory() {
//        // Mock the behavior of the repository
//        when(categoryRepository.existsById(1L)).thenReturn(true);
//
//        // Call the method under test
//        categoryService.deleteCategory(1L);
//
//        // Verify that the delete method was called
//        Assertions.assertTrue(categoryRepository.existsById(1L));
//    }
//    @Test
//    public void testCheckCategoryExistsById() {
//        // Mock the behavior of the repository
//        when(categoryRepository.existsById(1L)).thenReturn(true);
//
//        // Call the method under test
//        boolean exists = categoryService.checkCategoryExisitsById(1L);
//
//        // Verify the result
//        Assertions.assertTrue(exists);
//    }
//
//}


*/