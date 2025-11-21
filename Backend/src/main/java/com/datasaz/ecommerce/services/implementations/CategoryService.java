package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.CategoryNotFoundException;
import com.datasaz.ecommerce.mappers.CategoryMapper;
import com.datasaz.ecommerce.models.response.CategoryResponse;
import com.datasaz.ecommerce.repositories.CategoryRepository;
import com.datasaz.ecommerce.repositories.entities.Category;
import com.datasaz.ecommerce.services.interfaces.ICategoryService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryService implements ICategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    private final AuditLogService auditLogService;
    private static final Tika tika = new Tika();

    @Override
    @Cacheable(value = "categories", key = "#id")
    @RateLimiter(name = "categoryService")
    public CategoryResponse findCategoryById(Long id) {
        log.info("Find category by id: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("No categories found matching id: {}", id);
                    return CategoryNotFoundException.builder().message("Category not found.").build();
                });
        return categoryMapper.toResponse(category);
    }

    @Override
    @Cacheable(value = "categoriesByName", key = "#name")
    @RateLimiter(name = "categoryService")
    public List<CategoryResponse> findCategoriesByName(String name) {
        log.info("Find categories with name containing: {}", name);
        List<Category> categories = categoryRepository.findByNameContainingIgnoreCase(name);
        return categories.isEmpty()
                ? Collections.emptyList()
                : categories.stream().map(categoryMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "allCategories")
    @RateLimiter(name = "categoryService")
    public List<CategoryResponse> findAllCategory() {
        log.info("Find all categories");
        try {
            return categoryRepository.findAll()
                    .stream()
                    .map(categoryMapper::toResponse)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.error("Error accessing data {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    @RateLimiter(name = "categoryService")
    public Page<CategoryResponse> getCategories(Pageable pageable) {
        log.info("Getting paginated categories");
        return categoryRepository.findAll(pageable).map(categoryMapper::toResponse);
    }

    @Override
    public Page<CategoryResponse> getCategoriesSortedByProductFavorites(Pageable pageable) {
        log.info("Fetching categories sorted by: {}", "productfavorites");
        return categoryRepository.findAllSortedByProductFavorites(pageable)
                .map(categoryMapper::toResponse);
    }

    @Override
    public Page<CategoryResponse> getCategoriesSortedByProductViews(Pageable pageable) {
        log.info("Fetching categories sorted by: {}", "productviews");
        return categoryRepository.findAllSortedByProductViews(pageable)
                .map(categoryMapper::toResponse);
    }

    @Override
    public Page<CategoryResponse> getCategoriesSortedByProductQuantity(Pageable pageable) {
        log.info("Fetching categories sorted by: {}", "productquantity");
        return categoryRepository.findAllSortedByProductQuantity(pageable)
                .map(categoryMapper::toResponse);
    }

    @Override
    public Page<CategoryResponse> getCategoriesSortedByProductSoldQuantity(Pageable pageable) {
        log.info("Fetching categories sorted by: {}", "productsoldquantity");
        return categoryRepository.findAllSortedByProductSoldQuantity(pageable)
                .map(categoryMapper::toResponse);
    }

    @Override
    public Page<CategoryResponse> getCategoriesSortedByProductCount(Pageable pageable) {
        log.info("Fetching categories sorted by: {}", "productcount");
        return categoryRepository.findAllSortedByProductCount(pageable)
                .map(categoryMapper::toResponse);
    }

    @Override
    @Cacheable(value = "subcategories", key = "#categoryId")
    public List<CategoryResponse> findSubcategoriesByCategoryId(Long categoryId) {
        log.info("Find subcategories for category ID: {}", categoryId);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    log.error("Category not found with id: {}", categoryId);
                    return CategoryNotFoundException.builder().message("Category not found.").build();
                });
        return category.getSubcategories().stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "parentCategories")
    public List<CategoryResponse> findParentCategories() {
        log.info("Find all parent categories");
        try {
            return categoryRepository.findByParentIsNull()
                    .stream()
                    .map(categoryMapper::toResponse)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.error("Error accessing parent categories: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<CategoryResponse> getCategoriesWithSubcategoriesForProductSelection() {
        log.info("Find all categories with subcategories for product selection");
        try {
            // Fetch all leaf categories (categories with no subcategories)
            List<Category> leafCategories = categoryRepository.findBySubcategoriesIsEmpty();
            return leafCategories.stream()
                    .map(categoryMapper::toResponse)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.error("Error accessing categories for product selection: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public boolean checkCategoryExistsById(Long id) {
        log.info("Check exists by id: {}", id);
        return categoryRepository.existsById(id);
    }

}