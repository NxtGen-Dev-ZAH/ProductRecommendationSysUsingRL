package com.datasaz.ecommerce.repositories;

import com.datasaz.ecommerce.repositories.entities.OrderRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRefundRepository extends JpaRepository<OrderRefund, Long> {
    Optional<OrderRefund> findByReturnRequestId(Long returnRequestId);
}