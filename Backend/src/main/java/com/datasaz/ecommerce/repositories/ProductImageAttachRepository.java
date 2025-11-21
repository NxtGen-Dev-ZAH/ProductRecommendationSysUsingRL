package com.datasaz.ecommerce.repositories;

import com.datasaz.ecommerce.repositories.entities.ProductImageAttach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageAttachRepository extends JpaRepository<ProductImageAttach, Long> {
    List<ProductImageAttach> findByProductId(Long productId);

    Optional<ProductImageAttach> findByProductIdAndIsPrimaryTrue(Long productId);

    List<ProductImageAttach> findByProductIsNullAndCreatedAtBefore(LocalDateTime threshold);
}