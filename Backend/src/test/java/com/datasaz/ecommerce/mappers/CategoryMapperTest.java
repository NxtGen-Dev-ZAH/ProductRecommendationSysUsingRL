package com.datasaz.ecommerce.mappers;


import com.datasaz.ecommerce.models.request.CategoryRequest;
import com.datasaz.ecommerce.models.response.CategoryResponse;
import com.datasaz.ecommerce.repositories.entities.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;


class CategoryMapperTest {
    private CategoryMapper categoryMapper;

    @BeforeEach
    void setUp() {
        categoryMapper = new CategoryMapper();
    }

    @Test
    void toResponse_validCategoryWithSubcategories_mapsCorrectly() {
        // Arrange
        Category subcategory = Category.builder()
                .id(2L)
                .name("Subcategory")
                .description("Subcategory description")
                .subcategories(new ArrayList<>())
                .build();

        Category category = Category.builder()
                .id(1L)
                .name("Category")
                .description("Category description")
                .subcategories(new ArrayList<>(Collections.singletonList(subcategory)))
                .build();

        subcategory.setParent(category);

        // Act
        CategoryResponse response = categoryMapper.toResponse(category);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Category", response.getName());
        assertEquals("Category description", response.getDescription());
        assertNull(response.getParentId());
        assertEquals(1, response.getSubcategories().size());

        CategoryResponse subResponse = response.getSubcategories().get(0);
        assertEquals(2L, subResponse.getId());
        assertEquals("Subcategory", subResponse.getName());
        assertEquals("Subcategory description", subResponse.getDescription());
        assertEquals(1L, subResponse.getParentId());
        assertTrue(subResponse.getSubcategories().isEmpty());
    }

    @Test
    void toResponse_nullCategory_returnsNull() {
        // Act
        CategoryResponse response = categoryMapper.toResponse(null);

        // Assert
        assertNull(response);
    }

    @Test
    void toResponse_noSubcategories_mapsCorrectly() {
        // Arrange
        Category category = Category.builder()
                .id(1L)
                .name("Category")
                .description("Category description")
                .subcategories(new ArrayList<>())
                .build();

        // Act
        CategoryResponse response = categoryMapper.toResponse(category);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Category", response.getName());
        assertEquals("Category description", response.getDescription());
        assertNull(response.getParentId());
        assertTrue(response.getSubcategories().isEmpty());
    }

    @Test
    void toResponse_nullSubcategories_mapsCorrectly() {
        // Arrange
        Category category = Category.builder()
                .id(1L)
                .name("Category")
                .description("Category description")
                .build();
        category.setSubcategories(null); // Explicitly test null subcategories

        // Act
        CategoryResponse response = categoryMapper.toResponse(category);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Category", response.getName());
        assertEquals("Category description", response.getDescription());
        assertNull(response.getParentId());
        assertTrue(response.getSubcategories().isEmpty());
    }

    @Test
    void toEntity_validRequestWithParent_mapsCorrectly() {
        // Arrange
        CategoryRequest request = CategoryRequest.builder()
                .name("Category")
                .description("Category description")
                .parentId(1L)
                .build();

        Category parent = Category.builder()
                .id(1L)
                .name("Parent")
                .subcategories(new ArrayList<>())
                .build();

        // Act
        Category category = categoryMapper.toEntity(request, parent);

        // Assert
        assertNotNull(category);
        assertEquals("Category", category.getName());
        assertEquals("Category description", category.getDescription());
        assertEquals(parent, category.getParent());
        assertNotNull(category.getSubcategories());
        assertTrue(category.getSubcategories().isEmpty());
    }

    @Test
    void toEntity_nullRequest_returnsNull() {
        // Act
        Category category = categoryMapper.toEntity(null, null);

        // Assert
        assertNull(category);
    }

    @Test
    void toEntity_noParent_mapsCorrectly() {
        // Arrange
        CategoryRequest request = CategoryRequest.builder()
                .name("Category")
                .description("Category description")
                .parentId(null)
                .build();

        // Act
        Category category = categoryMapper.toEntity(request, null);

        // Assert
        assertNotNull(category);
        assertEquals("Category", category.getName());
        assertEquals("Category description", category.getDescription());
        assertNull(category.getParent());
        assertNotNull(category.getSubcategories());
        assertTrue(category.getSubcategories().isEmpty());
    }

    @Test
    void toEntity_nullFields_mapsCorrectly() {
        // Arrange
        CategoryRequest request = CategoryRequest.builder()
                .name("Category")
                .description(null)
                .parentId(null)
                .build();

        // Act
        Category category = categoryMapper.toEntity(request, null);

        // Assert
        assertNotNull(category);
        assertEquals("Category", category.getName());
        assertNull(category.getDescription());
        assertNull(category.getParent());
        assertNotNull(category.getSubcategories());
        assertTrue(category.getSubcategories().isEmpty());
    }
}

