package com.datasaz.ecommerce.repositories;

import com.datasaz.ecommerce.repositories.entities.ProductCustomFields;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductCustomFieldsRepository extends JpaRepository<ProductCustomFields, Long> {

}