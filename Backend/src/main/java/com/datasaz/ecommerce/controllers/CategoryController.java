package com.datasaz.ecommerce.controllers;

import com.datasaz.ecommerce.models.response.CategoryResponse;
import com.datasaz.ecommerce.services.interfaces.ICategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {
    private final ICategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.findAllCategory());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.findCategoryById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CategoryResponse>> searchByName(@RequestParam String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search name cannot be empty");
        }
        return ResponseEntity.ok(categoryService.findCategoriesByName(name));
    }

    @GetMapping("/pages")
    public ResponseEntity<Page<CategoryResponse>> getCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (page < 0 || size <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page or size");
        }
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(categoryService.getCategories(pageable));
    }

    @GetMapping("/sort")
    public ResponseEntity<Page<CategoryResponse>> getSortedCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam String sortBy) {
        if (page < 0 || size <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page or size");
        }
        Pageable pageable = PageRequest.of(page, size);
        switch (sortBy.toLowerCase()) {
            case "favorites":
                return ResponseEntity.ok(categoryService.getCategoriesSortedByProductFavorites(pageable));
            case "views":
                return ResponseEntity.ok(categoryService.getCategoriesSortedByProductViews(pageable));
            case "quantity":
                return ResponseEntity.ok(categoryService.getCategoriesSortedByProductQuantity(pageable));
            case "soldquantity":
                return ResponseEntity.ok(categoryService.getCategoriesSortedByProductSoldQuantity(pageable));
            case "count":
                return ResponseEntity.ok(categoryService.getCategoriesSortedByProductCount(pageable));
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sortBy parameter");
        }
    }

//    @GetMapping("/{id}/products")
//    public ResponseEntity<List<ProductResponse>> getProductsByCategoryId(@PathVariable Long id) {
//        if (!categoryService.checkCategoryExisitsById(id)) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found");
//        }
//        return ResponseEntity.ok(productService.findProductsByCategoryId(id));
//    }

    @GetMapping("/{id}/subcategories")
    public ResponseEntity<List<CategoryResponse>> getSubcategories(@PathVariable Long id) {
        if (!categoryService.checkCategoryExistsById(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found");
        }
        return ResponseEntity.ok(categoryService.findSubcategoriesByCategoryId(id));
    }

    @GetMapping("/parents")
    public ResponseEntity<List<CategoryResponse>> getParentCategories() {
        return ResponseEntity.ok(categoryService.findParentCategories());
    }

    @GetMapping("/product-selection")
    public ResponseEntity<List<CategoryResponse>> getCategoriesForProductSelection() {
        return ResponseEntity.ok(categoryService.getCategoriesWithSubcategoriesForProductSelection());
    }
}




/*package com.datasaz.ecommerce.controllers;

import com.datasaz.ecommerce.models.Response.CategoryResponse;
import com.datasaz.ecommerce.models.Response.ProductResponse;
import com.datasaz.ecommerce.services.interfaces.ICategoryService;
import com.datasaz.ecommerce.services.interfaces.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {
    private final ICategoryService categoryService;
    private final IProductService productService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.findAllCategory());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.findCategoryById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CategoryResponse>> searchByName(@RequestParam String name) {
        return ResponseEntity.ok(categoryService.findCategoriesByName(name));
    }

    @GetMapping("/pages")
    public ResponseEntity<Page<CategoryResponse>> getCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(categoryService.getCategories(pageable));
    }

    @GetMapping("/sort")
    public ResponseEntity<Page<CategoryResponse>> getSortedCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam String sortBy) {
        Pageable pageable = PageRequest.of(page, size);
        if ("favorites".equalsIgnoreCase(sortBy)) {
            return ResponseEntity.ok(categoryService.getCategoriesSortedByProductFavorites(pageable));
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/{id}/products")
    public ResponseEntity<List<ProductResponse>> getProductsByCategoryId(@PathVariable Long id) {
        if (!categoryService.checkCategoryExisitsById(id)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(productService.findProductsByCategoryId(id));
    }

    @GetMapping("/{id}/subcategories")
    public ResponseEntity<List<CategoryResponse>> getSubcategories(@PathVariable Long id) {
        if (!categoryService.checkCategoryExisitsById(id)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(categoryService.findSubcategoriesByCategoryId(id));
    }

    @GetMapping("/parents")
    public ResponseEntity<List<CategoryResponse>> getParentCategories() {
        return ResponseEntity.ok(categoryService.findParentCategories());
    }

    @GetMapping("/product-selection")
    public ResponseEntity<List<CategoryResponse>> getCategoriesForProductSelection() {
        return ResponseEntity.ok(categoryService.getCategoriesWithSubcategoriesForProductSelection());
    }
} */
