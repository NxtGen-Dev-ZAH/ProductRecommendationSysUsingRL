package com.datasaz.ecommerce.controllers;

import com.datasaz.ecommerce.exceptions.CategoryNotFoundException;
import com.datasaz.ecommerce.exceptions.response.ExceptionMessages;
import com.datasaz.ecommerce.filters.JwtAuthenticationFilter;
import com.datasaz.ecommerce.models.response.CategoryResponse;
import com.datasaz.ecommerce.models.response.ProductImageResponse;
import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.models.response.ProductVariantResponse;
import com.datasaz.ecommerce.repositories.entities.ProductStatus;
import com.datasaz.ecommerce.services.interfaces.ICategoryService;
import com.datasaz.ecommerce.services.interfaces.IProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = CategoryController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                        classes = {JwtAuthenticationFilter.class})
        },
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class
        })
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ICategoryService categoryService;

    @MockBean
    private IProductService productService;

    private CategoryResponse parentCategory;
    private CategoryResponse subcategory;
    private ProductResponse sampleProduct;
    private Page<CategoryResponse> categoryPage;

    @BeforeEach
    void setUp() {
        // Initialize realistic sample data
        subcategory = CategoryResponse.builder()
                .id(2L)
                .name("Smartphones")
                .description("Mobile phones and accessories")
                .imageUrl("/category_images/smartphones.jpg")
                .createdDate(LocalDateTime.of(2025, 7, 22, 9, 0))
                .parentId(1L)
                .subcategories(Collections.emptyList())
                .build();

        parentCategory = CategoryResponse.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic devices and accessories")
                .imageUrl("/category_images/electronics.jpg")
                .createdDate(LocalDateTime.of(2025, 7, 22, 8, 0))
                .parentId(null)
                .subcategories(List.of(subcategory))
                .build();

        ProductImageResponse image = ProductImageResponse.builder()
                .id(201L)
                .fileName("smartphone.jpg")
                .fileUrl("/Uploads/products/smartphone.jpg")
                .contentType("image/jpeg")
                .fileSize(102400)
                .fileExtension("jpg")
                .createdAt(LocalDateTime.of(2025, 7, 22, 9, 0))
                .isPrimary(true)
                .displayOrder(1)
                .build();

        ProductVariantResponse variant = ProductVariantResponse.builder()
                .id(301L)
                .name("64GB Black")
                .priceAdjustment(new BigDecimal("50.00"))
                .quantity(20)
                .build();

        sampleProduct = ProductResponse.builder()
                .id(101L)
                .name("Smartphone X")
                .price(new BigDecimal("699.99"))
                .offerPrice(new BigDecimal("749.99"))
                .quantity(50)
                .inventoryLocation("Warehouse A")
                .warranty("2 years")
                .brand("TechBrand")
                .productCode("SMX123")
                .manufacturingPieceNumber("MPN456")
                .manufacturingDate(LocalDate.of(2025, 1, 1))
                .EAN("1234567890123")
                .manufacturingPlace("China")
                .categoryId(2L)
                .authorId(1L)
                .companyId(null)
                .productStatus(ProductStatus.AVAILABLE)
                .images(List.of(image))
                .variants(List.of(variant))
                .build();

        categoryPage = new PageImpl<>(List.of(parentCategory), PageRequest.of(0, 10), 6);
    }

    @Test
    void testGetAllCategories_Success() throws Exception {
        when(categoryService.findAllCategory()).thenReturn(List.of(parentCategory));

        mockMvc.perform(get("/api/category")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Electronics"))
                .andExpect(jsonPath("$[0].description").value("Electronic devices and accessories"))
                .andExpect(jsonPath("$[0].imageUrl").value("/category_images/electronics.jpg"))
                .andExpect(jsonPath("$[0].createdDate").exists())
                .andExpect(jsonPath("$[0].parentId").doesNotExist())
                .andExpect(jsonPath("$[0].subcategories[0].id").value(2L))
                .andExpect(jsonPath("$[0].subcategories[0].name").value("Smartphones"));
    }

    @Test
    void testGetAllCategories_EmptyList() throws Exception {
        when(categoryService.findAllCategory()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/category")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testGetById_Success() throws Exception {
        when(categoryService.findCategoryById(1L)).thenReturn(parentCategory);

        mockMvc.perform(get("/api/category/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andExpect(jsonPath("$.description").value("Electronic devices and accessories"))
                .andExpect(jsonPath("$.imageUrl").value("/category_images/electronics.jpg"))
                .andExpect(jsonPath("$.createdDate").exists())
                .andExpect(jsonPath("$.parentId").doesNotExist())
                .andExpect(jsonPath("$.subcategories[0].id").value(2L));
    }

    @Test
    void testGetById_NotFound() throws Exception {
        when(categoryService.findCategoryById(anyLong())).thenThrow(
                CategoryNotFoundException.builder()
                        .message("Category not found.")
                        .build()
        );

        mockMvc.perform(get("/api/category/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.CATEGORY_NOT_FOUND + "Category not found."))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("CategoryNotFound"))
                .andExpect(jsonPath("$.path").value("/api/category/999"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void testSearchByName_Success() throws Exception {
        when(categoryService.findCategoriesByName("phone")).thenReturn(List.of(subcategory));

        mockMvc.perform(get("/api/category/search")
                        .param("name", "phone")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2L))
                .andExpect(jsonPath("$[0].name").value("Smartphones"))
                .andExpect(jsonPath("$[0].description").value("Mobile phones and accessories"))
                .andExpect(jsonPath("$[0].imageUrl").value("/category_images/smartphones.jpg"))
                .andExpect(jsonPath("$[0].createdDate").exists())
                .andExpect(jsonPath("$[0].parentId").value(1L))
                .andExpect(jsonPath("$[0].subcategories").isEmpty());
    }

    @Test
    void testSearchByName_CaseInsensitive() throws Exception {
        when(categoryService.findCategoriesByName("PHONE")).thenReturn(List.of(subcategory));

        mockMvc.perform(get("/api/category/search")
                        .param("name", "PHONE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2L))
                .andExpect(jsonPath("$[0].name").value("Smartphones"));
    }

    @Test
    void testSearchByName_EmptyName() throws Exception {
        mockMvc.perform(get("/api/category/search")
                        .param("name", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Search name cannot be empty"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("ResponseStatusError"))
                .andExpect(jsonPath("$.path").value("/api/category/search"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void testSearchByName_NoResults() throws Exception {
        when(categoryService.findCategoriesByName(anyString())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/category/search")
                        .param("name", "nonexistent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
            "0, 10",
            "1, 5",
            "0, 20"
    })
    void testGetCategories_Paginated_Success(int page, int size) throws Exception {
        // Set totalElements based on page
        long totalElements = page == 0 ? 1 : 6;
        // Create content based on page
        List<CategoryResponse> content = page == 0 ? List.of(parentCategory) : List.of(subcategory);
        Page<CategoryResponse> customPage = new PageImpl<>(
                content,
                PageRequest.of(page, size),
                totalElements
        );
        when(categoryService.getCategories(eq(PageRequest.of(page, size)))).thenReturn(customPage);

        mockMvc.perform(get("/api/category/pages")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(page == 0 ? 1L : 2L))
                .andExpect(jsonPath("$.content[0].name").value(page == 0 ? "Electronics" : "Smartphones"))
                .andExpect(jsonPath("$.pageable.pageNumber").value(page))
                .andExpect(jsonPath("$.pageable.pageSize").value(size))
                .andExpect(jsonPath("$.totalElements").value(totalElements))
                .andExpect(jsonPath("$.totalPages").value((int) Math.ceil((double) totalElements / size)));
    }

    @Test
    void testGetCategories_NegativePage() throws Exception {
        mockMvc.perform(get("/api/category/pages")
                        .param("page", "-1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid page or size"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("ResponseStatusError"))
                .andExpect(jsonPath("$.path").value("/api/category/pages"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void testGetCategories_InvalidSize() throws Exception {
        mockMvc.perform(get("/api/category/pages")
                        .param("page", "0")
                        .param("size", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid page or size"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("ResponseStatusError"))
                .andExpect(jsonPath("$.path").value("/api/category/pages"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"favorites", "views", "quantity", "soldquantity", "count"})
    void testGetSortedCategories_Success(String sortBy) throws Exception {
        when(categoryService.getCategoriesSortedByProductFavorites(any(Pageable.class)))
                .thenReturn(categoryPage);
        when(categoryService.getCategoriesSortedByProductViews(any(Pageable.class)))
                .thenReturn(categoryPage);
        when(categoryService.getCategoriesSortedByProductQuantity(any(Pageable.class)))
                .thenReturn(categoryPage);
        when(categoryService.getCategoriesSortedByProductSoldQuantity(any(Pageable.class)))
                .thenReturn(categoryPage);
        when(categoryService.getCategoriesSortedByProductCount(any(Pageable.class)))
                .thenReturn(categoryPage);

        mockMvc.perform(get("/api/category/sort")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", sortBy)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].name").value("Electronics"))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(10));
    }

    @Test
    void testGetSortedCategories_InvalidSortBy() throws Exception {
        mockMvc.perform(get("/api/category/sort")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "invalid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid sortBy parameter"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("ResponseStatusError"))
                .andExpect(jsonPath("$.path").value("/api/category/sort"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void testGetSubcategories_Success() throws Exception {
        when(categoryService.checkCategoryExistsById(1L)).thenReturn(true);
        when(categoryService.findSubcategoriesByCategoryId(1L)).thenReturn(List.of(subcategory));

        mockMvc.perform(get("/api/category/1/subcategories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2L))
                .andExpect(jsonPath("$[0].name").value("Smartphones"))
                .andExpect(jsonPath("$[0].description").value("Mobile phones and accessories"))
                .andExpect(jsonPath("$[0].imageUrl").value("/category_images/smartphones.jpg"))
                .andExpect(jsonPath("$[0].createdDate").exists())
                .andExpect(jsonPath("$[0].parentId").value(1L))
                .andExpect(jsonPath("$[0].subcategories").isEmpty());
    }

    @Test
    void testGetSubcategories_NoSubcategories() throws Exception {
        when(categoryService.checkCategoryExistsById(2L)).thenReturn(true);
        when(categoryService.findSubcategoriesByCategoryId(2L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/category/2/subcategories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testGetSubcategories_CategoryNotFound() throws Exception {
        when(categoryService.checkCategoryExistsById(999L)).thenReturn(false);

        mockMvc.perform(get("/api/category/999/subcategories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Category not found"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("ResponseStatusError"))
                .andExpect(jsonPath("$.path").value("/api/category/999/subcategories"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void testGetParentCategories_Success() throws Exception {
        when(categoryService.findParentCategories()).thenReturn(List.of(parentCategory));

        mockMvc.perform(get("/api/category/parents")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Electronics"))
                .andExpect(jsonPath("$[0].parentId").doesNotExist())
                .andExpect(jsonPath("$[0].subcategories[0].id").value(2L));
    }

    @Test
    void testGetParentCategories_EmptyList() throws Exception {
        when(categoryService.findParentCategories()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/category/parents")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testGetCategoriesForProductSelection_Success() throws Exception {
        when(categoryService.getCategoriesWithSubcategoriesForProductSelection()).thenReturn(List.of(subcategory));

        mockMvc.perform(get("/api/category/product-selection")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2L))
                .andExpect(jsonPath("$[0].name").value("Smartphones"))
                .andExpect(jsonPath("$[0].parentId").value(1L))
                .andExpect(jsonPath("$[0].subcategories").isEmpty());
    }

    @Test
    void testGetCategoriesForProductSelection_EmptyList() throws Exception {
        when(categoryService.getCategoriesWithSubcategoriesForProductSelection()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/category/product-selection")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
