package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.request.ProductImageRequest;
import com.datasaz.ecommerce.repositories.entities.ProductImage;
import com.datasaz.ecommerce.repositories.entities.ProductImageAttach;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IProductImageService {
    ProductImage uploadImage(Long productId, MultipartFile file, boolean isPrimary);
    //ProductImage updateUploadImage(Long productId, MultipartFile file, boolean isPrimary);
    ProductImage uploadImage(Long productId, ProductImageRequest imageRequest, boolean isPrimary);

    ProductImageAttach uploadImageAttach(Long productId, MultipartFile file, boolean isPrimary);
    ProductImageAttach uploadImageAttach(Long productId, ProductImageRequest imageRequest, boolean isPrimary);

    void deleteImageById(Long imageId);

    void deleteImagesByProductId(Long productId);

    void deleteImageAttachById(Long imageId);
    void deleteImageAttachesByProductId(Long productId);

    List<ProductImage> getImagesByProductId(Long productId);

    List<ProductImageAttach> getImageAttachesByProductId(Long productId);

    void cleanupOldImages();

    void cleanupOldImageAttaches();

}
