package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IProductService {

    /* TODO: see the following methods
    Page<ProductResponse> findAllProductsDeletedFalse(Pageable pageable);
    ProductResponse findProductByIdDeletedFalse(Long id);

    Page<ProductResponse> findProductsByAuthorEmailAddressDeletedFalse(String email, Pageable pageable);

    Page<ProductResponse> findProductsByNameAuthorNameDeletedFalse(String productName, String email, Pageable pageable);

    List<ProductResponse> findProductsByCategoryId(Long categoryId);

    Page<ProductResponse> findProductsByCategoryId(Long categoryId, Pageable pageable);

    Page<ProductResponse> findProductBySoldQuantity(Pageable pageable);
*/

    ProductResponse getProductById(Long id);

    Page<ProductResponse> getAllProducts(int page, int size);

    Page<ProductResponse> searchProductsByName(String name, Pageable pageable);
    //List<ProductResponse> searchProductsByName(String name);

    Page<ProductResponse> getProductsByCategory(Long categoryId, int page, int size);

    Page<ProductResponse> getFeaturedProducts(int page, int size);

    Page<ProductResponse> getNewArrivalProducts(Pageable pageable);

    Page<ProductResponse> getAllSortedByProductSoldQuantity(Pageable pageable);

    Page<ProductResponse> getByOfferPriceLessThanPrice(int page, int size);

    Page<ProductResponse> getRelatedProducts(Long categoryId, Long productId, Pageable pageable);

    Page<ProductResponse> getRecommendedProducts(int page, int size);


    //TODO: Sort by number of views of the products
    // List<ProductResponse> getProductSortedByViewCount(boolean ascending);

    //TODO: Sort by popularity
    // List<ProductResponse> getProductSortedByPopularity(boolean ascending);

    //TODO:
    // ProductResponse updateProductAnnounceViews(Long id, int announceViews);

    //TODO:
    // ProductResponse updateProductAnnounceViews24Hrs(Long id, int announceViews24Hrs);

    //TODO:
    // ProductResponse updateProductAnnounceFavorites(Long id, int announceFavorites);

    //TODO:
    // ProductResponse updateProductAnnounceLastViewTime(Long id, LocalDateTime announceLastViewTime);

    //TODO:
    // ProductResponse updateProductQuantityLastSold24Hrs(Long id, int quantityLastSold24Hrs);

    //TODO:
    // ProductResponse addProductInvoiceItem(Long id, InvoiceItem orderItem);

    //TODO:
    // ProductResponse updateProductInvoiceItem(Long id, InvoiceItem orderItem);

    //TODO:
    // ProductResponse deleteProductInvoiceItem(Long id, InvoiceItem orderItem);

    //TODO:
    // ProductResponse addProductProductReview(Long id, ProductReview review);

    //TODO:
    // ProductResponse updateProductProductReview(Long id, ProductReview review);

    //TODO:
    // ProductResponse deleteProductProductReview(Long id, ProductReview review);

    //TODO:
    // ProductResponse addProductPromotionCompaign(Long id, PromotionCompaign promotion);

    //TODO:
    // ProductResponse updateProductFavoritedByUsers(Long id, Users favoritedByUsers);


}
