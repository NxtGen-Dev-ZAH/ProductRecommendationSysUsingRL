package com.datasaz.ecommerce.repositories;

import com.datasaz.ecommerce.repositories.entities.ShippingTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShippingTrackingRepository extends JpaRepository<ShippingTracking, Long> {
    Optional<ShippingTracking> findByOrderShippingId(Long orderShippingId);
}