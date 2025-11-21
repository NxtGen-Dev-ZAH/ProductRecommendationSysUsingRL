package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.request.CategoryRequest;
import com.datasaz.ecommerce.models.response.CategoryResponse;
import org.springframework.web.multipart.MultipartFile;

public interface IAdminCategoryService {


    CategoryResponse saveCategory(CategoryRequest categoryRequest);

    CategoryResponse updateCategory(Long id, CategoryRequest categoryRequest);

    String uploadCategoryImage(MultipartFile image, Long categoryId);

    void deleteCategory(Long id);
    //boolean checkCategoryExisitsById(Long id);


}
