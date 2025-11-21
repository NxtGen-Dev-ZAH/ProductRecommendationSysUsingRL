package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.request.ProductRequest;
import com.datasaz.ecommerce.models.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface ISellerProductService {


    List<ProductResponse> findProductsByNameAuthorIdDeletedFalse(String name);

    Page<ProductResponse> findProductsByNameAuthorIdDeletedFalse(String name, Pageable pageable);

    ProductResponse saveProduct(ProductRequest productRequest, List<MultipartFile> imageFiles);

    //ProductResponse updateProduct(Long id, ProductRequest productRequest, List<MultipartFile> imageFiles);

    //void deleteProduct(Long id);


    //List<ProductResponse> findProductsByCategoryId(Long categoryId);

    //Page<ProductResponse> findProductsByCategoryId(Long categoryId, Pageable pageable);

    ProductResponse updateProductQuantity(Long id, int quantity);

    ProductResponse updateProductPrice(Long id, BigDecimal price);

    //Add a method for sorting products in a list for possiblities of edition
    // Page<ProductResponse> findProductBySoldQuantity(Pageable pageable);

    //ProductResponse createProduct(ProductRequest request, List<ProductImageRequest> images, String email);
    ProductResponse createProduct(ProductRequest productRequest, List<MultipartFile> images, String email);

    //ProductResponse updateProduct(Long productId, ProductRequest request, List<ProductImageRequest> newImages, List<Long> imagesToRemove, Long primaryImageId, String email);
    ProductResponse updateProduct(Long productId, ProductRequest request, List<MultipartFile> newImages, List<Long> imagesToRemove, Long primaryImageId, String email);

    void deleteProduct(Long productId, String email);

    Page<ProductResponse> getAuthorOrCompanyProducts(Long companyId, int page, int size);

    Page<ProductResponse> getAllAuthorOrCompanyProducts(int page, int size);

    void mergeProductsToCompany(Long companyId, String sellerEmail, String adminEmail);

    //ProductResponse updateProductImages(Long productId, List<ProductImageRequest> newImages, List<Long> imagesToRemove, Long primaryImageId, String email);
    ProductResponse updateProductImages(Long productId, List<MultipartFile> newImages, List<Long> imagesToRemove, Long primaryImageId, String email);


    //TODO:
    // ProductResponse updateProductCategory(Long id, Category category);

    //TODO:
    // ProductResponse updateProductStatus(Long id, String productStatus);

    //TODO:
    // ProductResponse updateProductAnnounceLastSoldTime(Long id, LocalDateTime announceLastSoldTime);

    //TODO:
    // ProductResponse updateProductProductDescription(Long id, ProductDescription productDescription);

    //TODO:
    // ProductResponse addProductProductCustomFields(Long id, ProductCustomFields productCustomFields);

    //TODO:
    // ProductResponse updateProductProductCustomFields(Long id, ProductCustomFields productCustomFields);

    //TODO:
    // ProductResponse deleteProductProductCustomFields(Long id, ProductCustomFields productCustomFields);

    //TODO:
    // ProductResponse addProductPromotionCompaign(Long id, PromotionCompaign promotion);

    //TODO:
    // ProductResponse updateProductPromotionCompaign(Long id, PromotionCompaign promotion);

    //TODO:
    // ProductResponse deleteProductPromotionCompaign(Long id, PromotionCompaign promotion);

}
