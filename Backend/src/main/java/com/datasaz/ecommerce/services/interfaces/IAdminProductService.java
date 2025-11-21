package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IAdminProductService {

    List<ProductResponse> findAllProducts();

    Page<ProductResponse> findAllProducts(Pageable pageable);

    ProductResponse findProductById(Long id);

    Page<ProductResponse> findProductsByName(String productName, Pageable pageable);

    //List<ProductResponse> findProductsByName(String name);
    void deleteProductByAdmin(Long productId, String email);

}
