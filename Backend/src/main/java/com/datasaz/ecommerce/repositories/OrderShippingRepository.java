package com.datasaz.ecommerce.repositories;

import com.datasaz.ecommerce.repositories.entities.OrderShipping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderShippingRepository extends JpaRepository<OrderShipping, Long> {
    Optional<OrderShipping> findByOrderId(Long orderId);
}