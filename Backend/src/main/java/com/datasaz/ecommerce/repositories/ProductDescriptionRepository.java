package com.datasaz.ecommerce.repositories;

import com.datasaz.ecommerce.repositories.entities.ProductDescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductDescriptionRepository extends JpaRepository<ProductDescription, Long> {

}