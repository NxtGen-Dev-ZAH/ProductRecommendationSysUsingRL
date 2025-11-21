package com.datasaz.ecommerce.repositories;

import com.datasaz.ecommerce.repositories.entities.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // Optimistic locking (default with @Version)
    Optional<Order> findById(Long id);

    // Pessimistic locking
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithLock(Long id);

    // Find orders by user
    List<Order> findByBuyerId(Long buyerId);
}
