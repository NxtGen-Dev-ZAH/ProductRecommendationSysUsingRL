package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.response.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ICategoryService {

    List<CategoryResponse> findAllCategory();
    CategoryResponse findCategoryById(Long id);

    List<CategoryResponse> findSubcategoriesByCategoryId(Long categoryId);

    List<CategoryResponse> findParentCategories();

    List<CategoryResponse> getCategoriesWithSubcategoriesForProductSelection();

    boolean checkCategoryExistsById(Long id);

    List<CategoryResponse> findCategoriesByName(String name);

    Page<CategoryResponse> getCategories(Pageable pageable);

    //Page<CategoryResponse> getCategories(Pageable pageable, String sortBy);

    Page<CategoryResponse> getCategoriesSortedByProductFavorites(Pageable pageable);

    Page<CategoryResponse> getCategoriesSortedByProductViews(Pageable pageable);

    Page<CategoryResponse> getCategoriesSortedByProductQuantity(Pageable pageable);

    Page<CategoryResponse> getCategoriesSortedByProductSoldQuantity(Pageable pageable);

    Page<CategoryResponse> getCategoriesSortedByProductCount(Pageable pageable);
}
