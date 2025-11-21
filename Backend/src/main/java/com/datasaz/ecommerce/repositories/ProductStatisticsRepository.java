package com.datasaz.ecommerce.repositories;

import com.datasaz.ecommerce.repositories.entities.ProductStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductStatisticsRepository extends JpaRepository<ProductStatistics, Long> {

}