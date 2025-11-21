package com.datasaz.ecommerce.repositories;

import com.datasaz.ecommerce.repositories.entities.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    @Query("SELECT pi FROM ProductImage pi WHERE pi.product.id = :productId AND pi.isPrimary = true")
    Optional<ProductImage> findByProductIdAndIsPrimaryTrue(Long productId);

    List<ProductImage> findByProductIsNullAndCreatedAtBefore(LocalDateTime threshold);

    List<ProductImage> findByProductId(Long productId);

    ProductImage findByFileName(String fileName);

    //TODO: verify if this is needed -> ProductFileAttach is moved to ProductImage
    @Modifying
    @Query("DELETE FROM ProductImage img WHERE img.createdAt < :thresholdDate")
    int deleteByCreationTimeBefore(LocalDateTime thresholdDate);
}
