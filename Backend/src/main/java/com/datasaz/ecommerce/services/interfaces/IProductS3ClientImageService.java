package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.repositories.entities.ProductImage;
import org.springframework.web.multipart.MultipartFile;

public interface IProductS3ClientImageService {
    ProductImage uploadImage(Long productId, MultipartFile file, boolean isPrimary);

    void deleteImage(Long imageId);
}
