package com.datasaz.ecommerce.controllers.admin;

import com.datasaz.ecommerce.models.request.CategoryRequest;
import com.datasaz.ecommerce.models.response.CategoryResponse;
import com.datasaz.ecommerce.services.interfaces.IAdminCategoryService;
import com.datasaz.ecommerce.services.interfaces.IProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/category")
@RequiredArgsConstructor
public class AdminCategoryController {
    private final IAdminCategoryService adminCategoryService;
    private final IProductService productService;

    @PostMapping
    public ResponseEntity<CategoryResponse> addCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminCategoryService.saveCategory(request));
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<String> uploadCategoryImage(
            @PathVariable Long id,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart(value = "request", required = false) CategoryRequest request) {
        if ((image == null || image.isEmpty()) && (request == null || request.getImageContent() == null || request.getImageContent().isEmpty())) {
            return ResponseEntity.badRequest().body("Image file or image content is required");
        }
        if (image != null && !image.isEmpty()) {
            return ResponseEntity.ok(adminCategoryService.uploadCategoryImage(image, id));
        } else {
            CategoryResponse response = adminCategoryService.updateCategory(id, request);
            return ResponseEntity.ok(response.getImageUrl() != null ? response.getImageUrl() : "");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(adminCategoryService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        adminCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

}


//    @GetMapping
//    public ResponseEntity<Page<CategoryResponse>> getCategories(Pageable pageable) {
//        return ResponseEntity.ok(categoryService.getCategories(pageable));
//    }
//
//    @GetMapping("/sort")
//    public ResponseEntity<Page<CategoryResponse>> getSortedCategories(
//            @RequestParam String sortBy, Pageable pageable) {
//        switch (sortBy.toLowerCase()) {
//            case "favorites":
//                return ResponseEntity.ok(categoryService.getCategoriesSortedByProductFavorites(pageable));
//            case "views":
//                return ResponseEntity.ok(categoryService.getCategoriesSortedByProductViews(pageable));
//            case "quantity":
//                return ResponseEntity.ok(categoryService.getCategoriesSortedByProductQuantity(pageable));
//            case "sold":
//                return ResponseEntity.ok(categoryService.getCategoriesSortedByProductSoldQuantity(pageable));
//            case "count":
//                return ResponseEntity.ok(categoryService.getCategoriesSortedByProductCount(pageable));
//            default:
//                return ResponseEntity.badRequest().build();
//        }
//    }
//
//    @GetMapping("/{id}/products")
//    public ResponseEntity<List<ProductResponse>> getProductsByCategoryId(@PathVariable Long id) {
//        if (!categoryService.checkCategoryExisitsById(id)) {
//            throw new IllegalArgumentException("Category not found");
//        }
//        return ResponseEntity.ok(productService.findProductsByCategoryId(id));
//    }
//
//    @PostMapping
//    public ResponseEntity<CategoryResponse> addCategory(@Valid @RequestBody CategoryRequest categoryRequest) {
//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(categoryService.saveCategory(categoryRequest));
//    }
//
//    @PutMapping("/{id}")
//    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest categoryRequest) {
//        return ResponseEntity.ok(categoryService.updateCategory(id, categoryRequest));
//    }
//
//    @DeleteMapping("/{id}")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
//        categoryService.deleteCategory(id);
//        return ResponseEntity.noContent().build();
//    }


//    // QA UNIT: Tested with Postman and it works fine
//    @GetMapping("/all")
//    public List<CategoryResponse> getAllCategories() {
//        return categoryService.findAllCategory();
//    }
//
//    // QA UNIT: Tested with Postman and it works fine
//    @GetMapping("/{id}")
//    public ResponseEntity<CategoryResponse> getById(@PathVariable Long id) {
//        return ResponseEntity.ok(categoryService.findCategoryById(id));
//    }
//
//    // QA UNIT: Tested with Postman and it works fine
//    @GetMapping("/search")
//    public ResponseEntity<List<CategoryResponse>> searchByName(@RequestParam String name) {
//        return ResponseEntity.ok(categoryService.findCategoriesByName(name));
//    }
//
//    // QA UNIT: Partially Tested with Postman it works fine
//    @GetMapping
//    public ResponseEntity<Page<CategoryResponse>> getCategories(Pageable pageable) {
//        return ResponseEntity.ok(categoryService.getCategories(pageable));
//    }
//
//    // QA UNIT: Partially Tested with Postman and it works fine
//    @GetMapping("/sort")
//    public ResponseEntity<Page<CategoryResponse>> getSortedCategories(
//            @RequestParam String sortBy, Pageable pageable) {
//
//        return switch (sortBy.toLowerCase()) {
//            case "favorites" -> ResponseEntity.ok(categoryService.getCategoriesSortedByProductFavorites(pageable));
//            case "views" -> ResponseEntity.ok(categoryService.getCategoriesSortedByProductViews(pageable));
//            case "quantity" -> ResponseEntity.ok(categoryService.getCategoriesSortedByProductQuantity(pageable));
//            case "sold" -> ResponseEntity.ok(categoryService.getCategoriesSortedByProductSoldQuantity(pageable));
//            case "count" -> ResponseEntity.ok(categoryService.getCategoriesSortedByProductCount(pageable));
//            default -> ResponseEntity.badRequest().build();
//        };
//    }
//
//    // QA UNIT: Tested with Postman and it works fine
//    @GetMapping("/{id}/products")
//    public ResponseEntity<List<ProductResponse>> getProductsByCategoryId(@PathVariable Long id) {
//        if (!categoryService.checkCategoryExisitsById(id)) {
//            throw new IllegalArgumentException("Category not found");
//        }
//        return ResponseEntity.ok(productService.findProductsByCategoryId(id));
//    }
//
//    // QA UNIT: Tested with Postman and it works fine
//    @PostMapping
//    @ResponseStatus(HttpStatus.CREATED)
//    public ResponseEntity<CategoryResponse> addCategory(@RequestBody CategoryRequest categoryRequest) {
//        return ResponseEntity.ok(categoryService.saveCategory(categoryRequest));
//    }
//
//    // QA UNIT: Tested with Postman and it works fine
//    @PutMapping("/{id}")
//    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id, @RequestBody CategoryRequest categoryRequest) {
//        return ResponseEntity.ok(categoryService.updateCategory(id, categoryRequest));
//    }
//
//    // QA UNIT: Tested with Postman and it works fine
//    @DeleteMapping("/{id}")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
//        categoryService.deleteCategory(id);
//        return ResponseEntity.noContent().build();
//    }